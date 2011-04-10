package org.jfrog.build.extractor.gradle;

import org.gradle.api.Project;
import org.gradle.api.artifacts.PublishArtifact;
import org.jfrog.build.client.DeployDetails;

/**
 * Container class that associates a Gradle {@link PublishArtifact} to its corresponding {@link DeployDetails}
 *
 * @author Tomer Cohen
 */
public class GradleDeployDetails {

    private final DeployDetails deployDetails;
    private final PublishArtifact publishArtifact;
    private final Project project;

    public GradleDeployDetails(PublishArtifact publishArtifact, DeployDetails deployDetails, Project project) {
        this.deployDetails = deployDetails;
        this.publishArtifact = publishArtifact;
        this.project = project;
    }

    public DeployDetails getDeployDetails() {
        return deployDetails;
    }

    public Project getProject() {
        return project;
    }

    public PublishArtifact getPublishArtifact() {
        return publishArtifact;
    }
}
