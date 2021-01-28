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

package org.jfrog.build.api.builder;

import org.apache.commons.lang.ArrayUtils;
import org.jfrog.build.api.Dependency;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * A builder for the dependency class
 *
 * @author Noam Y. Tenne
 */
public class DependencyBuilder {

    private String id;
    private String type;
    private Set<String> scopes;
    private String sha1;
    private String sha256;
    private String md5;
    private String localPath;
    private String remotePath;
    private String[][] requestedBy;
    private Properties properties;

    /**
     * Assembles the dependency class
     *
     * @return Assembled dependency
     */
    public Dependency build() {
        Dependency dependency = new Dependency();
        dependency.setId(id);
        dependency.setType(type);
        dependency.setScopes(scopes);
        dependency.setSha1(sha1);
        dependency.setSha256(sha256);
        dependency.setMd5(md5);
        dependency.setLocalPath(localPath);
        dependency.setRemotePath(remotePath);
        dependency.setRequestedBy(requestedBy);
        dependency.setProperties(properties);
        return dependency;
    }

    /**
     * Sets the ID of the dependency
     *
     * @param id Dependency ID
     * @return Builder instance
     */
    public DependencyBuilder id(String id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the type of the dependency
     *
     * @param type Dependency type
     * @return Builder instance
     */
    public DependencyBuilder type(String type) {
        this.type = type;
        return this;
    }

    /**
     * Sets the scope list of the dependency
     *
     * @param scopes Dependency scope list
     * @return Builder instance
     */
    public DependencyBuilder scopes(Set<String> scopes) {
        this.scopes = scopes;
        return this;
    }

    /**
     * Adds the given scope to the scopes set
     *
     * @param scope Dependency scope list
     * @return Builder instance
     */
    public DependencyBuilder addScope(String scope) {
        if (scopes == null) {
            scopes = new HashSet<>();
        }
        scopes.add(scope);
        return this;
    }

    /**
     * Sets the SHA1 checksum of the dependency
     *
     * @param sha1 Dependency SHA1 checksum
     * @return Builder instance
     */
    public DependencyBuilder sha1(String sha1) {
        this.sha1 = sha1;
        return this;
    }

    /**
     * Sets the SHA256 checksum of the dependency
     *
     * @param sha256 Dependency SHA256 checksum
     * @return Builder instance
     */
    public DependencyBuilder sha256(String sha256) {
        this.sha256 = sha256;
        return this;
    }

    /**
     * Sets the MD5 checksum of the dependency
     *
     * @param md5 Dependency MD5 checksum
     * @return Builder instance
     */
    public DependencyBuilder md5(String md5) {
        this.md5 = md5;
        return this;
    }

    /**
     * Sets the local path of the dependency
     *
     * @param localPath Dependency local path
     * @return Builder instance
     */
    public DependencyBuilder localPath(String localPath) {
        this.localPath = localPath;
        return this;
    }

    /**
     * Sets the remote path of the dependency
     *
     * @param remotePath Dependency remote path
     * @return Builder instance
     */
    public DependencyBuilder remotePath(String remotePath) {
        this.remotePath = remotePath;
        return this;
    }

    /**
     * Sets path-to-root dependency lists that directly depend on this dependency.
     *
     * @param requestedBy dependency path-to-root lists
     * @return Builder instance
     */
    public DependencyBuilder requestedBy(String[][] requestedBy) {
        this.requestedBy = requestedBy;
        return this;
    }

    /**
     * Adds an ID of another dependency that requires this one to the requestedBy dependencies list.
     *
     * @param pathToModuleRoot - path from parent dependency to the module ID, modules separated
     * @return Builder instance
     */
    public DependencyBuilder addRequestedBy(String[] pathToModuleRoot) {
        this.requestedBy = (String[][]) ArrayUtils.add(requestedBy, pathToModuleRoot);
        return this;
    }

    /**
     * Sets the properties of the dependency
     *
     * @param properties Dependency properties
     * @return Builder instance
     */
    public DependencyBuilder properties(Properties properties) {
        this.properties = properties;
        return this;
    }

    /**
     * Adds the given property to the properties object
     *
     * @param key   Key of property to add
     * @param value Value of property to add
     * @return Builder instance
     */
    public DependencyBuilder addProperty(Object key, Object value) {
        if (properties == null) {
            properties = new Properties();
        }
        properties.put(key, value);
        return this;
    }

    /**
     * Adds properties to the properties object
     *
     * @param properties Dependencies properties
     * @return Builder instance
     */
    public DependencyBuilder addProperties(Properties properties) {
        if (this.properties == null) {
            this.properties = new Properties();
        }
        this.properties.putAll(properties);
        return this;
    }
}