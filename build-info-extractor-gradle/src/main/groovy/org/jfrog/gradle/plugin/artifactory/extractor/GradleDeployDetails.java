package org.jfrog.gradle.plugin.artifactory.extractor;

import org.gradle.api.Project;
import org.gradle.api.artifacts.PublishArtifact;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;

/**
 * Container class that associates a Gradle {@link PublishArtifact} to its corresponding {@link DeployDetails}
 *
 * @author Tomer Cohen
 */
public class GradleDeployDetails implements Comparable<GradleDeployDetails> {

    private final DeployDetails deployDetails;
    private final PublishArtifactInfo publishArtifact;
    private final Project project;

    public GradleDeployDetails(PublishArtifactInfo publishArtifact, DeployDetails deployDetails, Project project) {
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

    public PublishArtifactInfo getPublishArtifact() {
        return publishArtifact;
    }

    public int compareTo(GradleDeployDetails that) {
        if (this.publishArtifact == null) {
            return -1;
        }
        if (that.publishArtifact == null) {
            return 1;
        }

        int compareDeployDetails = this.deployDetails.compareTo(that.deployDetails);
        if (compareDeployDetails == 0) {
            return 0;
        }

        String thisExtension = this.publishArtifact.getExtension();
        String thatExtension = that.publishArtifact.getExtension();
        if (thisExtension == null) {
            return -1;
        }
        if (thatExtension == null) {
            return 1;
        }
        thisExtension = thisExtension.toLowerCase();
        if ("xml".equals(thisExtension) || "pom".equals(thisExtension)) {
            return 1;
        }
        thatExtension = thatExtension.toLowerCase();
        if ("xml".equals(thatExtension) || "pom".equals(thatExtension)) {
            return -1;
        }
        return compareDeployDetails;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GradleDeployDetails that = (GradleDeployDetails) o;

        if (deployDetails != null ? !deployDetails.equals(that.deployDetails) : that.deployDetails != null)
            return false;
        if (publishArtifact != null ? !publishArtifact.equals(that.publishArtifact) : that.publishArtifact != null)
            return false;
        return !(project != null ? !project.equals(that.project) : that.project != null);
    }

    @Override
    public int hashCode() {
        int result = deployDetails != null ? deployDetails.hashCode() : 0;
        result = 31 * result + (publishArtifact != null ? publishArtifact.hashCode() : 0);
        result = 31 * result + (project != null ? project.hashCode() : 0);
        return result;
    }
}
