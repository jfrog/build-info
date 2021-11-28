package org.jfrog.build.extractor.ci;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Contains the build deployed artifact information
 */
@XStreamAlias(BuildBean.ARTIFACT)
public class Artifact extends BaseBuildFileBean {

    private String name;

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
        return name != null ? name.equals(artifact.name) : artifact.name == null;
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
        result.setMd5(md5);
        result.setSha256(sha256);
        result.setSha1(sha1);
        result.setRemotePath(remotePath);
        result.setProperties(getProperties());
        return result;
    }

    public static Artifact ToBuildInfoArtifact(org.jfrog.build.api.Artifact artifact) {
        Artifact result = new Artifact();
        result.setName(artifact.getName());
        result.setMd5(artifact.getMd5());
        result.setSha256(artifact.getSha256());
        result.setSha1(artifact.getSha1());
        result.setRemotePath(artifact.getRemotePath());
        result.setProperties(artifact.getProperties());
        return result;
    }
}