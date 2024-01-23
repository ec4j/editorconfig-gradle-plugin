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
package org.ec4j.gradle.runtime;

import org.ec4j.gradle.LinterConfig;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.workers.WorkParameters;

import java.io.Serializable;

/**
 * A {@link WorkParameters} for running EditorConfig based tasks
 */
public interface EditorconfigParameters extends WorkParameters, Serializable {

    Property<String> getTaskClass();

    SetProperty<String> getIncludedFiles();

    Property<String> getBasedirPath();

    Property<String> getCharset();

    Property<Boolean> getFailOnFormatViolation();

    Property<Boolean> getBackUp();

    Property<String> getBackupSuffix();

    Property<Boolean> getAddLintersFromClassPath();

    ListProperty<LinterConfig> getLinters();

    Property<Boolean> getFailOnNoMatchingProperties();

}
