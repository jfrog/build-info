package org.jfrog.build.extractor.ci;

import org.apache.commons.lang3.StringUtils;

/**
 * Contains the build deployed artifact information
 */
public class Artifact extends BaseBuildFileBean {

    private String name;
    private String originalDeploymentRepo;

    /**
     * Returns the name of the artifact
     *
     * @return Artifact name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the artifact
     *
     * @param name Artifact name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the original deployment repository of the artifact
     *
     * @return repository name
     */
    public String getOriginalDeploymentRepo() {
        return originalDeploymentRepo;
    }

    /**
     * Sets the original deployment repository of the artifact
     *
     * @param originalDeploymentRepo repository name
     */
    public void setOriginalDeploymentRepo(String originalDeploymentRepo) {
        this.originalDeploymentRepo = originalDeploymentRepo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        Artifact artifact = (Artifact) o;
        return StringUtils.equals(name, artifact.name) && StringUtils.equals(remotePath, artifact.remotePath);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    public org.jfrog.build.api.Artifact ToBuildArtifact() {
        org.jfrog.build.api.Artifact result = new org.jfrog.build.api.Artifact();
        result.setName(name);
        result.setType(type);
        result.setMd5(md5);
        result.setSha256(sha256);
        result.setSha1(sha1);
        result.setRemotePath(remotePath);
        result.setProperties(getProperties());
        result.setOriginalDeploymentRepo(originalDeploymentRepo);
        return result;
    }

    public static Artifact ToBuildInfoArtifact(org.jfrog.build.api.Artifact artifact) {
        Artifact result = new Artifact();
        result.setName(artifact.getName());
        result.setType(artifact.getType());
        result.setMd5(artifact.getMd5());
        result.setSha256(artifact.getSha256());
        result.setSha1(artifact.getSha1());
        result.setRemotePath(artifact.getRemotePath());
        result.setProperties(artifact.getProperties());
        result.setOriginalDeploymentRepo(artifact.getOriginalDeploymentRepo());
        return result;
    }
}
