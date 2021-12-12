package org.jfrog.build.api.builder;

import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * A builder for the module class
 *
 * @author Noam Y. Tenne
 */
public class ModuleBuilder {

    private ModuleType type;
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
        if (type != null) {
            module.setType(type.name().toLowerCase());
        }
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
    public ModuleBuilder type(ModuleType type) {
        this.type = type;
        return this;
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
     * Sets the repository of the module
     *
     * @param repository Module repository
     * @return Builder instance
     */
    public ModuleBuilder repository(String repository) {
        this.repository = repository;
        return this;
    }

    /**
     * Sets sha1 of the module
     *
     * @param sha1 Module sha1
     * @return Builder instance
     */
    public ModuleBuilder sha1(String sha1) {
        this.sha1 = sha1;
        return this;
    }

    /**
     * Sets md5 of the module
     *
     * @param md5 Module md5
     * @return Builder instance
     */
    public ModuleBuilder md5(String md5) {
        this.md5 = md5;
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
     * Sets the list of artifacts that have been excluded by the module
     *
     * @param excludedArtifacts Module excluded artifacts
     * @return Builder instance
     */
    public ModuleBuilder excludedArtifacts(List<Artifact> excludedArtifacts) {
        this.excludedArtifacts = excludedArtifacts;
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
    public ModuleBuilder addExcludedArtifact(Artifact artifact) {
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