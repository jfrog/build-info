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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

/**
 * Contains the build module dependency information
 *
 * @author Noam Y. Tenne
 */
@XStreamAlias(BuildBean.DEPENDENCY)
public class Dependency extends BaseBuildFileBean {

    public static final String SCOPE_BUILD = "_build_";

    private String id;
    private Set<String> scopes;

    /*
     * [["parentIdA", "a1", "a2",... "moduleId"],
     * ["parentIdB", "b1", "b2",... "moduleId"],
     * ["parentIdC", "c1", "c2",... "moduleId"],
     * ...]
     */
    private String[][] requestedBy;

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
    public Set<String> getScopes() {
        return scopes;
    }

    /**
     * Sets the scope list of the dependency
     *
     * @param scopes Dependency scope list
     */
    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    /**
     * Returns path-to-root dependency lists that directly depend on this dependency.
     * Used for building the module's transitive dependency graph. Can be left empty if a root dependency.
     *
     * @return dependency path-to-root lists.
     */
    public String[][] getRequestedBy() {
        return requestedBy;
    }

    /**
     * Sets path-to-root dependency lists that directly depend on this dependency.
     *
     * @param requestedBy dependency path-to-root lists
     */
    public void setRequestedBy(String[][] requestedBy) {
        this.requestedBy = requestedBy;
    }

    /**
     * Adds an ID of another dependency requested by this one to the required dependencies list.
     *
     * @param pathToModuleRoot - path from parent dependency to the module ID, modules separated
     */
    public void addRequestedBy(String[] pathToModuleRoot) {
        this.requestedBy = (String[][]) ArrayUtils.add(requestedBy, pathToModuleRoot);
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

        if (!StringUtils.equals(id, that.id)) {
            return false;
        }
        if (!StringUtils.equals(remotePath, that.remotePath)) {
            return false;
        }
        if (!Objects.equals(scopes, that.scopes)) {
            return false;
        }
        return Arrays.deepEquals(requestedBy, that.requestedBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, scopes, Arrays.deepHashCode(requestedBy), remotePath);
    }

    public org.jfrog.build.api.ci.Dependency ToBuildDependency() {
        org.jfrog.build.api.ci.Dependency result = new org.jfrog.build.api.ci.Dependency();
        result.setId(id);
        result.setRequestedBy(requestedBy);
        result.setScopes(scopes);
        result.setType(type);
        result.setMd5(md5);
        result.setSha256(sha256);
        result.setSha1(sha1);
        result.setRemotePath(remotePath);
        result.setProperties(getProperties());
        return result;
    }
}