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

package org.jfrog.gradle.plugin.artifactory.task.helper;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.tools.ant.util.FileUtils;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.artifacts.PublishArtifactSet;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.plugins.MavenPluginConvention;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.Upload;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.LayoutPatterns;
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPluginUtil;
import org.jfrog.gradle.plugin.artifactory.extractor.GradleDeployDetails;
import org.jfrog.gradle.plugin.artifactory.extractor.PublishArtifactInfo;
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Fred Simon
 */
public class TaskHelperConfigurations extends TaskHelper {
    private static final Logger log = Logging.getLogger(TaskHelperConfigurations.class);
    public static final String ARCHIVES_BASE_NAME = "archivesBaseName";
    private Set<Configuration> publishConfigurations;
    private boolean publishConfigsSpecified;
    private Set<Object> configurations = new HashSet<>();

    public TaskHelperConfigurations(ArtifactoryTask artifactoryTask) {
        super(artifactoryTask);
        publishConfigurations = artifactoryTask.publishConfigs;
    }

    public void addCollection(Object... objects) {
        Collections.addAll(this.configurations, objects);
    }

    private void publishConfigs() {
        if (configurations == null || configurations.size() == 0) {
            return;
        }
        for (Object conf : configurations) {
            if (conf instanceof CharSequence) {
                Configuration projectConfig = getProject().getConfigurations().findByName(conf.toString());
                if (projectConfig != null) {
                    publishConfigurations.add(projectConfig);
                } else {
                    logConfigurationNotFound(conf.toString());
                }
            } else if (conf instanceof Configuration) {
                publishConfigurations.add((Configuration) conf);
            } else {
                log.error("Configuration type '{}' not supported in task '{}'.",
                        new Object[]{conf.getClass().getName(), getPath()});
            }
        }
        publishConfigsSpecified = true;
    }

    private void logConfigurationNotFound(String configName) {
        log.debug("Configuration named '{}' does not exist for project '{}' in task '{}'.",
                configName, getProject().getPath(), getPath());
    }

    public Set<Configuration> getPublishConfigurations() {
        return publishConfigurations;
    }

    public boolean hasConfigurations() {
        return !publishConfigurations.isEmpty();
    }

    public void collectDescriptorsAndArtifactsForUpload() throws IOException {
        Set<GradleDeployDetails> deployDetailsFromProject = getArtifactDeployDetails();
        artifactoryTask.deployDetails.addAll(deployDetailsFromProject);

        // In case the build is configured to do so, add the ivy and maven descriptors if they exist
        if (isPublishIvy()) {
            if (artifactoryTask.ivyDescriptor != null && artifactoryTask.ivyDescriptor.exists()) {
                artifactoryTask.deployDetails.add(getIvyDescriptorDeployDetails());
            }
        }
        if (isPublishMaven()) {
            if (artifactoryTask.mavenDescriptor != null && artifactoryTask.mavenDescriptor.exists()) {
                artifactoryTask.deployDetails.add(getMavenDeployDetails());
            }
        }
    }

    /**
     * Check all files to publish, depends on it (to generate Gradle task graph to create them).
     */
    public void checkDependsOnArtifactsToPublish() {
        publishConfigs();
        if (!hasConfigurations()) {
            return;
        }
        // The task depends on the produced artifacts of all configurations "to publish"
        for (Configuration publishConfiguration : publishConfigurations) {
            dependsOn(publishConfiguration.getArtifacts());
        }

        // Set ivy descriptor parameters
        if (isPublishIvy()) {
            if (artifactoryTask.ivyDescriptor == null) {
                setDefaultIvyDescriptor();
            }
        } else {
            artifactoryTask.ivyDescriptor = null;
        }

        // Set maven pom parameters
        if (isPublishMaven()) {
            if (artifactoryTask.mavenDescriptor == null) {
                setDefaultMavenDescriptor();
            }
        } else {
            artifactoryTask.mavenDescriptor = null;
        }
    }

    public boolean hasModules() {
        return hasConfigurations();
    }

    protected void setDefaultIvyDescriptor() {
        Project project = getProject();
        TaskContainer tasks = project.getTasks();
        Configuration archiveConfig = project.getConfigurations().findByName(Dependency.ARCHIVES_CONFIGURATION);
        if (archiveConfig == null) {
            log.warn("Cannot publish Ivy descriptor if ivyDescriptor not set in task '{}' " +
                            "and no '{}' configuration exists in project '{}'.", Dependency.ARCHIVES_CONFIGURATION,
                    project.getPath());
        } else {
            // Flag to publish the Ivy XML file, but no ivy descriptor file inputted, activate default upload${configuration}.
            // ATTENTION: Tasks not part of the execution graph have withType(Upload.class) false ?!? Need to check for type our self.
            Task candidateUploadTask = tasks.findByName(archiveConfig.getUploadTaskName());
            if (candidateUploadTask == null) {
                log.warn("Cannot publish Ivy descriptor if ivyDescriptor not set in task '{}' " +
                                "and task '{}' does not exist." +
                                "\nAdding \"apply plugin: 'java'\" or any other plugin extending the 'base' plugin" +
                                "will solve this issue.",
                        new Object[]{getPath(), archiveConfig.getUploadTaskName()});
            } else {
                if (!(candidateUploadTask instanceof Upload)) {
                    log.warn("Cannot publish Ivy descriptor if ivyDescriptor not set in task '{}' " +
                                    "and task '{}' is not an Upload task." +
                                    "\nYou'll need to set publishIvy=false or provide a path to the ivy file to " +
                                    "publish to solve this issue.",
                            new Object[]{getPath(), archiveConfig.getUploadTaskName()});
                } else {
                    Upload uploadTask = (Upload) candidateUploadTask;
                    if (!uploadTask.isUploadDescriptor()) {
                        log.info("Forcing task '{}' to upload its Ivy descriptor (uploadDescriptor was false).",
                                uploadTask.getPath());
                        uploadTask.setUploadDescriptor(true);
                    }
                    artifactoryTask.ivyDescriptor = uploadTask.getDescriptorDestination();
                    dependsOn(candidateUploadTask);
                }
            }
        }
    }

    protected void setDefaultMavenDescriptor() {
        // Flag to publish the Maven POM, but no pom file inputted, activate default Maven "install" task.
        // if the project doesn't have the maven install task, warn
        Project project = getProject();
        TaskContainer tasks = project.getTasks();
        Upload installTask = tasks.withType(Upload.class).findByName("install");
        if (installTask == null) {
            log.warn("Cannot publish pom for project '{}' since it does not contain the Maven " +
                            "plugin install task and task '{}' does not specify a custom pom path.",
                    new Object[]{project.getPath(), getPath()});
            artifactoryTask.mavenDescriptor = null;
        } else {
            artifactoryTask.mavenDescriptor = new File(
                    project.getConvention().getPlugin(MavenPluginConvention.class).getMavenPomDir(),
                    "pom-default.xml");
            dependsOn(installTask);
        }
    }

    protected Set<GradleDeployDetails> getArtifactDeployDetails() {

        Set<GradleDeployDetails> deployDetails = Sets.newLinkedHashSet();
        if (!hasConfigurations()) {
            log.info("No configurations to publish for project '{}'.", getProject().getPath());
            return deployDetails;
        }

        Set<String> processedFiles = Sets.newHashSet();
        for (Configuration configuration : publishConfigurations) {
            PublishArtifactSet artifacts = configuration.getAllArtifacts();
            for (PublishArtifact artifact : artifacts) {
                GradleDeployDetails gdd = gradleDeployDetails(artifact, configuration.getName(), processedFiles);
                if (gdd != null) {
                    deployDetails.add(gdd);
                }
            }
        }
        return deployDetails;
    }

    public boolean AddDefaultArchiveConfiguration(Project project) {
        if (!hasConfigurations()) {
            if (publishConfigsSpecified) {
                log.warn("None of the specified publish configurations matched for project '{}' - nothing to publish.",
                        project.getPath());
                return true;
            } else {
                Configuration archiveConfig = project.getConfigurations().findByName(Dependency.ARCHIVES_CONFIGURATION);
                if (archiveConfig != null) {
                    log.info("No publish configurations specified for project '{}' - using the default '{}' " +
                            "configuration.", project.getPath(), Dependency.ARCHIVES_CONFIGURATION);
                    publishConfigurations.add(archiveConfig);
                } else {
                    log.warn("No publish configurations specified for project '{}' and the default '{}' " +
                            "configuration does not exist.", project.getPath(), Dependency.ARCHIVES_CONFIGURATION);
                    return true;
                }
            }
        }
        return false;
    }

    private GradleDeployDetails getIvyDescriptorDeployDetails() {
        ArtifactoryClientConfiguration.PublisherHandler publisher =
                ArtifactoryPluginUtil.getPublisherHandler(getProject());
        DeployDetails.Builder artifactBuilder = new DeployDetails.Builder().file(artifactoryTask.ivyDescriptor);
        try {
            Map<String, String> checksums =
                    FileChecksumCalculator.calculateChecksums(artifactoryTask.ivyDescriptor, "MD5", "SHA1");
            artifactBuilder.md5(checksums.get("MD5")).sha1(checksums.get("SHA1"));
        } catch (Exception e) {
            throw new GradleException(
                    "Failed to calculate checksums for artifact: " + artifactoryTask.ivyDescriptor.getAbsolutePath(), e);
        }
        String gid = getProject().getGroup().toString();
        if (publisher.isM2Compatible()) {
            gid = gid.replace(".", "/");
        }
        artifactBuilder.artifactPath(IvyPatternHelper
                .substitute(publisher.getIvyPattern(), gid, getModuleName(),
                        getProject().getVersion().toString(), null, "ivy", "xml"));
        artifactBuilder.targetRepository(publisher.getRepoKey());
        PublishArtifactInfo artifactInfo =
                new PublishArtifactInfo(artifactoryTask.ivyDescriptor.getName(), "xml", "ivy", null,
                        artifactoryTask.ivyDescriptor);
        Map<String, String> propsToAdd = getPropsToAdd(artifactInfo, null);
        artifactBuilder.addProperties(propsToAdd);
        return new GradleDeployDetails(artifactInfo, artifactBuilder.build(), getProject());
    }

    private GradleDeployDetails getMavenDeployDetails() {
        ArtifactoryClientConfiguration.PublisherHandler publisher =
                ArtifactoryPluginUtil.getPublisherHandler(getProject());
        DeployDetails.Builder artifactBuilder = new DeployDetails.Builder().file(artifactoryTask.mavenDescriptor);
        try {
            Map<String, String> checksums =
                    FileChecksumCalculator.calculateChecksums(artifactoryTask.mavenDescriptor, "MD5", "SHA1");
            artifactBuilder.md5(checksums.get("MD5")).sha1(checksums.get("SHA1"));
        } catch (Exception e) {
            throw new GradleException(
                    "Failed to calculate checksums for artifact: " + artifactoryTask.mavenDescriptor.getAbsolutePath(), e);
        }
        // for pom files always enforce the M2 pattern
        artifactBuilder.artifactPath(IvyPatternHelper.substitute(LayoutPatterns.M2_PATTERN,
                getProject().getGroup().toString().replace(".", "/"), getModuleName(),
                getProject().getVersion().toString(), null, "pom", "pom"));
        artifactBuilder.targetRepository(publisher.getRepoKey());
        PublishArtifactInfo artifactInfo =
                new PublishArtifactInfo(artifactoryTask.mavenDescriptor.getName(), "pom", "pom", null, artifactoryTask.mavenDescriptor);
        Map<String, String> propsToAdd = getPropsToAdd(artifactInfo, null);
        artifactBuilder.addProperties(propsToAdd);
        return new GradleDeployDetails(artifactInfo, artifactBuilder.build(), getProject());
    }

    public GradleDeployDetails gradleDeployDetails(
            PublishArtifact artifact, String configuration) {
        return gradleDeployDetails(artifact, configuration, null, null);
    }

    public GradleDeployDetails gradleDeployDetails(
            PublishArtifact artifact, String configuration, @Nullable String artifactPath) {
        return gradleDeployDetails(artifact, configuration, artifactPath, null);
    }

    public void setIvyDescriptor(Object ivyDescriptor) {
        if (ivyDescriptor != null) {
            if (ivyDescriptor instanceof File) {
                artifactoryTask.ivyDescriptor = (File) ivyDescriptor;
            } else if (ivyDescriptor instanceof CharSequence) {
                if (FileUtils.isAbsolutePath(ivyDescriptor.toString())) {
                    artifactoryTask.ivyDescriptor = new File(ivyDescriptor.toString());
                } else {
                    artifactoryTask.ivyDescriptor = new File(getProject().getProjectDir(), ivyDescriptor.toString());
                }
            } else {
                log.warn("Unknown type '{}' for ivy descriptor in task '{}'",
                        new Object[]{ivyDescriptor.getClass().getName(), getPath()});
            }
        } else {
            artifactoryTask.ivyDescriptor = null;
        }
    }

    public void setMavenDescriptor(Object mavenDescriptor) {
        if (mavenDescriptor != null) {
            if (mavenDescriptor instanceof File) {
                artifactoryTask.mavenDescriptor = (File) mavenDescriptor;
            } else if (mavenDescriptor instanceof CharSequence) {
                if (FileUtils.isAbsolutePath(mavenDescriptor.toString())) {
                    artifactoryTask.mavenDescriptor = new File(mavenDescriptor.toString());
                } else {
                    artifactoryTask.mavenDescriptor = new File(getProject().getProjectDir(), mavenDescriptor.toString());
                }
            } else {
                log.warn("Unknown type '{}' for maven descriptor in task '{}'",
                        new Object[]{mavenDescriptor.getClass().getName(), getPath()});
            }
        } else {
            artifactoryTask.mavenDescriptor = null;
        }
    }

    protected String getModuleName() {
        Project project = getProject();
        //Take into account the archivesBaseName if applied to the project by the Java plugin
        if (project.hasProperty(ARCHIVES_BASE_NAME)) {
            return project.property(ARCHIVES_BASE_NAME).toString();
        }
        return project.getName();
    }

    private GradleDeployDetails gradleDeployDetails(PublishArtifact artifact, String configuration, Set<String> files) {
        return gradleDeployDetails(artifact, configuration, null, files);
    }

    private GradleDeployDetails gradleDeployDetails(PublishArtifact artifact, String configuration,
                                                    @Nullable String artifactPath, @Nullable Set<String> processedFiles) {

        ArtifactoryClientConfiguration.PublisherHandler publisher =
                ArtifactoryPluginUtil.getPublisherHandler(getProject());
        if (publisher == null) {
            return null;
        }

        File file = artifact.getFile();
        if (processedFiles != null && processedFiles.contains(file.getAbsolutePath())) {
            return null;
        }
        if (!file.exists()) {
            throw new GradleException("File '" + file.getAbsolutePath() + "'" +
                    " does not exists, and need to be published!");
        }
        if (processedFiles != null) {
            processedFiles.add(file.getAbsolutePath());
        }

        String revision = getProject().getVersion().toString();
        Map<String, String> extraTokens = Maps.newHashMap();
        if (StringUtils.isNotBlank(artifact.getClassifier())) {
            extraTokens.put("classifier", artifact.getClassifier());
        }
        String pattern = publisher.getIvyArtifactPattern();
        String gid = getProject().getGroup().toString();
        if (publisher.isM2Compatible()) {
            gid = gid.replace(".", "/");
        }

        DeployDetails.Builder deployDetailsBuilder = new DeployDetails.Builder().file(file);
        try {
            Map<String, String> checksums = FileChecksumCalculator.calculateChecksums(file, "MD5", "SHA1");
            deployDetailsBuilder.md5(checksums.get("MD5")).sha1(checksums.get("SHA1"));
        } catch (Exception e) {
            throw new GradleException("Failed to calculate checksums for artifact: " + file.getAbsolutePath(), e);
        }

        if (artifactPath != null) {
            deployDetailsBuilder.artifactPath(artifactPath);
        } else {
            deployDetailsBuilder.artifactPath(IvyPatternHelper.substitute(pattern, gid, getModuleName(),
                    revision, artifact.getName(), artifact.getType(),
                    artifact.getExtension(), configuration,
                    extraTokens, null));
        }
        deployDetailsBuilder.targetRepository(publisher.getRepoKey());
        PublishArtifactInfo artifactInfo = new PublishArtifactInfo(artifact);
        Map<String, String> propsToAdd = getPropsToAdd(artifactInfo, configuration);
        deployDetailsBuilder.addProperties(propsToAdd);
        DeployDetails details = deployDetailsBuilder.build();
        return new GradleDeployDetails(artifactInfo, details, getProject());
    }
}
