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

import java.util.ArrayList;
import java.util.List;

import org.ec4j.maven.lint.api.Constants;
import org.ec4j.maven.lint.api.Linter;

/**
 * An {@code editorconfig}
 * <a href="https://docs.gradle.org/current/userguide/custom_plugins.html#sec:getting_input_from_the_build">extension
 * object</a>.
 *
 * @since 0.0.1
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class EditorconfigExtension {
    private static final EditorconfigExtension DEFAULT = null;
    static final String NAME = "editorconfig";

    /**
     * @return the default {@link EditorconfigExtension} instance
     */
    public static EditorconfigExtension default_() {
        return DEFAULT;
    }

    /**
     * If set to {@code true}, the class path will be scanned for implementations of {@link Linter} and all
     * {@link Linter}s found will be added to {@link #linters} with their default includes and excludes.
     *
     * @since 0.0.1
     */
    private boolean addLintersFromClassPath = true;

    /**
     * If {@code true}, a backup file will be created for every file that needs to be formatted just before the
     * formatted version is stored. If {@code false}, no backup is done and the files are formatted in place. See also
     * {@link #backupSuffix}.
     *
     * @since 0.0.1
     */
    private boolean backup;

    /**
     * A suffix to append to a file name to create its backup. See also {@link #backup}.
     *
     * @since 0.0.1
     */
    private String backupSuffix = ".bak";

    /**
     * The default encoding of files selected by {@link #includes} and {@link #excludes}. This value can be overriden by
     * a {@code charset} property of an {@code .editorconfig} file.
     *
     * @since 0.0.1
     */
    private String encoding = "utf-8";

    /**
     * If {@code true} the default exclude patterns (that exclude binary files and other non-source code files, see
     * {@link Constants#DEFAULT_EXCLUDES}) will be prepended to the list of {@link #excludes}. Otherwise, no defaults
     * will be prepended to {@link #excludes}.
     *
     * @since 0.0.1
     */
    private boolean excludeNonSourceFiles = true;

    /**
     * File patterns to exclude from the set of files to process. The patterns are relative to the current project's
     * {@code baseDir}. See also {@link #excludeNonSourceFiles} and {@link #excludeSubmodules}.
     *
     * @since 0.0.1
     */
    private List<String> excludes = new ArrayList<>();

    /**
     * If {@code true} the Maven submodule directories of the current project will be prepended to the list of
     * {@link #excludes}. Otherwise, the module directories will not be excluded.
     *
     * @since 0.0.1
     */
    private boolean excludeSubmodules = true;

    /**
     * Tells the task what to do in case formatting violations are found. if {@code true}, all violations will be
     * reported on the console as ERRORs and the build will fail. if {@code false}, all violations will be reported on
     * the console as WARNs and the build will proceed further.
     *
     * @since 0.0.1
     */
    private boolean failOnFormatViolation = true;

    /**
     * If {@code true} the task execution will fail with an error in case no single {@code .editorconfig} property
     * matches any file of the current Maven project - this usually means that there is no {@code .editorconfig} file in
     * the whole source tree. If {@code false}, only a warning is produced in such a situation.
     *
     * @since 0.0.1
     */
    private boolean failOnNoMatchingProperties = true;

    /**
     * File patterns to include into the set of files to process. The patterns are relative to the current project's
     * {@code baseDir}.
     *
     * @since 0.0.1
     */
    private List<String> includes = new ArrayList<>();

    /**
     * Set the includes and excludes for the individual {@link Linter}s
     *
     * @since 0.0.1
     */
    private List<LinterConfig> linters = new ArrayList<>();

    public String getBackupSuffix() {
        return backupSuffix;
    }

    public String getEncoding() {
        return encoding;
    }

    public List<String> getExcludes() {
        return excludes;
    }

    public List<String> getIncludes() {
        return includes;
    }

    public List<LinterConfig> getLinters() {
        return linters;
    }

    public boolean isAddLintersFromClassPath() {
        return addLintersFromClassPath;
    }

    public boolean isBackup() {
        return backup;
    }

    public boolean isExcludeNonSourceFiles() {
        return excludeNonSourceFiles;
    }

    public boolean isExcludeSubmodules() {
        return excludeSubmodules;
    }

    public boolean isFailOnFormatViolation() {
        return failOnFormatViolation;
    }

    public boolean isFailOnNoMatchingProperties() {
        return failOnNoMatchingProperties;
    }

    public void setAddLintersFromClassPath(boolean addLintersFromClassPath) {
        this.addLintersFromClassPath = addLintersFromClassPath;
    }

    public void setBackup(boolean backup) {
        this.backup = backup;
    }

    public void setBackupSuffix(String backupSuffix) {
        this.backupSuffix = backupSuffix;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public void setExcludeNonSourceFiles(boolean excludeNonSourceFiles) {
        this.excludeNonSourceFiles = excludeNonSourceFiles;
    }

    public void setExcludes(List<String> excludes) {
        this.excludes = excludes;
    }

    public void setExcludeSubmodules(boolean excludeSubmodules) {
        this.excludeSubmodules = excludeSubmodules;
    }

    public void setFailOnFormatViolation(boolean failOnFormatViolation) {
        this.failOnFormatViolation = failOnFormatViolation;
    }

    public void setFailOnNoMatchingProperties(boolean failOnNoMatchingProperties) {
        this.failOnNoMatchingProperties = failOnNoMatchingProperties;
    }

    public void setIncludes(List<String> includes) {
        this.includes = includes;
    }

    public void setLinters(List<LinterConfig> linters) {
        this.linters = linters;
    }
}
