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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.ivy.core.IvyPatternHelper;
import org.gradle.StartParameter;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.PublishArtifact;
import org.jfrog.build.api.BuildInfoProperties;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.client.*;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.gradle.BuildInfoRecorderTask;
import org.jfrog.build.extractor.logger.GradleClientLogger;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.jfrog.build.api.BuildInfoProperties.BUILD_INFO_PROP_PREFIX;
import static org.jfrog.build.extractor.BuildInfoExtractorUtils.BUILD_INFO_PROP_PREDICATE;

/**
 * Utility class for the Artifactory-Gradle plugin.
 *
 * @author Tomer Cohen
 */
public class ArtifactoryPluginUtils {

    private static final String NEW_LINE = "\n";
    private static final String QUOTE = "'";
    public static final String BUILD_INFO_TASK_NAME = "buildInfo";

    /**
     * Returns a new client configuration handler object out of a Gradle project.
     * This method will aggregate the properties in our defined hierarchy.<br/> <ol><li>First search for
     * the property as a system property, if found return it.</li> <li>Second search for the property in the Gradle
     * {@link org.gradle.StartParameter#getProjectProperties} container and if found there, then return it.</li>
     * <li>Third search for the property in {@link org.gradle.api.Project#property(String)}</li> <li>if not found,
     * search upwards in the project hierarchy until reach the root project.</li> <li> if not found at all in this
     * hierarchy return null</li></ol>
     *
     * @param project the gradle project with properties for build info client configuration (Usually in start parameter from CI Server)
     * @return a new client configuration for this project
     */
    public static ArtifactoryClientConfiguration getArtifactoryClientConfiguration(Project project) {
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
        ArtifactoryClientConfiguration result = new ArtifactoryClientConfiguration(new GradleClientLogger(project.getLogger()));
        result.fillFromProperties(props);
        return result;
    }

    private static void fillProperties(Project project, Properties props) {
        Project parent = project.getParent();
        if (parent != null) {
            // Parent first than me
            fillProperties(parent, props);
        }
        props.putAll(project.getProperties());
    }


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


    public static Set<DeployDetails> getIvyDescriptorDeployDetails(Project project, File ivyDescriptor,
                                                                   String artifactName) {
        Set<DeployDetails> deployDetails = Sets.newHashSet();
        String uploadId = getProperty(ClientProperties.PROP_PUBLISH_REPOKEY, project);
        String pattern = getIvyDescriptorPattern(project);
        DeployDetails.Builder artifactBuilder = new DeployDetails.Builder().file(ivyDescriptor);
        try {
            Map<String, String> checksums =
                    FileChecksumCalculator.calculateChecksums(ivyDescriptor, "MD5", "SHA1");
            artifactBuilder.md5(checksums.get("MD5")).sha1(checksums.get("SHA1"));
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to calculated checksums for artifact: " + ivyDescriptor.getAbsolutePath(),
                    e);
        }
        String name = getProjectName(project, artifactName);
        artifactBuilder.artifactPath(IvyPatternHelper
                .substitute(pattern, getGroupIdPatternByM2Compatible(project), name, project.getVersion().toString(),
                        null, "ivy", "xml"));
        artifactBuilder.targetRepository(uploadId);
        Properties matrixParams = getMatrixParams(project);
        artifactBuilder.addProperties(Maps.fromProperties(matrixParams));
        DeployDetails details = artifactBuilder.build();
        deployDetails.add(details);
        return deployDetails;
    }

    public static String getProjectName(Project project, String artifactName) {
        String name = project.getName();
        if (StringUtils.isNotBlank(artifactName)) {
            name = artifactName;
        }
        return name;
    }

    public static String getGroupIdPatternByM2Compatible(Project project) {
        String groupId = project.getGroup().toString();
        if (isM2Compatible(project)) {
            groupId = groupId.replace(".", "/");
        }
        return groupId;
    }

    private static boolean isM2Compatible(Project project) {
        String m2Compatible = getProperty(ClientIvyProperties.PROP_M2_COMPATIBLE, project);
        return Boolean.parseBoolean(m2Compatible);
    }

    public static DeployDetails getMavenDeployDetails(Project project, File mavenDescriptor, String artifactName) {
        String uploadId = getProperty(ClientProperties.PROP_PUBLISH_REPOKEY, project);
        DeployDetails.Builder artifactBuilder = new DeployDetails.Builder().file(mavenDescriptor);
        try {
            Map<String, String> checksums =
                    FileChecksumCalculator.calculateChecksums(mavenDescriptor, "MD5", "SHA1");
            artifactBuilder.md5(checksums.get("MD5")).sha1(checksums.get("SHA1"));
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to calculated checksums for artifact: " + mavenDescriptor.getAbsolutePath(),
                    e);
        }
        String name = getProjectName(project, artifactName);
        artifactBuilder.artifactPath(IvyPatternHelper.substitute(LayoutPatterns.M2_PATTERN,
                project.getGroup().toString().replace(".", "/"), name,
                project.getVersion().toString(), null, "pom", "pom"));
        artifactBuilder.targetRepository(uploadId);
        Properties matrixParams = getMatrixParams(project);
        artifactBuilder.addProperties(Maps.fromProperties(matrixParams));
        DeployDetails details = artifactBuilder.build();
        return details;
    }


    private static Properties getMatrixParams(Project project) {
        Properties props = new Properties();
        props.putAll(project.getGradle().getStartParameter().getProjectProperties());
        props.putAll(System.getProperties());

        if (StringUtils.isBlank(System.getProperty("timestamp"))) {
            System.setProperty("timestamp", String.valueOf(System.currentTimeMillis()));
        }

        String buildName = getProperty(BuildInfoProperties.PROP_BUILD_NAME, project);
        if (StringUtils.isBlank(buildName)) {
            Project rootProject = project.getRootProject();
            try {
                buildName = URLEncoder.encode(rootProject.getName(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new GradleException("JDK does not have UTF-8!", e);
            }
        }
        props.put(ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX +
                StringUtils.removeStart(BuildInfoProperties.PROP_BUILD_NAME, BuildInfoProperties.BUILD_INFO_PREFIX),
                buildName);

        String buildNumber = ArtifactoryPluginUtils.getProperty(BuildInfoProperties.PROP_BUILD_NUMBER, project);
        if (StringUtils.isBlank(buildNumber)) {
            buildNumber = System.getProperty("timestamp");
        }
        props.put(ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX +
                StringUtils.removeStart(BuildInfoProperties.PROP_BUILD_NUMBER, BuildInfoProperties.BUILD_INFO_PREFIX),
                buildNumber);

        String buildTimestamp = ArtifactoryPluginUtils.getProperty(BuildInfoProperties.PROP_BUILD_TIMESTAMP, project);
        if (StringUtils.isBlank(buildTimestamp)) {
            buildTimestamp = System.getProperty("timestamp");   // this must hold value
        }
        props.put(ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX +
                StringUtils.removeStart(BuildInfoProperties.PROP_BUILD_TIMESTAMP,
                        BuildInfoProperties.BUILD_INFO_PREFIX),
                buildTimestamp);

        String buildParentNumber =
                ArtifactoryPluginUtils.getProperty(BuildInfoProperties.PROP_PARENT_BUILD_NUMBER, project);
        if (StringUtils.isNotBlank(buildParentNumber)) {
            props.put(ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX +
                    StringUtils
                            .removeStart(BuildInfoProperties.PROP_PARENT_BUILD_NUMBER,
                                    BuildInfoProperties.BUILD_INFO_PREFIX),
                    buildParentNumber);
        }

        String buildParentName = getProperty(BuildInfoProperties.PROP_PARENT_BUILD_NAME, project);
        if (StringUtils.isNotBlank(buildParentName)) {
            props.put(ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX +
                    StringUtils.removeStart(BuildInfoProperties.PROP_PARENT_BUILD_NAME,
                            BuildInfoProperties.BUILD_INFO_PREFIX),
                    buildParentName);
        }

        String vcsRevision = ArtifactoryPluginUtils.getProperty(BuildInfoProperties.PROP_VCS_REVISION, project);
        if (StringUtils.isNotBlank(vcsRevision)) {
            props.put(ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX +
                    StringUtils
                            .removeStart(BuildInfoProperties.PROP_VCS_REVISION, BuildInfoProperties.BUILD_INFO_PREFIX),
                    vcsRevision);
        }

        Map properties = project.getProperties();
        Set<String> keys = properties.keySet();
        for (String key : keys) {
            if (key != null) {
                Object value = properties.get(key);
                if (value != null) {
                    value = value.toString();
                    props.put(key, value);
                }
            }
        }
        Properties filtered =
                BuildInfoExtractorUtils.filterDynamicProperties(props, BuildInfoExtractorUtils.MATRIX_PARAM_PREDICATE);
        Properties strippedProps = new Properties();
        for (Map.Entry<Object, Object> entry : filtered.entrySet()) {
            String key = entry.getKey().toString();
            strippedProps.setProperty(StringUtils.removeStart(key, ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX),
                    entry.getValue().toString());
        }
        return strippedProps;
    }
}
