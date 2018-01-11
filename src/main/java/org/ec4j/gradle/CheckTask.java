package org.ec4j.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class CheckTask extends DefaultTask {

    @TaskAction
    public void check() {
        System.out.println("--- check");
    }
}
