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
