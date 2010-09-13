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

package org.jfrog.build.extractor.maven;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.build.api.BuildInfoProperties;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.client.ClientProperties;
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.extractor.BuildInfoExtractor;
import org.jfrog.build.extractor.BuildInfoExtractorSpec;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author Noam Y. Tenne
 */
@Component(role = BuildInfoRecorder.class)
public class BuildInfoRecorder extends AbstractExecutionListener implements BuildInfoExtractor<ExecutionEvent, Build> {

    public static final String ACTIVATE_RECORDER = "org.jfrog.build.extractor.maven.recorder.activate";

    @Requirement
    private Logger logger;

    @Requirement
    private BuildInfoModelPropertyResolver buildInfoModelPropertyResolver;

    @Requirement
    private ClientPropertyResolver clientPropertyResolver;

    private ExecutionListener wrappedListener;
    private BuildInfoBuilder buildInfoBuilder;
    private ModuleBuilder currentModule;
    private Set<Artifact> currentModuleArtifacts;
    private Set<Artifact> currentModuleDependencies;
    private Map<org.jfrog.build.api.Artifact, DeployDetails> deployableArtifactBuilderMap;
    private Set<DeployDetails> deployableArtifacts;
    private Properties allProps;
    private Map<String, String> matrixParams;

    public void setListenerToWrap(ExecutionListener executionListener) {
        wrappedListener = executionListener;
    }

    public void setAllProps(Properties allProps) {
        this.allProps = allProps;
    }

    @Override
    public void projectDiscoveryStarted(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.projectDiscoveryStarted(event);
        }
    }

    @Override
    public void sessionStarted(ExecutionEvent event) {
        logger.info("Initializing Artifactory Build-Info Recording");
        buildInfoBuilder = buildInfoModelPropertyResolver.resolveProperties(event, allProps);
        deployableArtifactBuilderMap = Maps.newHashMap();
        deployableArtifacts = Sets.newHashSet();
        matrixParams = Maps.newHashMap();
        Properties matrixParamProps = BuildInfoExtractorUtils.filterDynamicProperties(allProps,
                BuildInfoExtractorUtils.MATRIX_PARAM_PREDICATE);
        for (Map.Entry<Object, Object> matrixParamProp : matrixParamProps.entrySet()) {
            String key = (String) matrixParamProp.getKey();
            key = StringUtils.removeStartIgnoreCase(key, ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX);
            matrixParams.put(key, ((String) matrixParamProp.getValue()));
        }

        if (wrappedListener != null) {
            wrappedListener.sessionStarted(event);
        }
    }

    @Override
    public void sessionEnded(ExecutionEvent event) {
        Build build = extract(event, BuildInfoExtractorSpec.fromProperties());

        if (build != null) {

            List<Module> modules = build.getModules();
            for (Module module : modules) {
                List<org.jfrog.build.api.Artifact> artifacts = module.getArtifacts();
                for (org.jfrog.build.api.Artifact artifact : artifacts) {
                    DeployDetails deployable = deployableArtifactBuilderMap.get(artifact);
                    if (deployable != null) {
                        File file = deployable.getFile();
                        setArtifactChecksums(file, artifact);
                        deployableArtifacts.add(new DeployDetails.Builder().artifactPath(deployable.getArtifactPath()).
                                file(file).md5(artifact.getMd5()).sha1(artifact.getSha1()).
                                addProperties(deployable.getProperties()).
                                targetRepository(deployable.getTargetRepository()).build());
                    }
                }
            }

            String outputFile = allProps.getProperty(BuildInfoProperties.PROP_BUILD_INFO_OUTPUT_FILE);
            logger.debug(
                    "Build Info Recorder: " + BuildInfoProperties.PROP_BUILD_INFO_OUTPUT_FILE + " = " + outputFile);
            if (StringUtils.isNotBlank(outputFile)) {
                try {
                    logger.info("Artifactory Build Info Recorder: Saving build info to " + outputFile);
                    BuildInfoExtractorUtils.saveBuildInfoToFile(build, new File(outputFile));
                } catch (IOException e) {
                    throw new RuntimeException("Error occurred while persisting Build Info to file.", e);
                }
            }

            boolean publishInfo = isPublishBuildInfo();
            boolean publishArtifacts = isPublishArtifacts();
            logger.debug("Build Info Recorder: " + ClientProperties.PROP_PUBLISH_BUILD_INFO + " = " + publishInfo);
            logger.debug("Build Info Recorder: " + ClientProperties.PROP_PUBLISH_ARTIFACT + " = " + publishArtifacts);
            if (publishInfo || publishArtifacts) {
                ArtifactoryBuildInfoClient client = clientPropertyResolver.resolveProperties(allProps);
                try {
                    if (publishArtifacts && (deployableArtifacts != null) && !deployableArtifacts.isEmpty()) {
                        logger.info("Artifactory Build Info Recorder: Deploying artifacts to " +
                                allProps.getProperty(ClientProperties.PROP_CONTEXT_URL));

                        for (DeployDetails artifact : deployableArtifacts) {
                            try {
                                client.deployArtifact(artifact);
                            } catch (IOException e) {
                                throw new RuntimeException("Error occurred while publishing artifact to Artifactory: " +
                                        artifact.getFile() +
                                        ".\n Skipping deployment of remaining artifacts (if any) and build info.", e);
                            }
                        }
                    }

                    if (publishInfo) {
                        try {
                            logger.info("Artifactory Build Info Recorder: Deploying build info ...");
                            client.sendBuildInfo(build);
                        } catch (IOException e) {
                            throw new RuntimeException("Error occurred while publishing Build Info to Artifactory.", e);
                        }
                    }
                } finally {
                    client.shutdown();
                }
            }
        }
        deployableArtifactBuilderMap.clear();
        deployableArtifacts.clear();
        if (wrappedListener != null) {
            wrappedListener.sessionEnded(event);
        }
    }

    @Override
    public void projectSkipped(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.projectSkipped(event);
        }
    }

    @Override
    public void projectStarted(ExecutionEvent event) {
        MavenProject project = event.getProject();
        initModule(project);

        if (wrappedListener != null) {
            wrappedListener.projectStarted(event);
        }
    }

    @Override
    public void projectSucceeded(ExecutionEvent event) {
        finalizeModule(event.getProject());
        if (wrappedListener != null) {
            wrappedListener.projectSucceeded(event);
        }
    }

    @Override
    public void projectFailed(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.projectFailed(event);
        }
    }

    @Override
    public void forkStarted(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.forkStarted(event);
        }
    }

    @Override
    public void forkSucceeded(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.forkSucceeded(event);
        }
    }

    @Override
    public void forkFailed(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.forkFailed(event);
        }
    }

    @Override
    public void forkedProjectStarted(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.forkedProjectStarted(event);
        }
    }

    @Override
    public void forkedProjectSucceeded(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.forkedProjectSucceeded(event);
        }
    }

    @Override
    public void forkedProjectFailed(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.forkedProjectFailed(event);
        }
    }

    @Override
    public void mojoSkipped(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.mojoSkipped(event);
        }
    }

    @Override
    public void mojoStarted(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.mojoStarted(event);
        }
    }

    @Override
    public void mojoSucceeded(ExecutionEvent event) {
        MavenProject project = event.getProject();
        if (project == null) {
            logger.warn("Skipping Artifactory Build-Info dependency extraction: Null project.");
            return;
        }
        extractModuleDependencies(project);

        if (wrappedListener != null) {
            wrappedListener.mojoSucceeded(event);
        }
    }

    @Override
    public void mojoFailed(ExecutionEvent event) {
        MavenProject project = event.getProject();
        if (project == null) {
            logger.warn("Skipping Artifactory Build-Info dependency extraction: Null project.");
            return;
        }
        extractModuleDependencies(project);

        if (wrappedListener != null) {
            wrappedListener.mojoFailed(event);
        }
    }

    private void initModule(MavenProject project) {
        if (project == null) {
            logger.warn("Skipping Artifactory Build-Info module initialization: Null project.");
            return;
        }
        currentModule = new ModuleBuilder();

        currentModule.id(getArtifactIdWithoutType(project.getGroupId(), project.getArtifactId(), project.getVersion()));
        currentModule.properties(project.getProperties());

        currentModuleArtifacts = Sets.newHashSet();
        currentModuleDependencies = Sets.newHashSet();
    }

    private void extractArtifactsAndDependencies(MavenProject project) {
        if (project == null) {
            logger.warn("Skipping Artifactory Build-Info artifact and dependency extraction: Null project.");
            return;
        }
        extractModuleArtifact(project);
        extractModuleAttachedArtifacts(project);
        extractModuleDependencies(project);
    }

    private void finalizeModule(MavenProject project) {
        extractArtifactsAndDependencies(project);
        finalizeAndAddModule(project);
    }

    private void extractModuleArtifact(MavenProject project) {
        Artifact artifact = project.getArtifact();
        if (artifact == null) {
            logger.warn("Skipping Artifactory Build-Info project artifact extraction: Null artifact.");
            return;
        }
        currentModuleArtifacts.add(artifact);
    }

    private void extractModuleAttachedArtifacts(MavenProject project) {
        List<Artifact> artifacts = project.getAttachedArtifacts();
        if (artifacts != null) {
            for (Artifact artifact : artifacts) {
                currentModuleArtifacts.add(artifact);
            }
        }
    }

    private void extractModuleDependencies(MavenProject project) {
        Set<Artifact> dependencies = project.getArtifacts();
        if (dependencies != null) {
            for (Artifact dependency : dependencies) {
                currentModuleDependencies.add(dependency);
            }
        }
    }

    private void finalizeAndAddModule(MavenProject project) {
        addFilesToCurrentModule(project);

        currentModule = null;

        currentModuleArtifacts.clear();
        currentModuleDependencies.clear();
    }

    private void addFilesToCurrentModule(MavenProject project) {
        if (currentModule == null) {
            logger.warn("Skipping Artifactory Build-Info module finalization: Null current module.");
            return;
        }
        addArtifactsToCurrentModule(project);
        addDependenciesToCurrentModule();

        buildInfoBuilder.addModule(currentModule.build());
    }

    private void addArtifactsToCurrentModule(MavenProject project) {
        if (currentModuleArtifacts == null) {
            logger.warn("Skipping Artifactory Build-Info module artifact addition: Null current module artifact list.");
            return;
        }

        for (Artifact moduleArtifact : currentModuleArtifacts) {
            String artifactId = moduleArtifact.getArtifactId();
            String artifactVersion = moduleArtifact.getVersion();
            String artifactClassifier = moduleArtifact.getClassifier();
            String type = moduleArtifact.getType();
            String artifactExtension = moduleArtifact.getArtifactHandler().getExtension();

            String artifactName = getArtifactName(artifactId, artifactVersion, artifactClassifier, artifactExtension);

            ArtifactBuilder artifactBuilder = new ArtifactBuilder(artifactName).type(type);
            File artifactFile = moduleArtifact.getFile();
            if ((artifactFile == null) && moduleArtifact.equals(project.getArtifact())) {
                artifactFile = project.getFile();
            }
            org.jfrog.build.api.Artifact artifact = artifactBuilder.build();
            currentModule.addArtifact(artifact);
            if (artifactFile != null && artifactFile.isFile() && isPublishArtifacts()) {
                addDeployableArtifact(artifact, artifactFile, moduleArtifact.getGroupId(),
                        artifactId, artifactVersion, artifactClassifier, artifactExtension);
            }

            if (!isPomProject(moduleArtifact)) {
                for (ArtifactMetadata metadata : moduleArtifact.getMetadataList()) {
                    if (metadata instanceof ProjectArtifactMetadata) {
                        Model model = project.getModel();
                        File pomFile = null;
                        if (model != null) {
                            pomFile = model.getPomFile();
                        }
                        artifactBuilder.type("pom");
                        artifactBuilder.name(artifactName.replace(artifactExtension, "pom"));
                        org.jfrog.build.api.Artifact pomArtifact = artifactBuilder.build();
                        currentModule.addArtifact(pomArtifact);
                        if (pomFile != null && pomFile.isFile() && isPublishArtifacts()) {
                            addDeployableArtifact(pomArtifact, pomFile, moduleArtifact.getGroupId(),
                                    artifactId, artifactVersion,
                                    artifactClassifier, "pom");
                        }
                    }
                }
            }
        }
    }

    private String getArtifactName(String artifactId, String version, String classifier, String fileExtension) {
        StringBuilder nameBuilder = new StringBuilder(artifactId).append("-").append(version);
        if (StringUtils.isNotBlank(classifier)) {
            nameBuilder.append("-").append(classifier);
        }

        return nameBuilder.append(".").append(fileExtension).toString();
    }

    private void addDeployableArtifact(org.jfrog.build.api.Artifact artifact, File artifactFile,
            String groupId, String artifactId, String version, String classifier, String fileExtension) {
        String deploymentPath = getDeploymentPath(groupId, artifactId, version, classifier, fileExtension);
        // deploy to snapshots or releases repository based on the deploy version
        String targetRepository = getTargetRepository(deploymentPath);

        DeployDetails deployable = new DeployDetails.Builder().artifactPath(deploymentPath).file(artifactFile).
                targetRepository(targetRepository).addProperties(matrixParams).build();

        deployableArtifactBuilderMap.put(artifact, deployable);
    }

    /**
     * @return Return the target deployment repository. Either the releases repository (default) or snapshots if
     *         defined and the deployed file is a snapshot.
     */
    public String getTargetRepository(String version) {
        String snapshotsRepository = allProps.getProperty(ClientProperties.PROP_PUBLISH_SNAPSHOTS_REPOKEY);
        if (snapshotsRepository != null && version.contains("-SNAPSHOT")) {
            return snapshotsRepository;
        }
        String releasesRepository = allProps.getProperty(ClientProperties.PROP_PUBLISH_REPOKEY);
        return releasesRepository;
    }

    private String getDeploymentPath(String groupId, String artifactId, String version, String classifier,
            String fileExtension) {
        return new StringBuilder(groupId.replace(".", "/")).append("/").append(artifactId).append("/").append(version).
                append("/").append(getArtifactName(artifactId, version, classifier, fileExtension)).toString();
    }

    private void addDependenciesToCurrentModule() {
        if (currentModuleDependencies == null) {
            logger.warn("Skipping Artifactory Build-Info module dependency addition: Null current module dependency " +
                    "list.");
            return;
        }
        for (Artifact dependency : currentModuleDependencies) {
            DependencyBuilder dependencyBuilder = new DependencyBuilder()
                    .id(getArtifactIdWithoutType(dependency.getGroupId(), dependency.getArtifactId(),
                            dependency.getVersion()))
                    .type(dependency.getType())
                    .scopes(Lists.newArrayList(dependency.getScope()));
            setDependencyChecksums(dependency.getFile(), dependencyBuilder);
            currentModule.addDependency(dependencyBuilder.build());
        }
    }

    private boolean isPomProject(Artifact moduleArtifact) {
        return "pom".equals(moduleArtifact.getType());
    }

    private void setArtifactChecksums(File artifactFile, org.jfrog.build.api.Artifact artifact) {
        if ((artifactFile != null) && (artifactFile.isFile())) {
            try {
                Map<String, String> checksums = FileChecksumCalculator.calculateChecksums(artifactFile, "md5", "sha1");
                artifact.setMd5(checksums.get("md5"));
                artifact.setSha1(checksums.get("sha1"));
            } catch (Exception e) {
                logger.error("Could not set checksum values on '" + artifact.getName() + "': " + e.getMessage(), e);
            }
        }
    }

    private void setDependencyChecksums(File dependencyFile, DependencyBuilder dependencyBuilder) {
        if ((dependencyFile != null) && (dependencyFile.isFile())) {
            try {
                Map<String, String> checksumsMap =
                        FileChecksumCalculator.calculateChecksums(dependencyFile, "md5", "sha1");
                dependencyBuilder.md5(checksumsMap.get("md5"));
                dependencyBuilder.sha1(checksumsMap.get("sha1"));
            } catch (Exception e) {
                logger.error("Could not set checksum values on '" + dependencyBuilder.build().getId() + "': " +
                        e.getMessage(), e);
            }
        }
    }

    public Build extract(ExecutionEvent event, BuildInfoExtractorSpec spec) {
        MavenSession session = event.getSession();

        if (!session.getResult().hasExceptions()) {

            if (Boolean.valueOf(allProps.getProperty(BuildInfoConfigProperties.PROP_INCLUDE_ENV_VARS))) {
                Properties envProperties = BuildInfoExtractorUtils.getEnvProperties(allProps);
                for (Map.Entry<Object, Object> envProp : envProperties.entrySet()) {
                    buildInfoBuilder.addProperty(envProp.getKey(), envProp.getValue());
                }

            }

            Date finish = new Date();
            long time = finish.getTime() - session.getRequest().getStartTime().getTime();

            return buildInfoBuilder.durationMillis(time).build();
        }

        return null;
    }

    private boolean isPublishArtifacts() {
        return Boolean.valueOf(allProps.getProperty(ClientProperties.PROP_PUBLISH_ARTIFACT));
    }

    private boolean isPublishBuildInfo() {
        return Boolean.valueOf(allProps.getProperty(ClientProperties.PROP_PUBLISH_BUILD_INFO));
    }

    private String getArtifactIdWithoutType(String groupId, String artifactId, String version) {
        return new StringBuilder(groupId).append(":").append(artifactId).
                append(":").append(version).toString();
    }
}
