package org.jfrog.build.client;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

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
    /**
     * sha256 checksum of the file to deploy.
     */
    private String sha256;
    /**
     * In case of deploy - is the deployment succeeded.
     */
    private Boolean deploySucceeded;
    /**
     * Target deployment repository.
     */
    private String targetRepository;

    /**
     * Properties to attach to the deployed file as matrix params.
     */
    private Map<String, Collection<String>> properties;

    public DeployableArtifactDetail(String artifactSource, String artifactDest, String sha1, String sha256, Boolean deploySucceeded, String targetRepository, Map<String, Collection<String>> properties) {
        this.sourcePath = artifactSource;
        this.artifactDest = artifactDest;
        this.sha1 = sha1;
        this.sha256 = sha256;
        this.deploySucceeded = deploySucceeded;
        this.targetRepository = targetRepository;
        this.properties = properties;
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

    public String getSha256() {
        return sha256;
    }

    public String getTargetRepository() {
        return targetRepository;
    }

    public Boolean isDeploySucceeded() {
        return deploySucceeded;
    }

    public Map<String, Collection<String>> getProperties() {
        return properties;
    }
}
