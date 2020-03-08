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
package org.jfrog.build.extractor.clientConfiguration;

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
    String NPM_INSTALL_ARGS = "npm.installArgs";
    String NPM_EXECUTABLE_PATH = "npm.executablePath"; // Path to npm CLI
    String NPM_PATH = "npm.path"; // Path to package.json dir
    String NPM_MODULE = "npm.module"; // Custom npm module name for the build-info
    String URL = "url";
    String REPO_KEY = "repoKey";
    String DOWN_SNAPSHOT_REPO_KEY = "downSnapshotRepoKey";
    // Publish fields
    String AGGREGATE_ARTIFACTS = "aggregate";   // String  - directory where artifacts should be aggregated
    String PUBLISH_AGGREGATED_ARTIFACTS = "aggregated"; // Boolean - whether or not aggregated artifacts should be published
    String COPY_AGGREGATED_ARTIFACTS = "copy.aggregated"; // Boolean - whether or not aggregated artifacts should be published
    String PUBLISH_ARTIFACTS = "artifacts";
    String PUBLISH_BUILD_INFO = "buildInfo";
    String PUBLISH_FORK_COUNT = "forkCount";
    String RECORD_ALL_DEPENDENCIES = "record.all.dependencies";
    String SNAPSHOT_REPO_KEY = "snapshot.repoKey";
    String RELEASE_REPO_KEY = "release.repoKey";
    String MATRIX = "matrix";
    String ARTIFACT_SPECS = "artifactSpecs";
    String INCLUDE_PATTERNS = "includePatterns";
    String EXCLUDE_PATTERNS = "excludePatterns";
    String FILTER_EXCLUDED_ARTIFACTS_FROM_BUILD = "filterExcludedArtifactsFromBuild";
    String EVEN_UNSTABLE = "unstable";
    String CONTEXT_URL = "contextUrl";
}
