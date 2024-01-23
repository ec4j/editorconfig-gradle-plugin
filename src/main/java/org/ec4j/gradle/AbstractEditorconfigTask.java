/*
 * Copyright (c) 2018 EditorConfig Gradle Plugin
 * project contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ec4j.gradle;

import java.io.File;
import java.nio.charset.Charset;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.ec4j.gradle.CollectingLogger.LogMessages;
import org.ec4j.gradle.runtime.EditorconfigInvoker;
import org.ec4j.gradle.runtime.EditorconfigParameters;
import org.ec4j.lint.api.Constants;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileTree;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkerExecutionException;
import org.gradle.workers.WorkerExecutor;

/**
 * A base for {@link EditorconfigCheckTask} and {@link EditorconfigFormatTask}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public abstract class AbstractEditorconfigTask extends DefaultTask {

    private final WorkerExecutor workerExecutor;

    /**
     * {@link FileTree} scanning boilerplate.
     *
     * @param project the project being built
     * @param editorconfigExtension the config, cannot be null
     * @return a {@link Set} of absolute paths of included files
     */
    private static Set<String> scanIncludedFiles(Project project, final EditorconfigExtension editorconfigExtension) {
        FileTree tree = project.fileTree(project.getProjectDir(), fileTree -> {
            fileTree.include(editorconfigExtension.getIncludes());

            Set<String> excls = new LinkedHashSet<>(editorconfigExtension.getExcludes());
            if (editorconfigExtension.isExcludeNonSourceFiles()) {
                excls.addAll(Constants.DEFAULT_EXCLUDES);
            }
            fileTree.exclude(excls);
        });
        final Set<String> result = new LinkedHashSet<>();
        for (File file : tree.getFiles()) {
            result.add(file.getAbsolutePath());
        }
        return result;
    }

    /** The {@link EditorconfigExtension} initialized in {@link #perform()} */
    protected EditorconfigExtension editorconfigExtension;

    protected AbstractEditorconfigTask(WorkerExecutor workerExecutor) {
        super();
        this.workerExecutor = workerExecutor;
    }

    /**
     * Performs this task.
     */
    @TaskAction
    public void perform() {
        final Logger log = getLogger();
        final Project project = getProject();
        editorconfigExtension = project.getExtensions().findByType(EditorconfigExtension.class);
        if (editorconfigExtension == null) {
            editorconfigExtension = EditorconfigExtension.default_();
        }

        final Charset charset;
        if (editorconfigExtension.getEncoding() == null || editorconfigExtension.getEncoding().isEmpty()) {
            charset = Charset.defaultCharset();
            log.warn(
                    "Using current platform's default encoding {} to read .editorconfig files. You do not want this. Set either 'project.build.sourceEncoding' or 'editorconfig.encoding' property.",
                    charset);
        } else {
            charset = Charset.forName(editorconfigExtension.getEncoding());
        }
        final String basedirPath = project.getProjectDir().toPath().toString();

        final Set<String> includedPaths = scanIncludedFiles(project, editorconfigExtension);

        final Configuration classpath = project.getConfigurations().getAt(EditorconfigGradlePlugin.CONFIGURATION_NAME);

        workerExecutor
                .classLoaderIsolation(spec -> spec.getClasspath().from(classpath))
                .submit(
                        EditorconfigInvoker.class,
                        parameters -> configureInvokerParameters(parameters, includedPaths, basedirPath, charset)
                );

        try {
            workerExecutor.await();
        } catch (WorkerExecutionException e) {

            /* A megahack to pass data from the classpath-isolated WorkerExecutor.
             * Found no better way than to use some Exception defined in JDK
             * and smuggle the data through its message field as a string.
             * The serialization format is defined in CollectingLogger.
             * Note that even the log messages need to be passed like that because
             * loggers instantiated by the isolated class loader do not obey
             * the log level set via Gradle CLI.
             * @ppalaga is open for suggestions to improve this :)
             */

            Throwable t = e;
            while (t.getCause() != null) {
                t = t.getCause();
                if (t instanceof RuntimeException) {
                    final String msg = t.getMessage();
                    if (msg != null && msg.startsWith(EditorconfigInvoker.FORMAT_EXCEPTION_PREFIX)) {
                        final LogMessages logData = CollectingLogger
                                .deserialize(EditorconfigInvoker.FORMAT_EXCEPTION_PREFIX.length(), msg);
                        for (Entry<String, String> logMessage : logData.getMessages()) {
                            final String key = logMessage.getKey();
                            /* There is no TRACE in org.gradle.api.logging.LogLevel */
                            final LogLevel logLevel = org.ec4j.lint.api.Logger.LogLevel.TRACE.name().equals(key)
                                    ? LogLevel.DEBUG
                                    : LogLevel.valueOf(key);
                            log.log(logLevel, logMessage.getValue());
                        }
                        final String failureMessage = logData.getFailureMessage();
                        if (failureMessage != null) {
                            throw new GradleException(failureMessage);
                        } else {
                            return;
                        }
                    }
                }
            }
            throw e;
        }

    }

    private void configureInvokerParameters(EditorconfigParameters editorconfigParameters,
                                            Set<String> includedPaths,
                                            String basedirPath,
                                            Charset charset) {
        final Class<?> taskClassName = AbstractEditorconfigTask.this.getClass();

        editorconfigParameters.getTaskClass().set(taskClassName.getName());
        editorconfigParameters.getIncludedFiles().set(includedPaths);
        editorconfigParameters.getBasedirPath().set(basedirPath);
        editorconfigParameters.getCharset().set(charset.name());

        editorconfigParameters.getFailOnFormatViolation().set(editorconfigExtension.isFailOnFormatViolation());
        editorconfigParameters.getBackUp().set(editorconfigExtension.isBackup());
        editorconfigParameters.getBackupSuffix().set(editorconfigExtension.getBackupSuffix());
        editorconfigParameters.getAddLintersFromClassPath().set(editorconfigExtension.isAddLintersFromClassPath());
        editorconfigParameters.getLinters().set(editorconfigExtension.getLinters());

        editorconfigParameters.getFailOnNoMatchingProperties().set(
                editorconfigExtension.isFailOnNoMatchingProperties()
        );
    }

}
