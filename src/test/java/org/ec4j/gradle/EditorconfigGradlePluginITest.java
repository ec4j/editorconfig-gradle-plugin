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

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.testkit.runner.UnexpectedBuildSuccess;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Objects.requireNonNull;

public class EditorconfigGradlePluginITest {

    private static final Path buildProjectsPath;
    private static final Logger log = LoggerFactory.getLogger(EditorconfigGradlePluginITest.class);
    private static final Path projectDir = Paths.get(System.getProperty("project.dir", "."));
    private static final Path srcProjectsPath;
    static {
        srcProjectsPath = projectDir.resolve("src/test/projects");
        buildProjectsPath = projectDir.resolve("build/test-projects");
    }

    private static void assertLogText(String projectName, String logText, String needle) {
        if (!logText.contains(needle)) {
            Assert.fail("Log text of '" + projectName + "' should contain '" + needle + "'\n\n" + logText);
        }
    }

    private static void assertNoLogText(String projectName, String logText, String needle) {
        if (logText.contains(needle)) {
            Assert.fail("Log text of '" + projectName + "' should not contain '" + needle + "'\n\n" + logText);
        }
    }

    @BeforeClass
    public static void beforeClass() throws IOException {
        IoTestUtils.deleteDirectory(buildProjectsPath);
    }

    //private Path testProjectPath;

    private static void assertFilesEqual(Path actualBaseDir, Path expectedBaseDir, String relPath) throws IOException {
        final String contentActual = new String(Files.readAllBytes(actualBaseDir.resolve(relPath)),
                StandardCharsets.UTF_8);
        final String contentExpected = new String(Files.readAllBytes(expectedBaseDir.resolve(relPath)),
                StandardCharsets.UTF_8);
        Assert.assertEquals(relPath, contentExpected, contentActual);
    }

    @Test
    public void checkDefaults() throws IOException {
        final String projectName = "defaults";
        Path testProjectPath = init(projectName, "checkDefaults");

        try {
            BuildResult result = GradleRunner.create().withProjectDir(testProjectPath.toFile()).withArguments( //
                    EditorconfigCheckTask.NAME //
                    , "--debug" //
                    , "--stacktrace" //
            ) //
                    .withDebug(true)
                    .withPluginClasspath() //
                    .buildAndFail();

            final String logText = result.getOutput();
            Files.write(testProjectPath.resolve("log.txt"), logText.getBytes(StandardCharsets.UTF_8));

            Assert.assertEquals(TaskOutcome.FAILED, requireNonNull(result.task(":" + EditorconfigCheckTask.NAME)).getOutcome());

            assertLogText(projectName, logText,
                    "Processing file '.editorconfig' using linter org.ec4j.maven.linters.TextLinter");
            assertLogText(projectName, logText, "No formatting violations found in file '.editorconfig'");
            assertLogText(projectName, logText,
                    "Processing file 'build.gradle' using linter org.ec4j.maven.linters.TextLinter");
            assertLogText(projectName, logText, "No formatting violations found in file 'build.gradle'");
            assertLogText(projectName, logText,
                    "Processing file 'src/main/java/org/ec4j/maven/it/defaults/App.java' using linter org.ec4j.maven.linters.TextLinter"
                            .replace('/', File.separatorChar));
            assertLogText(projectName, logText,
                    "No formatting violations found in file 'src/main/java/org/ec4j/maven/it/defaults/App.java'"
                            .replace('/', File.separatorChar));
            assertLogText(projectName, logText,
                    "Processing file 'src/main/resources/trailing-whitespace.txt' using linter org.ec4j.maven.linters.TextLinter"
                            .replace('/', File.separatorChar));
            assertLogText(projectName, logText,
                    "src/main/resources/trailing-whitespace.txt@1,7: Delete 2 characters - violates trim_trailing_whitespace = true, reported by org.ec4j.maven.linters.TextLinter"
                            .replace('/', File.separatorChar));
            assertLogText(projectName, logText,
                    "Processing file 'src/main/resources/indent.xml' using linter org.ec4j.maven.linters.TextLinter"
                            .replace('/', File.separatorChar));
            assertLogText(projectName, logText,
                    "Processing file 'src/main/resources/indent.xml' using linter org.ec4j.maven.linters.XmlLinter"
                            .replace('/', File.separatorChar));
            assertLogText(projectName, logText,
                    "src/main/resources/indent.xml@23,5: Delete 1 character - violates indent_style = space, indent_size = 2, reported by org.ec4j.maven.linters.XmlLinter"
                            .replace('/', File.separatorChar));
            assertLogText(projectName, logText,
                    "src/main/resources/indent.xml@24,3: Delete 2 characters - violates indent_style = space, indent_size = 2, reported by org.ec4j.maven.linters.XmlLinter"
                            .replace('/', File.separatorChar));
            assertLogText(projectName, logText,
                    "Processing file 'README.adoc' using linter org.ec4j.maven.linters.TextLinter");
            assertLogText(projectName, logText,
                    "README.adoc@2,1: Delete 2 characters - violates trim_trailing_whitespace = true, reported by org.ec4j.maven.linters.TextLinter");
            assertLogText(projectName, logText, "Checked 7 files");
            assertLogText(projectName, logText, ":" + EditorconfigCheckTask.NAME + " FAILED");
            assertLogText(projectName, logText, "There are .editorconfig violations. You may want to run");
            assertLogText(projectName, logText, "./gradlew editorconfigFormat");
            assertLogText(projectName, logText, "to fix them automagically.");
        } catch (UnexpectedBuildSuccess e) {
            final BuildResult result = e.getBuildResult();
            final String logText = result.getOutput();
            Files.write(testProjectPath.resolve("log.txt"), logText.getBytes(StandardCharsets.UTF_8));
            throw e;
        }

    }

    @Test
    public void checkExtension() throws IOException {
        final String projectName = "extension";
        final Path testProjectPath = init(projectName, "checkExtension");

        try {
            BuildResult result = GradleRunner.create().withProjectDir(testProjectPath.toFile()).withArguments( //
                    EditorconfigCheckTask.NAME //
                    , "--debug" //
            // , "--stacktrace" //
            ) //
                    // .withDebug(true)
                    .withPluginClasspath() //
                    .buildAndFail();

            final String logText = result.getOutput();
            Files.write(testProjectPath.resolve("log.txt"), logText.getBytes(StandardCharsets.UTF_8));

            Assert.assertEquals(TaskOutcome.FAILED, requireNonNull(result.task(":" + EditorconfigCheckTask.NAME)).getOutcome());


            assertNoLogText(projectName, logText,
                    "Processing file '.editorconfig' using linter org.ec4j.maven.linters.TextLinter");
            assertNoLogText(projectName, logText,
                    "Processing file 'build.gradle' using linter org.ec4j.maven.linters.TextLinter");
            assertLogText(projectName, logText,
                    "Processing file 'src/main/java/org/ec4j/maven/it/defaults/App.java' using linter org.ec4j.maven.linters.TextLinter"
                            .replace('/', File.separatorChar));
            assertLogText(projectName, logText,
                    "No formatting violations found in file 'src/main/java/org/ec4j/maven/it/defaults/App.java'"
                            .replace('/', File.separatorChar));
            assertLogText(projectName, logText,
                    "Processing file 'src/main/resources/trailing-whitespace.txt' using linter org.ec4j.maven.linters.TextLinter"
                            .replace('/', File.separatorChar));
            assertLogText(projectName, logText,
                    "src/main/resources/trailing-whitespace.txt@1,7: Delete 2 characters - violates trim_trailing_whitespace = true, reported by org.ec4j.maven.linters.TextLinter"
                            .replace('/', File.separatorChar));
            assertNoLogText(projectName, logText,
                    "Processing file 'src/main/resources/indent.xml' using linter org.ec4j.maven.linters.TextLinter"
                            .replace('/', File.separatorChar));
            assertNoLogText(projectName, logText,
                    "Processing file 'README.adoc' using linter org.ec4j.maven.linters.TextLinter");
            assertLogText(projectName, logText, "Checked 2 files");
            assertLogText(projectName, logText, ":" + EditorconfigCheckTask.NAME + " FAILED");
            assertLogText(projectName, logText, "There are .editorconfig violations. You may want to run");
            assertLogText(projectName, logText, "./gradlew editorconfigFormat");
            assertLogText(projectName, logText, "to fix them automagically.");
        } catch (UnexpectedBuildSuccess e) {
            final BuildResult result = e.getBuildResult();
            final String logText = result.getOutput();
            Files.write(testProjectPath.resolve("log.txt"), logText.getBytes(StandardCharsets.UTF_8));
            throw e;
        }


    }

    @Test
    public void formatDefaults() throws IOException {
        final String projectName = "defaults";
        final Path testProjectPath = init(projectName, "formatDefaults");

        BuildResult result = GradleRunner.create().withProjectDir(testProjectPath.toFile()).withArguments( //
                EditorconfigFormatTask.NAME //
                , "--debug" //
        // , "--stacktrace" //
        ) //
                // .withDebug(true)
                .withPluginClasspath() //
                .build();

        final String logText = result.getOutput();
        Files.write(testProjectPath.resolve("log.txt"), logText.getBytes(StandardCharsets.UTF_8));

        Assert.assertEquals(TaskOutcome.SUCCESS, requireNonNull(result.task(":" + EditorconfigFormatTask.NAME)).getOutcome());

        assertLogText(projectName, logText,
                "Processing file '.editorconfig' using linter org.ec4j.maven.linters.TextLinter");
        assertLogText(projectName, logText, "No formatting violations found in file '.editorconfig'");
        assertLogText(projectName, logText,
                "Processing file 'build.gradle' using linter org.ec4j.maven.linters.TextLinter");
        assertLogText(projectName, logText, "No formatting violations found in file 'build.gradle'");
        assertLogText(projectName, logText,
                "Processing file 'src/main/java/org/ec4j/maven/it/defaults/App.java' using linter org.ec4j.maven.linters.TextLinter"
                        .replace('/', File.separatorChar));
        assertLogText(projectName, logText,
                "No formatting violations found in file 'src/main/java/org/ec4j/maven/it/defaults/App.java'"
                        .replace('/', File.separatorChar));
        assertLogText(projectName, logText,
                "Processing file 'src/main/resources/trailing-whitespace.txt' using linter org.ec4j.maven.linters.TextLinter"
                        .replace('/', File.separatorChar));
        assertLogText(projectName, logText,
                "src/main/resources/trailing-whitespace.txt@1,7: Delete 2 characters - violates trim_trailing_whitespace = true, reported by org.ec4j.maven.linters.TextLinter"
                        .replace('/', File.separatorChar));
        assertLogText(projectName, logText,
                "Processing file 'src/main/resources/indent.xml' using linter org.ec4j.maven.linters.TextLinter"
                        .replace('/', File.separatorChar));
        assertLogText(projectName, logText,
                "Processing file 'src/main/resources/indent.xml' using linter org.ec4j.maven.linters.XmlLinter"
                        .replace('/', File.separatorChar));
        assertLogText(projectName, logText,
                "src/main/resources/indent.xml@23,5: Delete 1 character - violates indent_style = space, indent_size = 2, reported by org.ec4j.maven.linters.XmlLinter"
                        .replace('/', File.separatorChar));
        assertLogText(projectName, logText,
                "src/main/resources/indent.xml@24,3: Delete 2 characters - violates indent_style = space, indent_size = 2, reported by org.ec4j.maven.linters.XmlLinter"
                        .replace('/', File.separatorChar));
        assertLogText(projectName, logText,
                "Processing file 'README.adoc' using linter org.ec4j.maven.linters.TextLinter");
        assertLogText(projectName, logText,
                "README.adoc@2,1: Delete 2 characters - violates trim_trailing_whitespace = true, reported by org.ec4j.maven.linters.TextLinter");
        assertLogText(projectName, logText, "Formatted 3 out of 7 files");
        assertLogText(projectName, logText, ":" + EditorconfigFormatTask.NAME + " finished executing");

        final Path expectedBaseDir = srcProjectsPath.resolve(projectName + "-formatted");
        assertFilesEqual(testProjectPath, expectedBaseDir, ".editorconfig");
        assertFilesEqual(testProjectPath, expectedBaseDir, "build.gradle");
        assertFilesEqual(testProjectPath, expectedBaseDir, "README.adoc");
        assertFilesEqual(testProjectPath, expectedBaseDir, "src/main/java/org/ec4j/maven/it/defaults/App.java");
        assertFilesEqual(testProjectPath, expectedBaseDir, "src/main/resources/indent.xml");
        assertFilesEqual(testProjectPath, expectedBaseDir, "src/main/resources/trailing-whitespace.txt");

    }

    private static Path init(String projectName, String testName) throws IOException {
        final Path testProjectPath = buildProjectsPath.resolve(testName);
        IoTestUtils.deleteDirectory(testProjectPath);
        IoTestUtils.copyDirectory(srcProjectsPath.resolve(projectName), testProjectPath);
        return testProjectPath;
    }

}