package org.jfrog.build.api.builder;

import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.Artifact;

import java.util.Properties;

/**
 * A builder for the artifact class
 *
 * @author Noam Y. Tenne
 */
public class ArtifactBuilder {

    private String name;
    private String type;
    private String sha1;
    private String sha256;
    private String md5;
    private String remotePath;
    private Properties properties;

    public ArtifactBuilder(String name) {
        this.name = name;
    }

    /**
     * Assembles the artifact class
     *
     * @return Assembled dependency
     */
    public Artifact build() {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Artifact must have a name");
        }
        Artifact artifact = new Artifact();
        artifact.setName(name);
        artifact.setType(type);
        artifact.setSha1(sha1);
        artifact.setSha256(sha256);
        artifact.setMd5(md5);
        artifact.setRemotePath(remotePath);
        artifact.setProperties(properties);
        return artifact;
    }

    /**
     * Sets the name of the artifact
     *
     * @param name Artifact name
     * @return Builder instance
     */
    public ArtifactBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the type of the artifact
     *
     * @param type Artifact type
     * @return Builder instance
     */
    public ArtifactBuilder type(String type) {
        this.type = type;
        return this;
    }

    /**
     * Sets the SHA1 checksum of the artifact
     *
     * @param sha1 Artifact SHA1 checksum
     * @return Builder instance
     */
    public ArtifactBuilder sha1(String sha1) {
        this.sha1 = sha1;
        return this;
    }

    /**
     * Sets the SHA256 checksum of the artifact
     *
     * @param sha256 Artifact SHA256 checksum
     * @return Builder instance
     */
    public ArtifactBuilder sha256(String sha256) {
        this.sha256 = sha256;
        return this;
    }

    /**
     * Sets the MD5 checksum of the artifact
     *
     * @param md5 Artifact MD5 checksum
     * @return Builder instance
     */
    public ArtifactBuilder md5(String md5) {
        this.md5 = md5;
        return this;
    }

    /**
     * Sets the remote path of the artifact
     *
     * @param remotePath Artifact remote Path
     * @return Builder instance
     */
    public ArtifactBuilder remotePath(String remotePath) {
        this.remotePath = remotePath;
        return this;
    }

    /**
     * Sets the properties of the artifact
     *
     * @param properties Artifact properties
     * @return Builder instance
     */
    public ArtifactBuilder properties(Properties properties) {
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
    public ArtifactBuilder addProperty(Object key, Object value) {
        if (properties == null) {
            properties = new Properties();
        }
        properties.put(key, value);
        return this;
    }

    /**
     * Adds properties to the properties object
     *
     * @param properties Artifact properties
     * @return Builder instance
     */
    public ArtifactBuilder addProperties(Properties properties) {
        if (this.properties == null) {
            this.properties = new Properties();
        }
        this.properties.putAll(properties);
        return this;
    }
}
