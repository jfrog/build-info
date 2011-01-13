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

package org.jfrog.build.extractor.gradle;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.ivy.core.IvyPatternHelper;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.Upload;
import org.jfrog.build.ArtifactoryPluginUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.client.ArtifactoryClientConfiguration;
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.client.IncludeExcludePatterns;
import org.jfrog.build.client.LayoutPatterns;
import org.jfrog.build.client.PatternMatcher;
import org.jfrog.build.extractor.BuildInfoExtractorSpec;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.logger.GradleClientLogger;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.jfrog.build.ArtifactoryPluginUtils.BUILD_INFO_TASK_NAME;

/**
 * @author Tomer Cohen
 */
public class BuildInfoRecorderTask extends DefaultTask {
    private static final Logger log = Logging.getLogger(BuildInfoRecorderTask.class);

    @InputFile
    @Optional
    private File ivyDescriptor;

    @InputFile
    @Optional
    private File mavenDescriptor;

    @InputFiles
    @Optional
    private Configuration configuration;

    @Input
    @Optional
    private String artifactName;

    private boolean lastInGraph = false;

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public File getIvyDescriptor() {
        return ivyDescriptor;
    }

    public void setIvyDescriptor(File ivyDescriptor) {
        this.ivyDescriptor = ivyDescriptor;
    }

    public File getMavenDescriptor() {
        return mavenDescriptor;
    }

    public void setMavenDescriptor(File mavenDescriptor) {
        this.mavenDescriptor = mavenDescriptor;
    }

    public String getArtifactName() {
        if (artifactName == null) {
            return getProject().getName();
        }
        return artifactName;
    }

    public void setArtifactName(String artifactName) {
        this.artifactName = artifactName;
    }

    public void projectsEvaluated() {
        Project project = getProject();
        TaskContainer tasks = project.getTasks();
        if (configuration == null) {
            configuration = project.getConfigurations().findByName(Dependency.ARCHIVES_CONFIGURATION);
        }
        for (Project sub : project.getSubprojects()) {
            Task subBiTask = sub.getTasks().findByName(BUILD_INFO_TASK_NAME);
            if (subBiTask != null) {
                dependsOn(subBiTask);
            }
        }

        // If no configuration no descriptor
        if (configuration == null) {
            return;
        }
        ArtifactoryClientConfiguration acc = getArtifactoryClientConfiguration();

        // Set ivy descriptor parameters
        if (acc.publisher.isIvy()) {
            if (ivyDescriptor == null) {
                // Flag to publish the Ivy XML file, but no ivy descriptor file inputted, activate default upload${configuration}.
                Upload uploadTask = (Upload) tasks.getByName(configuration.getUploadTaskName());
                if (!uploadTask.isUploadDescriptor()) {
                    throw new GradleException(
                            "Cannot publish Ivy descriptor if ivyDescriptor not set in task: " + getPath() +
                                    "\nAnd flag uploadDescriptor not set in default task: " + uploadTask.getPath());
                }
                ivyDescriptor = uploadTask.getDescriptorDestination();
                dependsOn(uploadTask);
            }
        } else {
            ivyDescriptor = null;
        }

        // Set maven pom parameters
        if (acc.publisher.isMaven()) {
            if (mavenDescriptor == null) {
                // Flag to publish the Maven POM, but no pom file inputted, activate default Maven install.
                // if the project doesn't have the maven install task, throw an exception
                Upload installTask = tasks.withType(Upload.class).findByName("install");
                if (installTask == null) {
                    throw new GradleException(
                            "Cannot publish Maven descriptor if mavenDescriptor not set in task: " + getPath() +
                                    "\nAnd default install task for project " + project.getPath() +
                                    " is not an Upload task");
                }
                mavenDescriptor = new File(project.getRepositories().getMavenPomDir(), "pom-default.xml");
                dependsOn(installTask);
            }
        } else {
            mavenDescriptor = null;
        }
    }

    private ArtifactoryClientConfiguration getArtifactoryClientConfiguration() {
        BuildInfoConfigTask bict = (BuildInfoConfigTask) getProject().getRootProject().getTasks()
                .getByName(ArtifactoryPluginUtils.BUILD_INFO_CONFIG_TASK_NAME);
        return bict.acc;
    }

    @TaskAction
    public void collectProjectBuildInfo() throws IOException {
        log.debug("BuildInfo task for project {} activated", getProject().getPath());
        // Only the last buildInfo execution activate the deployment
        if (lastInGraph) {
            log.debug("Starting build info extraction for last project {}", getProject().getPath());
            closeAndDeploy();
        }
    }

    private void uploadDescriptorsAndArtifacts(Set<DeployDetails> allDeployableDetails) throws IOException {
        Set<DeployDetails> deployDetailsFromProject = getDeployArtifactsProject();
        allDeployableDetails.addAll(deployDetailsFromProject);
        if (ivyDescriptor != null && ivyDescriptor.exists()) {
            allDeployableDetails.add(getIvyDescriptorDeployDetails());
        }

        if (mavenDescriptor != null && mavenDescriptor.exists()) {
            allDeployableDetails.add(getMavenDeployDetails());
        }
    }

    private DeployDetails getIvyDescriptorDeployDetails() {
        ArtifactoryClientConfiguration clientConf = getArtifactoryClientConfiguration();
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
        String gid = getProject().getGroup().toString();
        if (clientConf.publisher.isM2Compatible()) {
            gid = gid.replace(".", "/");
        }
        artifactBuilder.artifactPath(IvyPatternHelper
                .substitute(clientConf.publisher.getIvyPattern(), gid, getArtifactName(),
                        getProject().getVersion().toString(),
                        null, "ivy", "xml"));
        artifactBuilder.targetRepository(clientConf.publisher.getRepoKey());
        artifactBuilder.addProperties(clientConf.publisher.getMatrixParams());
        return artifactBuilder.build();
    }

    private DeployDetails getMavenDeployDetails() {
        ArtifactoryClientConfiguration clientConf = getArtifactoryClientConfiguration();
        DeployDetails.Builder artifactBuilder = new DeployDetails.Builder().file(mavenDescriptor);
        try {
            Map<String, String> checksums =
                    FileChecksumCalculator.calculateChecksums(mavenDescriptor, "MD5", "SHA1");
            artifactBuilder.md5(checksums.get("MD5")).sha1(checksums.get("SHA1"));
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to calculated checksums for artifact: " + mavenDescriptor.getAbsolutePath(), e);
        }
        // for pom files always enforce the M2 pattern
        artifactBuilder.artifactPath(IvyPatternHelper.substitute(LayoutPatterns.M2_PATTERN,
                getProject().getGroup().toString().replace(".", "/"), getArtifactName(),
                getProject().getVersion().toString(), null, "pom", "pom"));
        artifactBuilder.targetRepository(clientConf.publisher.getRepoKey());
        artifactBuilder.addProperties(clientConf.publisher.getMatrixParams());
        return artifactBuilder.build();
    }

    /**
     * This method will be activated only at the end of the build, when we reached the root project.
     *
     * @throws java.io.IOException In case the deployment fails.
     */
    private void closeAndDeploy() throws IOException {
        ArtifactoryClientConfiguration clientConf = getArtifactoryClientConfiguration();
        GradleBuildInfoExtractor gbie = new GradleBuildInfoExtractor(clientConf);
        String fileExportPath = clientConf.getExportFile();
        String contextUrl = clientConf.getContextUrl();
        log.debug("Context URL for deployment '{}", contextUrl);
        String username = clientConf.publisher.getUserName();
        String password = clientConf.publisher.getPassword();
        if (StringUtils.isBlank(username)) {
            username = "";
        }
        if (StringUtils.isBlank(password)) {
            password = "";
        }
        ArtifactoryBuildInfoClient client =
                new ArtifactoryBuildInfoClient(contextUrl, username, password, new GradleClientLogger(log));
        Set<DeployDetails> allDeployableDetails = Sets.newHashSet();
        for (Task birt : getProject().getTasksByName(BUILD_INFO_TASK_NAME, true)) {
            ((BuildInfoRecorderTask) birt).uploadDescriptorsAndArtifacts(allDeployableDetails);
        }
        try {
            if (clientConf.publisher.isPublishArtifacts()) {
                log.debug("Uploading artifacts to Artifactory at '{}'", contextUrl);
                /**
                 * if the {@link org.jfrog.build.client.ClientProperties#PROP_PUBLISH_ARTIFACT} is set the true,
                 * The uploadArchives task will be triggered ONLY at the end, ensuring that the artifacts will be published
                 * only after a successful build. This is done before the build-info is sent.
                 */

                IncludeExcludePatterns patterns = new IncludeExcludePatterns(
                        clientConf.publisher.getIncludePatterns(),
                        clientConf.publisher.getExcludePatterns());
                configureProxy(clientConf, client);
                deployArtifacts(client, allDeployableDetails, patterns);
            }

            Build build = gbie.extract(getProject().getRootProject(), new BuildInfoExtractorSpec());
            if (clientConf.publisher.isPublishBuildInfo()) {
                log.debug("Publishing build info to artifactory at: '{}'", contextUrl);
                /**
                 * After all the artifacts were uploaded successfully the next task is to send the build-info
                 * object.
                 */
                if (fileExportPath != null) {
                    // If export property set always save the file before sending it to artifactory
                    exportBuildInfo(build, new File(fileExportPath));
                }
                client.sendBuildInfo(build);
            } else {
                /**
                 * If we do not deploy any artifacts or build-info, the build-info will be written to a file in its
                 * JSON form.
                 */
                File savedFile;
                if (fileExportPath == null) {
                    savedFile = new File(getProject().getBuildDir(), "build-info.json");
                } else {
                    savedFile = new File(fileExportPath);
                }
                exportBuildInfo(build, savedFile);
            }
        } finally {
            client.shutdown();
        }
    }


    private void configureProxy(ArtifactoryClientConfiguration clientConf, ArtifactoryBuildInfoClient client) {
        ArtifactoryClientConfiguration.ProxyHandler proxy = clientConf.proxy;
        String proxyHost = proxy.getHost();
        if (StringUtils.isNotBlank(proxyHost) && proxy.getPort() != null) {
            log.debug("Found proxy host '{}'", proxyHost);
            String proxyUserName = proxy.getUserName();
            if (StringUtils.isNotBlank(proxyUserName)) {
                log.debug("Found proxy user name '{}'", proxyUserName);
                client.setProxyConfiguration(proxyHost, proxy.getPort(), proxyUserName, proxy.getPassword());
            } else {
                log.debug("No proxy user name and password found, using anonymous proxy");
                client.setProxyConfiguration(proxyHost, proxy.getPort());
            }
        }
    }

    private void deployArtifacts(ArtifactoryBuildInfoClient client, Set<DeployDetails> details,
            IncludeExcludePatterns patterns) throws IOException {
        for (DeployDetails detail : details) {
            String artifactPath = detail.getArtifactPath();
            if (PatternMatcher.pathConflicts(artifactPath, patterns)) {
                log.log(LogLevel.LIFECYCLE, "Skipping the deployment of '" + artifactPath +
                        "' due to the defined include-exclude patterns.");
                continue;
            }
            client.deployArtifact(detail);
        }
    }

    private void exportBuildInfo(Build build, File toFile) throws IOException {
        log.debug("Exporting generated build info to '{}'", toFile.getAbsolutePath());
        BuildInfoExtractorUtils.saveBuildInfoToFile(build, toFile);
    }

    private Set<DeployDetails> getDeployArtifactsProject() {
        ArtifactoryClientConfiguration clientConf = getArtifactoryClientConfiguration();
        Set<DeployDetails> deployDetails = Sets.newLinkedHashSet();
        if (configuration == null) {
            return deployDetails;
        }
        ArtifactoryClientConfiguration.PublisherHandler publisherConf = clientConf.publisher;
        String pattern = publisherConf.getIvyArtifactPattern();
        Map<String, String> matrixParams = publisherConf.getMatrixParams();
        String gid = getProject().getGroup().toString();
        if (publisherConf.isM2Compatible()) {
            gid = gid.replace(".", "/");
        }
        Set<PublishArtifact> artifacts = configuration.getArtifacts();
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
            String revision = getProject().getVersion().toString();
            Map<String, String> extraTokens = Maps.newHashMap();
            if (StringUtils.isNotBlank(publishArtifact.getClassifier())) {
                extraTokens.put("classifier", publishArtifact.getClassifier());
            }
            artifactBuilder.artifactPath(
                    IvyPatternHelper.substitute(pattern, gid,
                            getArtifactName(),
                            revision, null, publishArtifact.getType(),
                            publishArtifact.getExtension(), configuration.getName(),
                            extraTokens, null));
            artifactBuilder.targetRepository(publisherConf.getRepoKey());
            artifactBuilder.addProperties(matrixParams);
            DeployDetails details = artifactBuilder.build();
            deployDetails.add(details);
        }
        return deployDetails;
    }
}