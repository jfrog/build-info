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

package org.jfrog.build.extractor.maven;

import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.client.ArtifactoryClientConfiguration;
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.client.IncludeExcludePatterns;
import org.jfrog.build.client.PatternMatcher;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Noam Y. Tenne
 */
@Component(role = BuildDeploymentHelper.class)
public class BuildDeploymentHelper {

    @Requirement
    private Logger logger;

    @Requirement
    private BuildInfoClientBuilder buildInfoClientBuilder;

    public void deploy(Build build, ArtifactoryClientConfiguration clientConf,
            Map<String, DeployDetails> deployableArtifactBuilders, boolean wereThereTestFailures, File basedir) {
        Set<DeployDetails> deployableArtifacts = prepareDeployableArtifacts(build, deployableArtifactBuilders);
        String outputFile = clientConf.getExportFile();
        logger.debug("Build Info Recorder: " + BuildInfoConfigProperties.EXPORT_FILE + " = " + outputFile);
        try {
            if (StringUtils.isNotBlank(outputFile)) {
                logger.info("Artifactory Build Info Recorder: Saving build info to " + outputFile);
                BuildInfoExtractorUtils.saveBuildInfoToFile(build, new File(outputFile));
            } else {
                File buildInfo = new File(basedir, "target/build-info.json");
                BuildInfoExtractorUtils.saveBuildInfoToFile(build, buildInfo);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error occurred while persisting Build Info to file.", e);
        }
        logger.debug("Build Info Recorder: " + clientConf.publisher.isPublishBuildInfo() + " = " +
                clientConf.publisher.isPublishBuildInfo());
        logger.debug("Build Info Recorder: " + clientConf.publisher.isPublishArtifacts() + " = " + clientConf);
        if (clientConf.publisher.isPublishBuildInfo() || clientConf.publisher.isPublishArtifacts()) {
            ArtifactoryBuildInfoClient client = buildInfoClientBuilder.resolveProperties(clientConf);
            try {
                if (clientConf.publisher.isPublishArtifacts() && (deployableArtifacts != null) &&
                        !deployableArtifacts.isEmpty()) {
                    deployArtifacts(clientConf.publisher, deployableArtifacts, client);
                }

                if (clientConf.publisher.isPublishBuildInfo() &&
                        (clientConf.publisher.isEvenUnstable() || !wereThereTestFailures)) {
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

    private Set<DeployDetails> prepareDeployableArtifacts(Build build,
            Map<String, DeployDetails> deployableArtifactBuilders) {
        Set<DeployDetails> deployableArtifacts = Sets.newLinkedHashSet();
        List<Module> modules = build.getModules();
        for (Module module : modules) {
            List<Artifact> artifacts = module.getArtifacts();
            for (Artifact artifact : artifacts) {
                String artifactId = BuildInfoExtractorUtils.getArtifactId(module.getId(), artifact.getName());
                DeployDetails deployable = deployableArtifactBuilders.get(artifactId);
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
        return deployableArtifacts;
    }

    private void deployArtifacts(ArtifactoryClientConfiguration.PublisherHandler publishConf,
            Set<DeployDetails> deployableArtifacts,
            ArtifactoryBuildInfoClient client) {
        logger.info("Artifactory Build Info Recorder: Deploying artifacts to " + publishConf.getUrl());

        IncludeExcludePatterns includeExcludePatterns = getArtifactDeploymentPatterns(publishConf);
        for (DeployDetails artifact : deployableArtifacts) {
            String artifactPath = artifact.getArtifactPath();
            if (PatternMatcher.pathConflicts(artifactPath, includeExcludePatterns)) {
                logger.info("Artifactory Build Info Recorder: Skipping the deployment of '" +
                        artifactPath + "' due to the defined include-exclude patterns.");
                continue;
            }

            try {
                client.deployArtifact(artifact);
            } catch (IOException e) {
                throw new RuntimeException("Error occurred while publishing artifact to Artifactory: " +
                        artifact.getFile() +
                        ".\n Skipping deployment of remaining artifacts (if any) and build info.", e);
            }
        }
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

    private IncludeExcludePatterns getArtifactDeploymentPatterns(
            ArtifactoryClientConfiguration.PublisherHandler publishConf) {
        return new IncludeExcludePatterns(publishConf.getIncludePatterns(), publishConf.getExcludePatterns());
    }
}
