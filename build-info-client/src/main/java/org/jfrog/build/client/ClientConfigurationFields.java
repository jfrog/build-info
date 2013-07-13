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
package org.jfrog.build.client;

/**
 * @author freds
 */
public interface ClientConfigurationFields {
    String NAME = "name";
    String USERNAME = "username";
    String HOST = "host";
    String PORT = "port";
    String PASSWORD = "password";
    String MAVEN = "maven";
    String IVY = "ivy";
    String IVY_M2_COMPATIBLE = "ivy.m2compatible";
    String IVY_ART_PATTERN = "ivy.artPattern";
    String IVY_REPO_DEFINED = "ivy.repo.defined";
    String IVY_IVY_PATTERN = "ivy.ivyPattern";
    String URL = "url";
    String REPO_KEY = "repoKey";
    // Publish fields
    String AGGREGATE_ARTIFACTS = "aggregate";   // String  - directory where artifacts should be aggregated
    String PUBLISH_AGGREGATED_ARTIFACTS = "aggregated"; // Boolean - whether or not aggregated artifacts should be published
    String COPY_AGGREGATED_ARTIFACTS = "copy.aggregated"; // Boolean - whether or not aggregated artifacts should be published
    String PUBLISH_ARTIFACTS = "artifacts";
    String PUBLISH_BUILD_INFO = "buildInfo";
    String SNAPSHOT_REPO_KEY = "snapshot.repoKey";
    String MATRIX = "matrix";
    String ARTIFACT_SPECS = "artifactSpecs";
    String INCLUDE_PATTERNS = "includePatterns";
    String EXCLUDE_PATTERNS = "excludePatterns";
    String EVEN_UNSTABLE = "unstable";
    String CONTEXT_URL = "contextUrl";
}
