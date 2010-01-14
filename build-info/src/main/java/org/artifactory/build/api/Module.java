package org.artifactory.build.api;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import static org.artifactory.build.api.BuildBean.MODULE;

import java.util.List;

/**
 * Contains the build module information
 *
 * @author Noam Y. Tenne
 */
@XStreamAlias(MODULE)
public class Module extends BaseBuildBean {

    private String id;

    @XStreamAlias(ARTIFACTS)
    private List<Artifact> artifacts;

    @XStreamAlias(DEPENDENCIES)
    private List<Dependency> dependencies;

    /**
     * Returns the ID of the module
     *
     * @return Module ID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the ID of the module
     *
     * @param id Module ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the list of artifacts that have been deployed by the module
     *
     * @return Module deployed artifacts
     */
    public List<Artifact> getArtifacts() {
        return artifacts;
    }

    /**
     * Sets the list of artifacts that have been deployed by the module
     *
     * @param artifacts Module deployed artifacts
     */
    public void setArtifacts(List<Artifact> artifacts) {
        this.artifacts = artifacts;
    }

    /**
     * Returns the dependencies of the module
     *
     * @return Module dependencies
     */
    public List<Dependency> getDependencies() {
        return dependencies;
    }

    /**
     * Sets the dependencies of the module
     *
     * @param dependencies Module dependencies
     */
    public void setDependencies(List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }
}