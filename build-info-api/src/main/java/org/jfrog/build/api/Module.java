/*
 * Copyright (C) 2011 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.build.api;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.util.List;

import static org.jfrog.build.api.BuildBean.MODULE;

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

    @XStreamAlias(EXCLUDED_ARTIFACTS)
    private List<Artifact> excludedArtifacts;

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
     * Returns the list of excluded_artifacts that haven't been deployed by the module
     *
     * @return Module deployed artifacts
     */
    public List<Artifact> getExcludedArtifacts() {
        return excludedArtifacts;
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

    /**
     * Sets the list of artifacts that haven't been deployed by the module
     *
     * @param excludedArtifacts Module excluded artifacts
     */
    public void setExcludedArtifacts(List<Artifact> excludedArtifacts) {
        this.excludedArtifacts = excludedArtifacts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Module module = (Module) o;

        if (getId() != null ? !getId().equals(module.getId()) : module.getId() != null) return false;
        if (getArtifacts() != null ? !getArtifacts().equals(module.getArtifacts()) : module.getArtifacts() != null)
            return false;
        if (getExcludedArtifacts() != null ? !getExcludedArtifacts().equals(module.getExcludedArtifacts()) : module.getExcludedArtifacts() != null)
            return false;
        return getDependencies() != null ? getDependencies().equals(module.getDependencies()) : module.getDependencies() == null;

    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getArtifacts() != null ? getArtifacts().hashCode() : 0);
        result = 31 * result + (getExcludedArtifacts() != null ? getExcludedArtifacts().hashCode() : 0);
        result = 31 * result + (getDependencies() != null ? getDependencies().hashCode() : 0);
        return result;
    }
}