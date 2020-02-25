package org.jfrog.build.extractor.moduleParallelDeployer;

import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
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
 * The deployment of every module will always be serial, with pom deployed last. This is done to prevent conflicts in Artifactory.
 */
public class ModuleParallelDeployHelper {
    public void deployArtifacts(ArtifactoryBuildInfoClient client,
                                Map<String, Set<DeployDetails>> deployableArtifactsByModule, int publishForkCount) {
        if (publishForkCount <= 1) {
            deployableArtifactsByModule.forEach((module, deployableArtifacts) -> deploy(client, deployableArtifacts, null));
        } else {
            try {
                ExecutorService executor = Executors.newFixedThreadPool(publishForkCount);
                CompletableFuture<Void> allDeployments = CompletableFuture.allOf(
                        deployableArtifactsByModule.values().stream()
                                .map(deployDetails ->
                                        CompletableFuture.runAsync(() ->
                                                deploy(client, deployDetails, "[" + Thread.currentThread().getName() + "]"), executor))
                                .toArray(CompletableFuture[]::new));
                allDeployments.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void deploy(ArtifactoryBuildInfoClient client, Set<DeployDetails> deployableArtifacts, String logPrefix) {
        deployableArtifacts.forEach(artifact -> {
            try {
                client.deployArtifact(artifact, logPrefix);
            } catch (IOException e) {
                throw new RuntimeException("Error occurred while publishing artifact to Artifactory: " +
                        artifact.getFile() +
                        ".\n Skipping deployment of remaining artifacts (if any) and build info.", e);
            }
        });
    }
}
