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

import javax.inject.Inject;

import org.gradle.workers.WorkerExecutor;

/**
 * Checks whether files are formatted according to rules defined in {@code .editorconfig} files. If fomat violations are
 * detected, either causes the build to fail (if {@link EditorconfigExtension#isFailOnFormatViolation()} is
 * {@code true}) or just produces a warning.
 *
 * @since 0.0.1
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class EditorconfigCheckTask extends AbstractEditorconfigTask {

    public static final String NAME = "editorconfigCheck";

    @Inject
    public EditorconfigCheckTask(WorkerExecutor workerExecutor) {
        super(workerExecutor);
    }

}
