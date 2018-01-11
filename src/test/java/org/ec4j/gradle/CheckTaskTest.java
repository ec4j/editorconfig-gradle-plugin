package org.ec4j.gradle;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Assert;
import org.junit.Test;

public class CheckTaskTest {
    @Test
    public void canAddTaskToProject() {
        Project project = ProjectBuilder.builder().build();
        project.getTasks().create("check", CheckTask.class);
        Task task = project.getTasks().getByName("check");
        Assert.assertTrue("found: "+ task, task instanceof CheckTask);
    }

}
