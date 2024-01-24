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

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Adds {@link EditorconfigExtension}, {@link EditorconfigCheckTask} and {@link EditorconfigFormatTask}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class EditorconfigGradlePlugin implements Plugin<Project> {
    public static final String CONFIGURATION_NAME = "editorconfig";

    /** {@inheritDoc} */
    @Override
    public void apply(Project project) {
        project.getExtensions().create(EditorconfigExtension.NAME, EditorconfigExtension.class);
        project.getConfigurations().maybeCreate(CONFIGURATION_NAME);

        project.getTasks().create(EditorconfigCheckTask.NAME, EditorconfigCheckTask.class);
        project.getTasks().create(EditorconfigFormatTask.NAME, EditorconfigFormatTask.class);
    }

}
