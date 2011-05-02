/*
 * Copyright (C) 2011 JFrog Ltd.
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

package org.jfrog.build.extractor.gradle;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import org.gradle.StartParameter;
import org.gradle.api.Project;
import org.jfrog.build.client.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.dsl.ArtifactoryPluginConvention;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Properties;

import static org.jfrog.build.api.BuildInfoProperties.BUILD_INFO_PROP_PREFIX;
import static org.jfrog.build.extractor.BuildInfoExtractorUtils.BUILD_INFO_PROP_PREDICATE;

/**
 * Utility class for the Artifactory-Gradle plugin.
 *
 * @author Tomer Cohen
 */
public class GradlePluginUtils {

    public static final String BUILD_INFO_TASK_NAME = "buildInfo";

    /**
     * Returns a  configuration handler object out of a Gradle project. This method will aggregate the properties in our
     * defined hierarchy.<br/> <ol><li>First search for the property as a system property, if found return it.</li>
     * <li>Second search for the property in the Gradle {@link org.gradle.StartParameter#getProjectProperties} container
     * and if found there, then return it.</li> <li>Third search for the property in {@link
     * org.gradle.api.Project#property(String)}</li> <li>if not found, search upwards in the project hierarchy until
     * reach the root project.</li> <li> if not found at all in this hierarchy return null</li></ol>
     *
     * @param project the gradle project with properties for build info client configuration (Usually in start parameter
     *                from CI Server)
     */
    public static void fillArtifactoryClientConfiguration(ArtifactoryClientConfiguration configuration,
            Project project) {
        Properties props = new Properties();
        // First aggregate properties from parent to child
        fillProperties(project, props);
        // Then start parameters
        StartParameter startParameter = project.getGradle().getStartParameter();
        props.putAll(startParameter.getProjectProperties());
        // Then System properties
        Properties mergedProps = BuildInfoExtractorUtils.mergePropertiesWithSystemAndPropertyFile(props);
        // Then special buildInfo properties
        Properties buildInfoProperties =
                BuildInfoExtractorUtils.filterDynamicProperties(mergedProps, BUILD_INFO_PROP_PREDICATE);
        buildInfoProperties =
                BuildInfoExtractorUtils.stripPrefixFromProperties(buildInfoProperties, BUILD_INFO_PROP_PREFIX);
        props.putAll(buildInfoProperties);
        configuration.fillFromProperties(mergedProps);
    }

    private static void fillProperties(Project project, Properties props) {
        Project parent = project.getParent();
        if (parent != null) {
            // Parent first than me
            fillProperties(parent, props);
        }
        Map<String, ?> projectProperties = project.getProperties();
        projectProperties = Maps.filterValues(projectProperties, new Predicate<Object>() {
            public boolean apply(@Nullable Object input) {
                return input != null && input instanceof String;
            }
        });
        props.putAll(projectProperties);
    }

    public static ArtifactoryPluginConvention getArtifactoryConvention(Project project) {
        return project.getRootProject().getConvention().getPlugin(ArtifactoryPluginConvention.class);
    }
}
