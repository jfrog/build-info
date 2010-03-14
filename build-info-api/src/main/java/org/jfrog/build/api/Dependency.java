package org.jfrog.build.api;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.util.List;

/**
 * Contains the build module dependency information
 *
 * @author Noam Y. Tenne
 */
@XStreamAlias(BuildBean.DEPENDENCY)
public class Dependency extends BaseBuildFileBean {

    private String id;
    private List<String> scopes;
    private List<String> requiredBy;

    /**
     * Returns the ID of the dependency
     *
     * @return Dependency ID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the ID of the dependency
     *
     * @param id Dependency ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the scope list of the dependency
     *
     * @return Dependency scope list
     */
    public List<String> getScopes() {
        return scopes;
    }

    /**
     * Sets the scope list of the dependency
     *
     * @param scopes Dependency scope list
     */
    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    /**
     * Returns an ID list of other dependencies required by this one
     *
     * @return Required dependency IDs list
     */
    public List<String> getRequiredBy() {
        return requiredBy;
    }

    /**
     * Sets an ID list of other dependencies required by this one
     *
     * @param requiredBy Required dependency IDs list
     */
    public void setRequiredBy(List<String> requiredBy) {
        this.requiredBy = requiredBy;
    }
}