/*
 * Copyright (C) 2010 JFrog Ltd.
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

package org.jfrog.build.client;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.BuildFileBean;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Simple value object holding an artifact deploy request details.
 *
 * @author Yossi Shaul
 */
public class DeployDetails {
    /**
     * Target deploy repository for release versions
     */
    private String targetRepository;
    /**
     * Target deploy repository for snapshot versions
     */
    private String targetSnapshotsRepository;
    /**
     * Artifact deployment path
     */
    String artifactPath;
    /**
     * The file to deploy
     */
    File file;
    /**
     * sha1 checksum of the file to deploy
     */
    String sha1;
    /**
     * md5 checksum of the file to deploy
     */
    String md5;
    /**
     * Properties to attach to the deployed file
     */
    Map<String, String> properties;

    public static class Builder {
        private DeployDetails deployDetails;

        public Builder() {
            deployDetails = new DeployDetails();
        }

        public DeployDetails build() {
            if (deployDetails.file == null || !deployDetails.file.exists()) {
                throw new IllegalArgumentException("File not found: " + deployDetails.file);
            }
            if (StringUtils.isBlank(deployDetails.targetRepository)) {
                throw new IllegalArgumentException("Target repository cannot be empty");
            }
            if (StringUtils.isBlank(deployDetails.artifactPath)) {
                throw new IllegalArgumentException("Artifact path cannot be empty");
            }
            return deployDetails;
        }

        public Builder bean(BuildFileBean bean) {
            Properties beanProperties = bean.getProperties();
            if (beanProperties != null) {
                deployDetails.properties = Maps.fromProperties(beanProperties);
            }
            deployDetails.sha1 = bean.getSha1();
            deployDetails.md5 = bean.getMd5();
            return this;
        }

        public Builder file(File file) {
            deployDetails.file = file;
            return this;
        }

        public Builder targetRepository(String targetRepository) {
            deployDetails.targetRepository = targetRepository;
            return this;
        }

        public Builder targetSnapshotsRepository(String targetSnapshotsRepository) {
            deployDetails.targetSnapshotsRepository = targetSnapshotsRepository;
            return this;
        }

        public Builder artifactPath(String artifactPath) {
            deployDetails.artifactPath = artifactPath;
            return this;
        }

        public Builder sha1(String sha1) {
            deployDetails.sha1 = sha1;
            return this;
        }

        public Builder md5(String md5) {
            deployDetails.md5 = md5;
            return this;
        }

        public Builder addProperty(String key, String value) {
            if (deployDetails.properties == null) {
                deployDetails.properties = new HashMap<String, String>();
            }
            deployDetails.properties.put(key, value);
            return this;
        }

        public Builder addProperties(Map<String, String> propertiesToAdd) {
            if (deployDetails.properties == null) {
                deployDetails.properties = new HashMap<String, String>();
            }
            deployDetails.properties.putAll(propertiesToAdd);
            return this;
        }
    }

    /**
     * @return Return the target deployment repository. Either the releases repository (default) or snapshots if
     *         defined and the deployed file is a snapshot.
     */
    public String getTargetRepository() {
        if (targetSnapshotsRepository != null && file.getPath().contains("-SNAPSHOT")) {
            return targetSnapshotsRepository;
        }
        return targetRepository;
    }

    public String getArtifactPath() {
        return artifactPath;
    }

    public File getFile() {
        return file;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
