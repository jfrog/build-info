package org.jfrog.build.api;

import org.apache.commons.lang3.StringUtils;

/**
 * Contains the build deployed artifact information
 *
 * @author Noam Y. Tenne
 */
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
        return StringUtils.equals(name, artifact.name) && StringUtils.equals(remotePath, artifact.remotePath);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}