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
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.ec4j.core.Cache.Caches;
import org.ec4j.core.Resource.Resources;
import org.ec4j.core.ResourceProperties;
import org.ec4j.core.ResourcePropertiesService;
import org.ec4j.core.model.PropertyType;
import org.ec4j.maven.lint.api.Constants;
import org.ec4j.maven.lint.api.EditableResource;
import org.ec4j.maven.lint.api.FormatException;
import org.ec4j.maven.lint.api.Linter;
import org.ec4j.maven.lint.api.LinterRegistry;
import org.ec4j.maven.lint.api.Resource;
import org.ec4j.maven.lint.api.ViolationHandler;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.FileTree;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.TaskAction;

/**
 * A base for {@link EditorconfigCheckTask} and {@link EditorconfigFormatTask}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public abstract class AbstractEditorconfigTask extends DefaultTask {

    private static LinterRegistry buildLinterRegistry(EditorconfigExtension ext) {

        final LinterRegistry.Builder linterRegistryBuilder = LinterRegistry.builder();

        final ClassLoader cl = ext.getClass().getClassLoader();
        if (ext.isAddLintersFromClassPath()) {
            linterRegistryBuilder.scan(cl);
        }

        final List<LinterConfig> linters = ext.getLinters();
        if (linters != null && !linters.isEmpty()) {
            for (LinterConfig linter : linters) {
                if (linter.isEnabled()) {
                    linterRegistryBuilder.entry(linter.getId(), linter.getClassName(), cl, linter.getIncludes(),
                            linter.getExcludes(), linter.isUseDefaultIncludesAndExcludes());
                } else {
                    linterRegistryBuilder.removeEntry(linter.getId());
                }
            }
        }

        return linterRegistryBuilder.build();

    }

    /**
     * {@link FileTree} scanning boilerplate.
     *
     * @param project
     *            the project being built
     * @param editorconfigExtension
     *            the config, cannot be null
     * @return a {@link Set} of included files
     */
    private static Set<File> scanIncludedFiles(Project project, final EditorconfigExtension editorconfigExtension) {
        FileTree tree = project.fileTree(project.getProjectDir(), new Action<ConfigurableFileTree>() {
            @Override
            public void execute(ConfigurableFileTree tree) {
                tree.include(editorconfigExtension.getIncludes());

                Set<String> excls = new LinkedHashSet<>(editorconfigExtension.getExcludes());
                if (editorconfigExtension.isExcludeNonSourceFiles()) {
                    excls.addAll(Constants.DEFAULT_EXCLUDES);
                }
                tree.exclude(excls);
            }
        });
        return tree.getFiles();
    }

    /** Rhe {@link EditorconfigExtension} initialized in {@link #perform()} */
    protected EditorconfigExtension editorconfigExtension;

    protected abstract ViolationHandler createHandler();

    /**
     * Create a new {@link Resource} suitable for the current task.
     *
     * @param absFile
     *            the {@link Path} to create a {@link Resource} for. Must be absolute.
     * @param relFile
     *            the {@link Path} to create a {@link Resource} for. Must be relative to {@link #basedirPath}.
     * @param encoding
     *            the encoding of the resulting {@link Resource}
     * @return a new {@link Resource} or a new {@link EditableResource}
     */
    protected abstract Resource createResource(Path absFile, Path relFile, Charset encoding);

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
        final Path basedirPath = project.getProjectDir().toPath();

        LinterRegistry linterRegistry = buildLinterRegistry(editorconfigExtension);
        final Set<File> includedFiles = scanIncludedFiles(project, editorconfigExtension);

        try {
            final ViolationHandler handler = createHandler();

            final ResourcePropertiesService resourcePropertiesService = ResourcePropertiesService.builder() //
                    .cache(Caches.permanent()) //
                    .build();
            handler.startFiles();
            boolean propertyMatched = false;
            for (File includedFile : includedFiles) {
                final Path absFile = includedFile.getAbsoluteFile().toPath();
                final Path file = basedirPath.relativize(absFile);
                log.debug("Processing file '{}'", file);
                final ResourceProperties editorConfigProperties = resourcePropertiesService
                        .queryProperties(Resources.ofPath(absFile, charset));
                if (!editorConfigProperties.getProperties().isEmpty()) {
                    propertyMatched = true;
                    final Charset useEncoding = Charset.forName(editorConfigProperties.getValue(PropertyType.charset,
                            editorconfigExtension.getEncoding(), true));
                    final Resource resource = createResource(absFile, file, useEncoding);
                    final List<Linter> filteredLinters = linterRegistry.filter(file);
                    ViolationHandler.ReturnState state = ViolationHandler.ReturnState.RECHECK;
                    while (state != ViolationHandler.ReturnState.FINISHED) {
                        for (Linter linter : filteredLinters) {
                            if (log.isDebugEnabled()) {
                                log.debug("Processing file '{}' using linter {}", file, linter.getClass().getName());
                            }
                            handler.startFile(resource);
                            linter.process(resource, editorConfigProperties, handler);
                        }
                        state = handler.endFile();
                    }
                }
            }
            if (!propertyMatched) {
                if (editorconfigExtension.isFailOnNoMatchingProperties()) {
                    log.error("No .editorconfig properties applicable for files under '{}'", basedirPath);
                } else {
                    log.warn("No .editorconfig properties applicable for files under '{}'", basedirPath);
                }
            }
            handler.endFiles();
        } catch (IOException e) {
            throw new GradleException(e.getMessage(), e);
        } catch (FormatException e) {
            throw new GradleException("\n\n" + e.getMessage() + "\n\n", e);
        }
    }
}
