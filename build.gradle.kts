/**
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

import org.cadixdev.gradle.licenser.LicenseExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    java
    `java-library`
    `java-gradle-plugin`
    `maven-publish`

    id("com.gradle.plugin-publish") version "1.2.1"
    id("org.cadixdev.licenser") version "0.6.1"
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
}

group = "org.ec4j.gradle"
version = "0.1.0"
description =
    "A Gradle plugin for checking whether project files comply with format rules defined in .editorconfig files and eventually also for fixing the violations"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(16))
}

configurations.all {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 16)
}

tasks.compileJava.configure {
    options.release.set(8)
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    val ec4jLintersVersion = "0.2.1"

    compileOnly(gradleApi())
    compileOnly(localGroovy())
    implementation("org.ec4j.linters:editorconfig-linters:$ec4jLintersVersion")
    implementation("org.ec4j.linters:editorconfig-lint-api:$ec4jLintersVersion")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testCompileOnly(gradleTestKit())
    testCompileOnly("org.slf4j:slf4j-api:1.7.30")
    testCompileOnly("org.slf4j:slf4j-simple:1.7.30")
}

gradlePlugin {
    website = "https://github.com/ec4j/editorconfig-gradle-plugin"
    vcsUrl = "https://github.com/ec4j/editorconfig-gradle-plugin.git"
    plugins {
        create("editorconfigPlugin") {
            id = "org.ec4j.editorconfig"
            displayName = "EditorConfig Gradle Plugin"
            implementationClass = "org.ec4j.gradle.EditorconfigGradlePlugin"
            description = project.description
            tags = listOf("editorconfig", "lint")
        }
    }
}

tasks.withType<Test> {
    testLogging {
        showStandardStreams = true
        exceptionFormat = TestExceptionFormat.FULL
    }
    useJUnitPlatform()
}

java {
    withSourcesJar()
    withJavadocJar()
}

configure<LicenseExtension> {
    header.set(resources.text.fromFile(file("license-header-template.txt")))
    include("**/*.java")
    newLine.set(false)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {

                name.set(project.name + " " + project.version)
                description.set(project.description)
                url.set("https://github.com/ec4j/editorconfig-gradle-plugin")

                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("ppalaga")
                    }
                }

                scm {
                    url.set("https://github.com/ec4j/editorconfig-gradle-plugin")
                    connection.set("scm:https://ec4j@github.com/ec4j/editorconfig-gradle-plugin.git")
                    developerConnection.set("scm:git://github.com/ec4j/editorconfig-gradle-plugin.git")
                }

                issueManagement {
                    system.set("GitHub")
                    url.set("https://github.com/ec4j/editorconfig-gradle-plugin")
                }
            }
        }
    }
}

nexusPublishing {
    repositories {
        sonatype()
    }
}
