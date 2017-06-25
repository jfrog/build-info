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

import org.apache.commons.lang.StringUtils;
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
}
