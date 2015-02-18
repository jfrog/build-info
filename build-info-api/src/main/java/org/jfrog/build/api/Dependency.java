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

/**
 * Contains the build module dependency information
 *
 * @author Noam Y. Tenne
 */
@XStreamAlias(BuildBean.DEPENDENCY)
public class Dependency extends BaseBuildFileBean {

    public static final String SCOPE_BUILD = "_build_";

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
     * Returns an ID list of dependencies that directly depend on this dependency. Used for building the module's
     * transitive dependency graph. Can be left empty if a root dependency.
     *
     * @return Required dependency IDs list
     */
    public List<String> getRequiredBy() {
        return requiredBy;
    }

    /**
     * Sets an ID list of dependencies that directly depend on this dependency.
     *
     * @param requiredBy Required dependency IDs list
     */
    public void setRequiredBy(List<String> requiredBy) {
        this.requiredBy = requiredBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Dependency)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Dependency that = (Dependency) o;

        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (requiredBy != null ? !requiredBy.equals(that.requiredBy) : that.requiredBy != null) {
            return false;
        }
        if (scopes != null ? !scopes.equals(that.scopes) : that.scopes != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (scopes != null ? scopes.hashCode() : 0);
        result = 31 * result + (requiredBy != null ? requiredBy.hashCode() : 0);
        return result;
    }
}