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
package org.jfrog.build.api.ci;

/**
 * @author Tomer Cohen
 */
public interface BuildInfoConfigProperties {

    /**
     * Prefix for all config/runtime properties
     */
    String BUILD_INFO_CONFIG_PREFIX = "buildInfoConfig.";
    String PROPERTIES_FILE = "propertiesFile";
    String PROP_PROPS_FILE = BUILD_INFO_CONFIG_PREFIX + PROPERTIES_FILE;
    String EXPORT_FILE = "exportFile";
    String PROP_EXPORT_FILE_PATH = BUILD_INFO_CONFIG_PREFIX + EXPORT_FILE;

    String ACTIVATE_RECORDER = "org.jfrog.build.extractor.maven.recorder.activate";

    /**
     * Property for whether to include all environment variables in the generic set of build info properties
     */
    String INCLUDE_ENV_VARS = "includeEnvVars";

    String ENV_VARS_INCLUDE_PATTERNS = "envVarsIncludePatterns";
    String PROP_ENV_VARS_INCLUDE_PATTERNS = BUILD_INFO_CONFIG_PREFIX + ENV_VARS_INCLUDE_PATTERNS;

    String ENV_VARS_EXCLUDE_PATTERNS = "envVarsExcludePatterns";
    String PROP_ENV_VARS_EXCLUDE_PATTERNS = BUILD_INFO_CONFIG_PREFIX + ENV_VARS_EXCLUDE_PATTERNS;

    /**
     * Secondary environment variable to hold the properties file name
     */
    String ENV_BUILDINFO_PROPFILE = "BUILDINFO_PROPFILE";

    /**
     * Maven property which indicates whether to resolve dependencies from Artifactory.
     */
    String ARTIFACTORY_RESOLUTION_ENABLED = "artifactoryResolutionEnabled";

    String PROP_ARTIFACTORY_RESOLUTION_ENABLED = BUILD_INFO_CONFIG_PREFIX + ARTIFACTORY_RESOLUTION_ENABLED;
}