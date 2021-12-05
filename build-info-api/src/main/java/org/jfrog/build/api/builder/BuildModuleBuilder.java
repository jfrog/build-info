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

import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


public class BuildModuleBuilder {
    private String type;
    private String id;
    private String repository;
    private String sha1;
    private String md5;
    private List<Artifact> artifacts;
    private List<Artifact> excludedArtifacts;
    private List<Dependency> dependencies;
    private Properties properties;

    /**
     * Assembles the module class
     *
     * @return Assembled module
     */
    public Module build() {
        if (id == null || id.trim().length() == 0) {
            throw new IllegalArgumentException("Cannot build module entity without Module ID value");
        }
        Module module = new Module();
        module.setType(type);
        module.setId(id.trim());
        module.setRepository(repository);
        module.setSha1(sha1);
        module.setMd5(md5);
        module.setArtifacts(artifacts);
        module.setDependencies(dependencies);
        module.setProperties(properties);
        module.setExcludedArtifacts(excludedArtifacts);
        return module;
    }

    /**
     * Sets the type of the module
     *
     * @param type Module type
     * @return Builder instance
     */
    public BuildModuleBuilder type(String type) {
        this.type = type;
        return this;
    }

    /**
     * Sets the ID of the module
     *
     * @param id Module ID
     * @return Builder instance
     */
    public BuildModuleBuilder id(String id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the repository of the module
     *
     * @param repository Module repository
     * @return Builder instance
     */
    public BuildModuleBuilder repository(String repository) {
        this.repository = repository;
        return this;
    }

    /**
     * Sets sha1 of the module
     *
     * @param sha1 Module sha1
     * @return Builder instance
     */
    public BuildModuleBuilder sha1(String sha1) {
        this.sha1 = sha1;
        return this;
    }

    /**
     * Sets md5 of the module
     *
     * @param md5 Module md5
     * @return Builder instance
     */
    public BuildModuleBuilder md5(String md5) {
        this.md5 = md5;
        return this;
    }

    /**
     * Sets the list of artifacts that have been deployed by the module
     *
     * @param artifacts Module deployed artifacts
     * @return Builder instance
     */
    public BuildModuleBuilder artifacts(List<Artifact> artifacts) {
        this.artifacts = artifacts;
        return this;
    }

    /**
     * Sets the list of artifacts that have been excluded by the module
     *
     * @param excludedArtifacts Module excluded artifacts
     * @return Builder instance
     */
    public BuildModuleBuilder excludedArtifacts(List<Artifact> excludedArtifacts) {
        this.excludedArtifacts = excludedArtifacts;
        return this;
    }

    /**
     * Adds the given artifact to the artifacts list
     *
     * @param artifact Artifact to add
     * @return Builder instance
     */
    public BuildModuleBuilder addArtifact(Artifact artifact) {
        if (this.artifacts == null) {
            artifacts = new ArrayList<>();
        }
        artifacts.add(artifact);
        return this;
    }

    /**
     * Adds the given artifact to the exclude artifacts list
     *
     * @param artifact Artifact to add
     * @return Builder instance
     */
    public BuildModuleBuilder addExcludedArtifact(Artifact artifact) {
        if (this.excludedArtifacts == null) {
            excludedArtifacts = new ArrayList<>();
        }
        excludedArtifacts.add(artifact);
        return this;
    }

    /**
     * Sets the dependencies of the module
     *
     * @param dependencies Module dependencies
     * @return Builder instance
     */
    public BuildModuleBuilder dependencies(List<Dependency> dependencies) {
        this.dependencies = dependencies;
        return this;
    }

    /**
     * Adds the given dependency to the dependencies list
     *
     * @param dependency Dependency to add
     * @return Builder instance
     */
    public BuildModuleBuilder addDependency(Dependency dependency) {
        if (this.dependencies == null) {
            dependencies = new ArrayList<>();
        }
        dependencies.add(dependency);
        return this;
    }

    /**
     * Sets the properties of the module
     *
     * @param properties Module properties
     * @return Builder instance
     */
    public BuildModuleBuilder properties(Properties properties) {
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
    public BuildModuleBuilder addProperty(Object key, Object value) {
        if (properties == null) {
            properties = new Properties();
        }
        properties.put(key, value);
        return this;
    }
}