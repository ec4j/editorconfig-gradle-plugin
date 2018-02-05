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

import java.nio.charset.Charset;
import java.nio.file.Path;

import org.ec4j.maven.lint.api.EditableResource;
import org.ec4j.maven.lint.api.FormattingHandler;
import org.ec4j.maven.lint.api.Resource;
import org.ec4j.maven.lint.api.ViolationHandler;

/**
 * Formats a set of files so that they comply with rules defined in {@code .editorconfig} files.
 *
 * @since 0.0.1
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class EditorconfigFormatTask extends AbstractEditorconfigTask {
    public static final String NAME = "editorconfigFormat";

    /** {@inheritDoc} */
    @Override
    protected ViolationHandler createHandler() {
        return new FormattingHandler(editorconfigExtension.isBackup(), editorconfigExtension.getBackupSuffix());
    }

    /** {@inheritDoc} */
    @Override
    protected Resource createResource(Path absFile, Path relFile, Charset encoding) {
        return new EditableResource(absFile, relFile, encoding);
    }
}
