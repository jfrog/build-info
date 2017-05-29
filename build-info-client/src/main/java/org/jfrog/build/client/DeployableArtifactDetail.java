package org.jfrog.build.client;

import java.io.Serializable;

/**
 * Created by yahavi on 08/05/2017.
 */
public class DeployableArtifactDetail implements Serializable {
    /**
     * Artifact source path.
     */
    private String sourcePath;
    /**
     * Artifact deployment path.
     */
    private String artifactDest;
    /**
     * sha1 checksum of the file to deploy.
     */
    private String sha1;

    public DeployableArtifactDetail(String artifactSource, String artifactDest, String sha1) {
        this.sourcePath = artifactSource;
        this.artifactDest = artifactDest;
        this.sha1 = sha1;
    }

    public DeployableArtifactDetail() {
    }

    public String getArtifactDest() {
        return artifactDest;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public String getSha1() {
        return sha1;
    }
}
