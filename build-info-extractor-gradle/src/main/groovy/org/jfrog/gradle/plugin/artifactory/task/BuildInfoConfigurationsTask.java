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

package org.jfrog.gradle.plugin.artifactory.task;

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
import org.gradle.api.tasks.*;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.client.ArtifactoryClientConfiguration;
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.client.LayoutPatterns;
import org.jfrog.gradle.plugin.artifactory.extractor.GradleDeployDetails;
import org.jfrog.gradle.plugin.artifactory.extractor.PublishArtifactInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * @author Fred Simon
 */
public class BuildInfoConfigurationsTask extends BuildInfoBaseTask {

    private static final Logger log = Logging.getLogger(BuildInfoConfigurationsTask.class);

    public static final String PUBLISH_IVY = "publishIvy";
    public static final String PUBLISH_POM = "publishPom";

    @InputFile
    @Optional
    protected File ivyDescriptor;

    @InputFile
    @Optional
    protected File mavenDescriptor;

    @InputFiles
    @Optional
    private Set<Configuration> publishConfigurations = Sets.newHashSet();

    private boolean publishConfigsSpecified;

    @Input
    @Optional
    @Nullable
    public Boolean getPublishIvy() {
        // TODO: Need to take the default from ACC
        return getFlag(PUBLISH_IVY);
    }

    public void setPublishIvy(Object publishIvy) {
        setFlag(PUBLISH_IVY, toBoolean(publishIvy));
    }

    @Input
    @Optional
    @Nullable
    public Boolean getPublishPom() {
        return getFlag(PUBLISH_POM);
    }

    public void setPublishPom(Object publishPom) {
        setFlag(PUBLISH_POM, toBoolean(publishPom));
    }

    public File getIvyDescriptor() {
        return ivyDescriptor;
    }

    public void setIvyDescriptor(Object ivyDescriptor) {
        if (ivyDescriptor != null) {
            if (ivyDescriptor instanceof File) {
                this.ivyDescriptor = (File) ivyDescriptor;
            } else if (ivyDescriptor instanceof CharSequence) {
                if (FileUtils.isAbsolutePath(ivyDescriptor.toString())) {
                    this.ivyDescriptor = new File(ivyDescriptor.toString());
                } else {
                    this.ivyDescriptor = new File(getProject().getProjectDir(), ivyDescriptor.toString());
                }
            } else {
                log.warn("Unknown type '{}' for ivy descriptor in task '{}'",
                        new Object[]{ivyDescriptor.getClass().getName(), getPath()});
            }
        } else {
            this.ivyDescriptor = null;
        }
    }

    public File getMavenDescriptor() {
        return mavenDescriptor;
    }

    public void setMavenDescriptor(Object mavenDescriptor) {
        if (mavenDescriptor != null) {
            if (mavenDescriptor instanceof File) {
                this.mavenDescriptor = (File) mavenDescriptor;
            } else if (mavenDescriptor instanceof CharSequence) {
                if (FileUtils.isAbsolutePath(mavenDescriptor.toString())) {
                    this.mavenDescriptor = new File(mavenDescriptor.toString());
                } else {
                    this.mavenDescriptor = new File(getProject().getProjectDir(), mavenDescriptor.toString());
                }
            } else {
                log.warn("Unknown type '{}' for maven descriptor in task '{}'",
                        new Object[]{mavenDescriptor.getClass().getName(), getPath()});
            }
        } else {
            this.mavenDescriptor = null;
        }
    }

    public void publishConfigs(Object... confs) {
        if (confs == null) {
            return;
        }
        for (Object conf : confs) {
            if (conf instanceof CharSequence) {
                Configuration projectConfig = getProject().getConfigurations().findByName(conf.toString());
                if (projectConfig != null) {
                    publishConfigurations.add(projectConfig);
                } else {
                    log.error("Configuration named '{}' does not exist for project '{}' in task '{}'.",
                            conf.toString(), getProject().getPath(), getPath());
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

    public Set<Configuration> getPublishConfigurations() {
        return publishConfigurations;
    }

    public boolean hasConfigurations() {
        return !publishConfigurations.isEmpty();
    }

    @Nonnull
    protected Boolean isPublishMaven(ArtifactoryClientConfiguration acc) {
        Boolean publishPom = getPublishPom();
        if (publishPom == null) {
            return acc.publisher.isMaven();
        }
        return publishPom;
    }

    @Nonnull
    protected Boolean isPublishIvy(ArtifactoryClientConfiguration acc) {
        Boolean publishIvy = getPublishIvy();
        if (publishIvy == null) {
            return acc.publisher.isIvy();
        }
        return publishIvy;
    }

    protected void collectDescriptorsAndArtifactsForUpload(Set<GradleDeployDetails> allDeployableDetails)
            throws IOException {
        Set<GradleDeployDetails> deployDetailsFromProject = getArtifactDeployDetails();
        allDeployableDetails.addAll(deployDetailsFromProject);

        //Add the ivy and maven descriptors if they exist
        ArtifactoryClientConfiguration acc = getArtifactoryClientConfiguration();
        if (isPublishIvy(acc)) {
            if (ivyDescriptor != null && ivyDescriptor.exists()) {
                allDeployableDetails.add(getIvyDescriptorDeployDetails());
            }
        }
        if (isPublishMaven(acc)) {
            if (mavenDescriptor != null && mavenDescriptor.exists()) {
                allDeployableDetails.add(getMavenDeployDetails());
            }
        }
    }

    @Override
    public boolean hasModules() {
        return hasConfigurations();
    }

    private GradleDeployDetails getIvyDescriptorDeployDetails() {
        ArtifactoryClientConfiguration clientConf = getArtifactoryClientConfiguration();
        DeployDetails.Builder artifactBuilder = new DeployDetails.Builder().file(ivyDescriptor);
        try {
            Map<String, String> checksums =
                    FileChecksumCalculator.calculateChecksums(ivyDescriptor, "MD5", "SHA1");
            artifactBuilder.md5(checksums.get("MD5")).sha1(checksums.get("SHA1"));
        } catch (Exception e) {
            throw new GradleException(
                    "Failed to calculate checksums for artifact: " + ivyDescriptor.getAbsolutePath(), e);
        }
        String gid = getProject().getGroup().toString();
        if (clientConf.publisher.isM2Compatible()) {
            gid = gid.replace(".", "/");
        }
        artifactBuilder.artifactPath(IvyPatternHelper
                .substitute(clientConf.publisher.getIvyPattern(), gid, getModuleName(),
                        getProject().getVersion().toString(), null, "ivy", "xml"));
        artifactBuilder.targetRepository(clientConf.publisher.getRepoKey());
        PublishArtifactInfo artifactInfo =
                new PublishArtifactInfo(ivyDescriptor.getName(), "xml", "ivy", null, ivyDescriptor);
        Map<String, String> propsToAdd = getPropsToAdd(artifactInfo, null);
        artifactBuilder.addProperties(propsToAdd);
        return new GradleDeployDetails(artifactInfo, artifactBuilder.build(), getProject());
    }

    private GradleDeployDetails getMavenDeployDetails() {
        ArtifactoryClientConfiguration clientConf = getArtifactoryClientConfiguration();
        DeployDetails.Builder artifactBuilder = new DeployDetails.Builder().file(mavenDescriptor);
        try {
            Map<String, String> checksums =
                    FileChecksumCalculator.calculateChecksums(mavenDescriptor, "MD5", "SHA1");
            artifactBuilder.md5(checksums.get("MD5")).sha1(checksums.get("SHA1"));
        } catch (Exception e) {
            throw new GradleException(
                    "Failed to calculate checksums for artifact: " + mavenDescriptor.getAbsolutePath(), e);
        }
        // for pom files always enforce the M2 pattern
        artifactBuilder.artifactPath(IvyPatternHelper.substitute(LayoutPatterns.M2_PATTERN,
                getProject().getGroup().toString().replace(".", "/"), getModuleName(),
                getProject().getVersion().toString(), null, "pom", "pom"));
        artifactBuilder.targetRepository(clientConf.publisher.getRepoKey());
        PublishArtifactInfo artifactInfo =
                new PublishArtifactInfo(mavenDescriptor.getName(), "pom", "pom", null, mavenDescriptor);
        Map<String, String> propsToAdd = getPropsToAdd(artifactInfo, null);
        artifactBuilder.addProperties(propsToAdd);
        return new GradleDeployDetails(artifactInfo, artifactBuilder.build(), getProject());
    }

    /**
     * Check all files to publish, depends on it (to generate Gradle task graph to create them).
     *
     * @param project The project of this task
     * @param acc     The convention client configuration
     */
    @Override
    protected void checkDependsOnArtifactsToPublish(Project project, ArtifactoryClientConfiguration acc) {
        // If no configuration no descriptors
        if (!hasConfigurations()) {
            if (publishConfigsSpecified) {
                log.warn("None of the specified publish configurations matched for project '{}' - nothing to publish.",
                        project.getPath());
                return;
            } else {
                Configuration archiveConfig = project.getConfigurations().findByName(Dependency.ARCHIVES_CONFIGURATION);
                if (archiveConfig != null) {
                    log.info("No publish configurations specified for project '{}' - using the default '{}' " +
                            "configuration.", project.getPath(), Dependency.ARCHIVES_CONFIGURATION);
                    publishConfigurations.add(archiveConfig);
                } else {
                    log.warn("No publish configurations specified for project '{}' and the default '{}' " +
                            "configuration does not exist.", project.getPath(), Dependency.ARCHIVES_CONFIGURATION);
                    return;
                }
            }
        }

        // The task depends on the produced artifacts of all configurations "to publish"
        for (Configuration publishConfiguration : publishConfigurations) {
            dependsOn(publishConfiguration.getArtifacts());
        }

        // Set ivy descriptor parameters
        if (isPublishIvy(acc)) {
            if (ivyDescriptor == null) {
                setDefaultIvyDescriptor();
            }
        } else {
            ivyDescriptor = null;
        }

        // Set maven pom parameters
        if (isPublishMaven(acc)) {
            if (mavenDescriptor == null) {
                setDefaultMavenDescriptor();
            }
        } else {
            mavenDescriptor = null;
        }
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
                    ivyDescriptor = uploadTask.getDescriptorDestination();
                    dependsOn(candidateUploadTask);
                }
            }
        }
    }

    protected void setDefaultMavenDescriptor() {
        // Flag to publish the Maven POM, but no pom file inputted, activate default Maven install.
        // if the project doesn't have the maven install task, warn
        Project project = getProject();
        TaskContainer tasks = project.getTasks();
        Upload installTask = tasks.withType(Upload.class).findByName("install");
        if (installTask == null) {
            log.warn("Cannot publish pom for project '{}' since it does not contain the Maven " +
                    "plugin install task and task '{}' does not specify a custom pom path.",
                    new Object[]{project.getPath(), getPath()});
            mavenDescriptor = null;
        } else {
            mavenDescriptor = new File(
                    project.getConvention().getPlugin(MavenPluginConvention.class).getMavenPomDir(),
                    "pom-default.xml");
            dependsOn(installTask);
        }
    }

    protected Set<GradleDeployDetails> getArtifactDeployDetails() {
        ArtifactoryClientConfiguration clientConf = getArtifactoryClientConfiguration();

        Set<GradleDeployDetails> deployDetails = Sets.newLinkedHashSet();
        if (!hasConfigurations()) {
            log.info("No configurations to publish for project '{}'.", getProject().getPath());
            return deployDetails;
        }
        ArtifactoryClientConfiguration.PublisherHandler publisherConf = clientConf.publisher;
        String pattern = publisherConf.getIvyArtifactPattern();
        String gid = getProject().getGroup().toString();
        if (publisherConf.isM2Compatible()) {
            gid = gid.replace(".", "/");
        }

        Set<String> processedFiles = Sets.newHashSet();
        for (Configuration configuration : publishConfigurations) {
            PublishArtifactSet artifacts = configuration.getAllArtifacts();
            for (PublishArtifact artifact : artifacts) {
                File file = artifact.getFile();
                if (processedFiles.contains(file.getAbsolutePath())) {
                    continue;
                }
                if (!file.exists()) {
                    throw new GradleException("File '" + file.getAbsolutePath() + "'" +
                            " does not exists, and need to be published!");
                }
                processedFiles.add(file.getAbsolutePath());
                DeployDetails.Builder artifactBuilder = new DeployDetails.Builder().file(file);
                try {
                    Map<String, String> checksums =
                            FileChecksumCalculator.calculateChecksums(file, "MD5", "SHA1");
                    artifactBuilder.md5(checksums.get("MD5")).sha1(checksums.get("SHA1"));
                } catch (Exception e) {
                    throw new GradleException(
                            "Failed to calculate checksums for artifact: " + file.getAbsolutePath(), e);
                }
                String revision = getProject().getVersion().toString();
                Map<String, String> extraTokens = Maps.newHashMap();
                if (StringUtils.isNotBlank(artifact.getClassifier())) {
                    extraTokens.put("classifier", artifact.getClassifier());
                }
                artifactBuilder.artifactPath(IvyPatternHelper.substitute(pattern, gid, getModuleName(),
                        revision, artifact.getName(), artifact.getType(),
                        artifact.getExtension(), configuration.getName(),
                        extraTokens, null));
                artifactBuilder.targetRepository(publisherConf.getRepoKey());
                PublishArtifactInfo artifactInfo = new PublishArtifactInfo(artifact);
                Map<String, String> propsToAdd = getPropsToAdd(artifactInfo, configuration.getName());
                artifactBuilder.addProperties(propsToAdd);
                DeployDetails details = artifactBuilder.build();
                deployDetails.add(new GradleDeployDetails(artifactInfo, details, getProject()));
            }
        }
        return deployDetails;
    }

}