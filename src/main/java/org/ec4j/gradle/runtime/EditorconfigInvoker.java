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
package org.ec4j.gradle.runtime;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.ec4j.core.Cache.Caches;
import org.ec4j.core.Resource.Resources;
import org.ec4j.core.ResourceProperties;
import org.ec4j.core.ResourcePropertiesService;
import org.ec4j.core.model.PropertyType;
import org.ec4j.gradle.CollectingLogger;
import org.ec4j.gradle.EditorconfigCheckTask;
import org.ec4j.gradle.EditorconfigFormatTask;
import org.ec4j.gradle.LinterConfig;
import org.ec4j.maven.lint.api.EditableResource;
import org.ec4j.maven.lint.api.FormatException;
import org.ec4j.maven.lint.api.FormattingHandler;
import org.ec4j.maven.lint.api.Linter;
import org.ec4j.maven.lint.api.LinterRegistry;
import org.ec4j.maven.lint.api.Logger;
import org.ec4j.maven.lint.api.Logger.LogLevel;
import org.ec4j.maven.lint.api.Resource;
import org.ec4j.maven.lint.api.ViolationCollector;
import org.ec4j.maven.lint.api.ViolationHandler;
import org.gradle.api.GradleException;

/**
 * A {@link Runnable} suitable for being invoked in an isolated class loader.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class EditorconfigInvoker implements Runnable {

    interface ResourceFactory {
        Resource createResource(Path absFile, Path relFile, Charset encoding);
    }

    public static final String FORMAT_EXCEPTION_PREFIX = FormatException.class.getName() + "\n";

    private static LinterRegistry buildLinterRegistry(boolean isAddLintersFromClassPath, List<LinterConfig> linters,
            ClassLoader cl, Logger log) {
        final LinterRegistry.Builder linterRegistryBuilder = LinterRegistry.builder().log(log);
        if (isAddLintersFromClassPath) {
            linterRegistryBuilder.scan(cl);
        }
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

    private final Path basedirPath;
    private final Charset charset;
    private final boolean failOnNoMatchingProperties;
    private final ViolationHandler handler;
    private final Set<String> includedFiles;
    private final LinterRegistry linterRegistry;
    private final CollectingLogger log;
    private final ResourceFactory resourceFactory;

    @Inject
    public EditorconfigInvoker(String taskClass, Set<String> includedFiles, String basedirPath, String charset,
            boolean isFailOnFormatViolation, boolean isBackUp, String backupSuffix, boolean isAddLintersFromClassPath,
            List<LinterConfig> linters, boolean failOnNoMatchingProperties) {
        super();
        this.log = new CollectingLogger(LogLevel.TRACE);
        if (taskClass.startsWith(EditorconfigCheckTask.class.getName())) {
            this.handler = new ViolationCollector(isFailOnFormatViolation, "./gradlew editorconfigFormat", log);
            this.resourceFactory = new ResourceFactory() {
                @Override
                public Resource createResource(Path absFile, Path relFile, Charset encoding) {
                    return new Resource(absFile, relFile, encoding);
                }
            };
        } else if (taskClass.startsWith(EditorconfigFormatTask.class.getName())) {
            this.handler = new FormattingHandler(isBackUp, backupSuffix, log);
            this.resourceFactory = new ResourceFactory() {
                @Override
                public Resource createResource(Path absFile, Path relFile, Charset encoding) {
                    return new EditableResource(absFile, relFile, encoding);
                }
            };
        } else {
            throw new IllegalStateException(String.format("Expected %s or %s; got %s",
                    EditorconfigCheckTask.class.getName(), EditorconfigFormatTask.class.getName(), taskClass));
        }
        this.includedFiles = includedFiles;
        this.basedirPath = Paths.get(basedirPath);
        this.charset = Charset.forName(charset);
        final ClassLoader invokerCl = EditorconfigInvoker.class.getClassLoader();
        this.linterRegistry = buildLinterRegistry(isAddLintersFromClassPath, linters, invokerCl, log);
        this.failOnNoMatchingProperties = failOnNoMatchingProperties;
    }

    @Override
    public void run() {
        FormatException formatException = null;
        try {

            final ResourcePropertiesService resourcePropertiesService = ResourcePropertiesService.builder() //
                    .cache(Caches.permanent()) //
                    .build();
            handler.startFiles();
            boolean propertyMatched = false;
            for (String includedFile : includedFiles) {
                final Path absFile = Paths.get(includedFile);
                final Path file = basedirPath.relativize(absFile);
                log.info("Processing file '{}'", file);
                final ResourceProperties editorConfigProperties = resourcePropertiesService
                        .queryProperties(Resources.ofPath(absFile, charset));
                if (!editorConfigProperties.getProperties().isEmpty()) {
                    propertyMatched = true;
                    final Charset useEncoding = Charset
                            .forName(editorConfigProperties.getValue(PropertyType.charset, charset.name(), true));
                    final Resource resource = resourceFactory.createResource(absFile, file, useEncoding);
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
                if (failOnNoMatchingProperties) {
                    log.error("No .editorconfig properties applicable for files under '{}'", basedirPath);
                } else {
                    log.warn("No .editorconfig properties applicable for files under '{}'", basedirPath);
                }
            }
            handler.endFiles();
        } catch (IOException e) {
            throw new GradleException(e.getMessage(), e);
        } catch (FormatException e) {
            log.fail(e.getMessage());
            formatException = e;
        }
        final StringBuilder msg = new StringBuilder(FORMAT_EXCEPTION_PREFIX);
        try {
            log.serialize(msg);
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
        throw new RuntimeException(msg.toString(), formatException);

    }

}