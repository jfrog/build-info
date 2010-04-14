/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
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
            return (String) project.property(propertyName);
        } else {
            project = project.getParent();
            if (project == null) {
                return null;
            } else {
                return getProperty(propertyName, project);
            }
        }
    }
}
