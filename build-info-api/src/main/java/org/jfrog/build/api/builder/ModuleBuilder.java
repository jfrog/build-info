package org.jfrog.build.api.builder;

import com.google.common.collect.Lists;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * A builder for the module class
 *
 * @author Noam Y. Tenne
 */
public class ModuleBuilder {

    private String id;
    private List<Artifact> artifacts;
    private List<Dependency> dependencies;
    private Properties properties;

    /**
     * Assembles the module class
     *
     * @return Assembled module
     */
    public Module build() {
        if (artifacts == null) {
            artifacts = Collections.emptyList();
        }
        if (dependencies == null) {
            dependencies = Collections.emptyList();
        }
        Module module = new Module();
        module.setId(id);
        module.setArtifacts(artifacts);
        module.setDependencies(dependencies);
        module.setProperties(properties);
        return module;
    }

    /**
     * Sets the ID of the module
     *
     * @param id Module ID
     * @return Builder instance
     */
    public ModuleBuilder id(String id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the list of artifacts that have been deployed by the module
     *
     * @param artifacts Module deployed artifacts
     * @return Builder instance
     */
    public ModuleBuilder artifacts(List<Artifact> artifacts) {
        this.artifacts = artifacts;
        return this;
    }

    /**
     * Adds the given artifact to the artifacts list
     *
     * @param artifact Artifact to add
     * @return Builder instance
     */
    public ModuleBuilder addArtifact(Artifact artifact) {
        if (this.artifacts == null) {
            artifacts = Lists.newArrayList();
        }
        artifacts.add(artifact);
        return this;
    }

    /**
     * Sets the dependencies of the module
     *
     * @param dependencies Module dependencies
     * @return Builder instance
     */
    public ModuleBuilder dependencies(List<Dependency> dependencies) {
        this.dependencies = dependencies;
        return this;
    }

    /**
     * Adds the given dependency to the dependencies list
     *
     * @param dependency Dependency to add
     * @return Builder instance
     */
    public ModuleBuilder addDependency(Dependency dependency) {
        if (this.dependencies == null) {
            dependencies = Lists.newArrayList();
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
    public ModuleBuilder properties(Properties properties) {
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
    public ModuleBuilder addProperty(Object key, Object value) {
        if (properties == null) {
            properties = new Properties();
        }
        properties.put(key, value);
        return this;
    }
}