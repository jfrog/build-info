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
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.Upload;
import org.jfrog.build.ArtifactoryPluginUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.client.ClientGradleProperties;
import org.jfrog.build.client.ClientIvyProperties;
import org.jfrog.build.client.ClientProperties;
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.client.IncludeExcludePatterns;
import org.jfrog.build.client.PatternMatcher;
import org.jfrog.build.extractor.BuildInfoExtractorSpec;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.logger.GradleClientLogger;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import static org.jfrog.build.ArtifactoryPluginUtils.getProperty;
import static org.jfrog.build.api.BuildInfoConfigProperties.PROP_EXPORT_FILE_PATH;

/**
 * @author Tomer Cohen
 */
public class BuildInfoRecorderTask extends ConventionTask {
    private static final Logger log = Logging.getLogger(BuildInfoRecorderTask.class);

    private Configuration configuration;

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
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

    /**
     * Returns the artifacts which will be uploaded.
     *
     * @param gbie Tomer will document later.
     * @throws java.io.IOException Tomer will document later.
     */

    private void closeAndDeploy(GradleBuildInfoExtractor gbie) throws IOException {
        Project rootProject = getRootProject();
        String uploadArtifactsProperty = getProperty(ClientProperties.PROP_PUBLISH_ARTIFACT, rootProject);
        String fileExportPath = getProperty(PROP_EXPORT_FILE_PATH, rootProject);
        String contextUrl = getProperty(ClientProperties.PROP_CONTEXT_URL, rootProject);
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
                /**
                 * if the {@link org.jfrog.build.client.ClientProperties#PROP_PUBLISH_ARTIFACT} is set the true,
                 * The uploadArchives task will be triggered ONLY at the end, ensuring that the artifacts will be published
                 * only after a successful build. This is done before the build-info is sent.
                 */
                Set<DeployDetails> allDeployableDetails = Sets.newHashSet();
                for (Project uploadingProject : rootProject.getAllprojects()) {
                    Set<DeployDetails> deployDetailsFromProject =
                            ArtifactoryPluginUtils.getDeployArtifactsProject(uploadingProject);
                    allDeployableDetails.addAll(deployDetailsFromProject);
                    String deployIvy = getProperty(ClientIvyProperties.PROP_PUBLISH_IVY, uploadingProject);
                    if (Boolean.parseBoolean(deployIvy)) {
                        File ivyFile = new File(uploadingProject.getBuildDir(), "ivy.xml");
                        if (!ivyFile.exists()) {
                            Set<Task> installTask = uploadingProject.getTasksByName("uploadArchives", false);
                            for (Task task : installTask) {
                                ((Upload) task).execute();
                            }
                        }
                        Set<DeployDetails> details =
                                ArtifactoryPluginUtils.getIvyDescriptorDeployDetails(uploadingProject);
                        allDeployableDetails.addAll(details);
                    }
                    String deployMaven = getProperty(ClientGradleProperties.PROP_PUBLISH_MAVEN, uploadingProject);
                    if (Boolean.parseBoolean(deployMaven)) {
                        File mavenPom =
                                new File(uploadingProject.getRepositories().getMavenPomDir(), "pom-default.xml");
                        if (!mavenPom.exists()) {
                            Set<Task> installTask = uploadingProject.getTasksByName("install", false);
                            if (installTask == null ||
                                    installTask.isEmpty() &&
                                            !uploadingProject.equals(uploadingProject.getRootProject())) {
                                throw new GradleException("Maven plugin is not configured");
                            }
                            for (Task task : installTask) {
                                ((Upload) task).execute();
                            }
                        }
                        if (mavenPom.exists()) {
                            allDeployableDetails.add(ArtifactoryPluginUtils.getMavenDeployDetails(uploadingProject));
                        }
                    }
                }

                IncludeExcludePatterns patterns = new IncludeExcludePatterns(
                        ArtifactoryPluginUtils
                                .getProperty(ClientProperties.PROP_PUBLISH_ARTIFACT_INCLUDE_PATTERNS, rootProject),
                        ArtifactoryPluginUtils
                                .getProperty(ClientProperties.PROP_PUBLISH_ARTIFACT_EXCLUDE_PATTERNS, rootProject));
                deployArtifacts(client, allDeployableDetails, patterns);
            }
            String publishBuildInfo = getProperty(ClientProperties.PROP_PUBLISH_BUILD_INFO, rootProject);
            boolean isPublishBuildInfo = Boolean.parseBoolean(publishBuildInfo);
            if (isPublishBuildInfo) {
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

    private void deployArtifacts(ArtifactoryBuildInfoClient client, Set<DeployDetails> details,
            IncludeExcludePatterns patterns) throws IOException {
        for (DeployDetails detail : details) {
            String artifactPath = detail.getArtifactPath();
            if (PatternMatcher.pathConflicts(artifactPath, patterns)) {
                log.info("Skipping the deployment of '" + artifactPath +
                        "' due to the defined include-exclude patterns.");
                continue;
            }
            client.deployArtifact(detail);
        }
    }

    private void exportBuildInfo(Build build, File toFile) throws IOException {
        BuildInfoExtractorUtils.saveBuildInfoToFile(build, toFile);
    }
}
