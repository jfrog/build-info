package org.jfrog.build.extractor;

import org.jfrog.build.client.ArtifactoryUploadResponse;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class manages artifacts deployment after a maven / gradle build.
 * If publishForkCount is larger than 1, deployment will be parallel between modules.
 * The deployment of every module will always be serial, with maven / gradle descriptors deployed last. This is done to prevent conflicts in Artifactory.
 */
public class ModuleParallelDeployHelper {
    public static final int DEFAULT_DEPLOYMENT_THREADS = 3;

    public void deployArtifacts(ArtifactoryManager artifactoryManager,
                                Map<String, Set<DeployDetails>> deployableArtifactsByModule, int publishForkCount) {
        if (publishForkCount <= 1) {
            deployableArtifactsByModule.forEach((module, deployableArtifacts) -> deploy(artifactoryManager, deployableArtifacts, null));
        } else {
            try {
                ExecutorService executor = Executors.newFixedThreadPool(publishForkCount);
                CompletableFuture<Void> allDeployments = CompletableFuture.allOf(
                        deployableArtifactsByModule.values().stream()
                                .map(deployDetails ->
                                        CompletableFuture.runAsync(() ->
                                                deploy(artifactoryManager, deployDetails, "[" + Thread.currentThread().getName() + "]"), executor))
                                .toArray(CompletableFuture[]::new));
                allDeployments.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void deploy(ArtifactoryManager artifactoryManager, Set<DeployDetails> deployableArtifacts, String logPrefix) {
        deployableArtifacts.forEach(artifact -> {
            try {
                ArtifactoryUploadResponse response = artifactoryManager.upload(artifact, logPrefix);
                // Save information returned from Artifactory after the deployment.
                artifact.setDeploySucceeded(true);
                artifact.setSha256(response.getChecksums().getSha256());
                // When a maven SNAPSHOT artifact is deployed, Artifactory adds a timestamp to the artifact name, after the artifact is deployed.
                // ArtifactPath needs to be updated accordingly.
                artifact.setArtifactPath(response.getPath());
            } catch (IOException e) {
                artifact.setDeploySucceeded(false);
                artifact.setSha256("");
                throw new RuntimeException("Error occurred while publishing artifact to Artifactory: " +
                        artifact.getFile() +
                        ".\n Skipping deployment of remaining artifacts (if any) and build info.", e);
            }
        });
    }
}
