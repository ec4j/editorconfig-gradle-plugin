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

import org.junit.Test;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.api.Project;
import org.gradle.api.Task;

import static org.junit.Assert.*;

public class EditorconfigGradlePluginTest {
    @Test
    public void addCheckTask() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("org.ec4j.editorconfig");
        Task task = project.getTasks().getByName("check");
        assertTrue("Found "+ task, task instanceof CheckTask);
    }

}
