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
import org.apache.ivy.plugins.resolver.IBiblioResolver;
import org.apache.ivy.plugins.resolver.IvyRepResolver;
import org.gradle.StartParameter;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.PublishArtifact;
import org.jfrog.build.api.BuildInfoProperties;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.client.ClientIvyProperties;
import org.jfrog.build.client.ClientProperties;
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.gradle.BuildInfoRecorderTask;

import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Utility class for the Artifactory-Gradle plugin.
 *
 * @author Tomer Cohen
 */
public class ArtifactoryPluginUtils {

    private static final String NEW_LINE = "\n";
    private static final String QUOTE = "'";
    private static final String M2_PER_MODULE_PATTERN
            = "[revision]/[artifact]-[revision](-[classifier]).[ext]";
    private static final String M2_PATTERN = "[organisation]/[module]/" + M2_PER_MODULE_PATTERN;
    private static final String M2_IVY_PATTERN = "[organisation]/[module]/[revision]/ivy-[revision].xml";
    public static final String BUILD_INFO_TASK_NAME = "buildInfo";


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

    public static boolean getBooleanProperty(String propertyName, Project project) {
        String value = ArtifactoryPluginUtils.getProperty(propertyName, project);
        return StringUtils.isNotBlank(value) && Boolean.parseBoolean(value);
    }

    /**
     * Appends a key-value property in compatible format for the gradle init-script build info properties collection
     * replacement
     *
     * @param stringBuilder Property collection string
     * @param key           Key to add
     * @param value         Value to add
     */
    public static void addProperty(StringBuilder stringBuilder, String key, String value) {
        key = key.replace("\\", "\\\\");
        value = value.replace("\\", "\\\\");
        value = value.replace('"', ' ');
        stringBuilder.append(QUOTE).append(key).append(QUOTE).append(":").append(QUOTE).append(value).append(QUOTE)
                .append(",").append(NEW_LINE);
    }

    public static Set<DeployDetails> getDeployArtifactsProject(Project project, String artifactName) {
        Set<DeployDetails> deployDetails = Sets.newHashSet();
        Set<Task> buildInfoTask = project.getTasksByName("buildInfo", false);
        if (buildInfoTask.isEmpty()) {
            return deployDetails;
        }
        BuildInfoRecorderTask buildInfoRecorderTask = (BuildInfoRecorderTask) buildInfoTask.iterator().next();
        Configuration configuration = buildInfoRecorderTask.getConfiguration();
        if (configuration == null) {
            return deployDetails;
        }
        String pattern = getArtifactPattern(project);
        Set<PublishArtifact> artifacts = configuration.getAllArtifacts();
        for (PublishArtifact publishArtifact : artifacts) {
            File file = publishArtifact.getFile();
            DeployDetails.Builder artifactBuilder = new DeployDetails.Builder().file(file);
            try {
                Map<String, String> checksums =
                        FileChecksumCalculator.calculateChecksums(file, "MD5", "SHA1");
                artifactBuilder.md5(checksums.get("MD5")).sha1(checksums.get("SHA1"));
            } catch (Exception e) {
                throw new RuntimeException(
                        "Failed to calculated checksums for artifact: " + file.getAbsolutePath(), e);
            }
            String revision = project.getVersion().toString();
            Map<String, String> extraTokens = Maps.newHashMap();
            if (StringUtils.isNotBlank(publishArtifact.getClassifier())) {
                extraTokens.put("classifier", publishArtifact.getClassifier());
            }
            String name = getProjectName(project, artifactName);
            artifactBuilder.artifactPath(
                    IvyPatternHelper.substitute(pattern, getGroupIdPatternByM2Compatible(project), name,
                            revision, null, publishArtifact.getType(),
                            publishArtifact.getExtension(), configuration.getName(),
                            extraTokens, null));
            String uploadId = getProperty(ClientProperties.PROP_PUBLISH_REPOKEY, project);
            artifactBuilder.targetRepository(uploadId);
            Properties matrixParams = getMatrixParams(project);
            artifactBuilder.addProperties(Maps.fromProperties(matrixParams));
            DeployDetails details = artifactBuilder.build();
            deployDetails.add(details);
        }
        return deployDetails;
    }

    public static String getArtifactPattern(Project project) {
        String pattern = getProperty(ClientIvyProperties.PROP_IVY_ARTIFACT_PATTERN, project);
        if (StringUtils.isBlank(pattern)) {
            if (isM2Compatible(project)) {
                pattern = M2_PATTERN;
            } else {
                pattern = IBiblioResolver.DEFAULT_PATTERN;
            }
        }
        return pattern.trim();
    }

    public static String getIvyDescriptorPattern(Project project) {
        String pattern = getProperty(ClientIvyProperties.PROP_IVY_IVY_PATTERN, project);
        if (StringUtils.isNotBlank(pattern)) {
            return pattern.trim();
        }
        if (isM2Compatible(project)) {
            return M2_IVY_PATTERN;
        } else {
            return IvyRepResolver.DEFAULT_IVYPATTERN;
        }
    }

    public static Set<DeployDetails> getIvyDescriptorDeployDetails(Project project, File ivyDescriptor, String artifactName) {
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
        artifactBuilder.artifactPath(IvyPatternHelper.substitute(M2_PATTERN,
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
        String buildNumber = ArtifactoryPluginUtils.getProperty(BuildInfoProperties.PROP_BUILD_NUMBER, project);
        if (StringUtils.isBlank(System.getProperty("timestamp"))) {
            System.setProperty("timestamp", String.valueOf(System.currentTimeMillis()));
        }
        props.put(ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX +
                StringUtils.removeStart(BuildInfoProperties.PROP_BUILD_NUMBER, BuildInfoProperties.BUILD_INFO_PREFIX),
                System.getProperty("timestamp", Long.toString(System.currentTimeMillis()) + ""));
        if (StringUtils.isNotBlank(buildNumber)) {
            props.put(ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX +
                    StringUtils
                            .removeStart(BuildInfoProperties.PROP_BUILD_NUMBER, BuildInfoProperties.BUILD_INFO_PREFIX),
                    buildNumber);
        }
        String buildName = getProperty(BuildInfoProperties.PROP_BUILD_NAME, project);
        if (StringUtils.isNotBlank(buildName)) {
            props.put(ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX +
                    StringUtils.removeStart(BuildInfoProperties.PROP_BUILD_NAME, BuildInfoProperties.BUILD_INFO_PREFIX),
                    buildName);
        } else {
            Project rootProject = project.getRootProject();
            props.put(ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX +
                    StringUtils.removeStart(BuildInfoProperties.PROP_BUILD_NAME, BuildInfoProperties.BUILD_INFO_PREFIX),
                    rootProject.getName().replace(' ', '-'));
        }
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
