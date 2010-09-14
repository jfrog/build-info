/*
 * Copyright (C) 2010 JFrog Ltd.
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

package org.jfrog.build;

import org.gradle.StartParameter;
import org.gradle.api.Project;

import java.util.Map;

/**
 * Utility class for the Artifactory-Gradle plugin.
 *
 * @author Tomer Cohen
 */
public class ArtifactoryPluginUtils {

    private static final String NEW_LINE = "\n";
    private static final String QUOTE = "'";

    /**
     * Get a property, this method will search for a property in our defined hierarchy.<br/> <ol><li>First search for
     * the property as a system property, if found return it.</li> <li>Second search for the property in the Gradle
     * {@link org.gradle.StartParameter#getProjectProperties} container and if found there, then return it.</li>
     * <li>Third search for the property in {@link org.gradle.api.Project#property(String)}</li> <li>if not found,
     * search upwards in the project hierarchy until reach the root project.</li> <li> if not found at all in this
     * hierarchy return null</li></ol>
     *
     * @param propertyName The property name to search.
     * @param project      The starting project from where to start the property search.
     * @return The property value if found, {@code null} otherwise.
     */
    public static String getProperty(String propertyName, Project project) {
        if (System.getProperty(propertyName) != null) {
            return System.getProperty(propertyName);
        }
        StartParameter startParameter = project.getGradle().getStartParameter();
        Map<String, String> projectProperties = startParameter.getProjectProperties();
        if (projectProperties != null) {
            String propertyValue = projectProperties.get(propertyName);
            if (propertyValue != null) {
                return propertyValue;
            }
        }
        if (project.hasProperty(propertyName)) {
            return project.property(propertyName).toString();
        } else {
            project = project.getParent();
            if (project == null) {
                return null;
            } else {
                return getProperty(propertyName, project);
            }
        }
    }

    /**
     * Appends a key-value property in compatible format for the gradle init-script build info properties collection
     * replacement
     *
     * @param stringBuilder Property collection string
     * @param key Key to add
     * @param value Value to add
     */
    public static void addProperty(StringBuilder stringBuilder, String key, String value) {
        key = key.replace("\\", "\\\\");
        value = value.replace("\\", "\\\\");
        value = value.replace('"', ' ');
        stringBuilder.append(QUOTE).append(key).append(QUOTE).append(":").append(QUOTE).append(value).append(QUOTE)
                .append(",").append(NEW_LINE);
    }
}
