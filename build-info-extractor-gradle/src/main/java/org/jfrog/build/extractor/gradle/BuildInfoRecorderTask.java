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

import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.jfrog.build.ArtifactoryPluginUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.client.*;
import org.jfrog.build.extractor.BuildInfoExtractorSpec;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.logger.GradleClientLogger;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import static org.jfrog.build.ArtifactoryPluginUtils.BUILD_INFO_TASK_NAME;
import static org.jfrog.build.ArtifactoryPluginUtils.getProperty;
import static org.jfrog.build.api.BuildInfoConfigProperties.PROP_EXPORT_FILE_PATH;
import static org.jfrog.build.client.ClientProperties.*;

/**
 * @author Tomer Cohen
 */
public class BuildInfoRecorderTask extends ConventionTask {
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

    /**
     * @return The root project.
     */
    public Project getRootProject() {
        return getProject().getRootProject();
    }

    @TaskAction
    public void collectProjectBuildInfo() throws IOException {
        log.debug("Starting extraction for project {}", getProject());
        Project rootProject = getRootProject();
        if (getProject().equals(rootProject)) {
            GradleBuildInfoExtractor infoExtractor = new GradleBuildInfoExtractor(rootProject);
            closeAndDeploy(infoExtractor);
        }
    }

    private void uploadDescriptorsAndArtifacts(Project project, Set<DeployDetails> allDeployableDetails) throws IOException {
        Set<DeployDetails> deployDetailsFromProject = ArtifactoryPluginUtils.getDeployArtifactsProject(project);
        allDeployableDetails.addAll(deployDetailsFromProject);
        if (ivyDescriptor != null && ivyDescriptor.exists()) {
            Set<DeployDetails> details =
                    ArtifactoryPluginUtils.getIvyDescriptorDeployDetails(project, ivyDescriptor);
            allDeployableDetails.addAll(details);
        }

        if (mavenDescriptor != null && mavenDescriptor.exists()) {
            allDeployableDetails.add(ArtifactoryPluginUtils.getMavenDeployDetails(project, mavenDescriptor));
        }
    }

    /**
     * This method will be activated only at the end of the build, when we reached the root project.
     *
     * @param gbie The Build info extractor object will will assemble the {@link Build} object
     * @throws java.io.IOException In case the deployment fails.
     */
    private void closeAndDeploy(GradleBuildInfoExtractor gbie) throws IOException {
        Project rootProject = getRootProject();
        String uploadArtifactsProperty = getProperty(ClientProperties.PROP_PUBLISH_ARTIFACT, rootProject);
        String fileExportPath = getProperty(PROP_EXPORT_FILE_PATH, rootProject);
        String contextUrl = getProperty(ClientProperties.PROP_CONTEXT_URL, rootProject);
        log.debug("Context URL for deployment '{}", contextUrl);
        String username = getProperty(ClientProperties.PROP_PUBLISH_USERNAME, rootProject);
        String password = getProperty(ClientProperties.PROP_PUBLISH_PASSWORD, rootProject);
        if (StringUtils.isBlank(username)) {
            username = "";
        }
        if (StringUtils.isBlank(password)) {
            password = "";
        }
        ArtifactoryBuildInfoClient client =
                new ArtifactoryBuildInfoClient(contextUrl, username, password, new GradleClientLogger(log));
        try {
            if (Boolean.parseBoolean(uploadArtifactsProperty)) {
                log.debug("Uploading artifacts to Artifactory at '{}'", contextUrl);
                /**
                 * if the {@link org.jfrog.build.client.ClientProperties#PROP_PUBLISH_ARTIFACT} is set the true,
                 * The uploadArchives task will be triggered ONLY at the end, ensuring that the artifacts will be published
                 * only after a successful build. This is done before the build-info is sent.
                 */
                Set<DeployDetails> allDeployableDetails = Sets.newHashSet();
                for (Project uploadingProject : rootProject.getAllprojects()) {
                    BuildInfoRecorderTask upTasksBuildInfo =
                            (BuildInfoRecorderTask) uploadingProject.getTasks().findByName(BUILD_INFO_TASK_NAME);
                    if (upTasksBuildInfo != null) {
                        upTasksBuildInfo.uploadDescriptorsAndArtifacts(uploadingProject, allDeployableDetails);
                    }
                }
                IncludeExcludePatterns patterns = new IncludeExcludePatterns(
                        ArtifactoryPluginUtils
                                .getProperty(ClientProperties.PROP_PUBLISH_ARTIFACT_INCLUDE_PATTERNS, rootProject),
                        ArtifactoryPluginUtils
                                .getProperty(ClientProperties.PROP_PUBLISH_ARTIFACT_EXCLUDE_PATTERNS, rootProject));
                configureProxy(rootProject, client);
                deployArtifacts(client, allDeployableDetails, patterns);
            }

            String publishBuildInfo = getProperty(ClientProperties.PROP_PUBLISH_BUILD_INFO, rootProject);
            boolean isPublishBuildInfo = Boolean.parseBoolean(publishBuildInfo);
            if (isPublishBuildInfo) {
                log.debug("Publishing build info to artifactory at: '{}'", contextUrl);
                /**
                 * After all the artifacts were uploaded successfully the next task is to send the build-info
                 * object.
                 */
                Build build = gbie.extract(this, new BuildInfoExtractorSpec());
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
                Build build = gbie.extract(this, new BuildInfoExtractorSpec());
                exportBuildInfo(build, savedFile);
            }
        } finally {
            client.shutdown();
        }
    }


    private void configureProxy(Project project, ArtifactoryBuildInfoClient client) {
        String proxyHost = ArtifactoryPluginUtils.getProperty(PROP_PROXY_HOST, project);
        if (StringUtils.isNotBlank(proxyHost)) {
            log.debug("Found proxy host '{}'", proxyHost);
            String proxyPort = ArtifactoryPluginUtils.getProperty(PROP_PROXY_PORT, project);
            if (!StringUtils.isNumeric(proxyPort)) {
                log.debug("Proxy port is not of numeric value '{}'", proxyPort);
                return;
            }
            String proxyUserName = ArtifactoryPluginUtils.getProperty(PROP_PROXY_USERNAME, project);
            if (StringUtils.isNotBlank(proxyUserName)) {
                log.debug("Found proxy user name '{}'", proxyUserName);
                String proxyPassword = ArtifactoryPluginUtils.getProperty(PROP_PROXY_PASSWORD, project);
                log.debug("Using proxy password '{}'", proxyPassword);
                client.setProxyConfiguration(proxyHost, Integer.parseInt(proxyPort), proxyUserName, proxyPassword);
            } else {
                log.debug("No proxy user name and password found, using anonymous proxy");
                client.setProxyConfiguration(proxyHost, Integer.parseInt(proxyPort));
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
}