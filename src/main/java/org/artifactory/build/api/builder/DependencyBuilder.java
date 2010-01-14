package org.artifactory.build.api.builder;

import com.google.common.collect.Lists;
import org.artifactory.build.api.Dependency;

import java.util.List;
import java.util.Properties;

/**
 * A builder for the dependency class
 *
 * @author Noam Y. Tenne
 */
public class DependencyBuilder {

    private String id = "";
    private String type = "";
    private List<String> scopes = Lists.newArrayList();
    private String sha1 = "";
    private String md5 = "";
    private List<String> requiredBy = Lists.newArrayList();
    private Properties properties = new Properties();

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
        dependency.setMd5(md5);
        dependency.setRequiredBy(requiredBy);
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
    public DependencyBuilder scopes(List<String> scopes) {
        this.scopes = scopes;
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
     * Sets an ID list of other dependencies required by this one
     *
     * @param requiredBy Required dependency IDs list
     * @return Builder instance
     */
    public DependencyBuilder requiredBy(List<String> requiredBy) {
        this.requiredBy = requiredBy;
        return this;
    }

    /**
     * Adds an ID of another dependency required by this one to the required dependencies list
     *
     * @param requiredBy Required dependency ID
     * @return Builder instance
     */
    public DependencyBuilder addRequiredBy(String requiredBy) {
        this.requiredBy.add(requiredBy);
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
        properties.put(key, value);
        return this;
    }
}