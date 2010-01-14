package org.artifactory.build.api;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Contains the build deployed artifact information
 *
 * @author Noam Y. Tenne
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
}