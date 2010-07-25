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
package org.jfrog.build.api;

/**
 * @author Tomer Cohen
 */
public interface BuildInfoConfigProperties {

    /**
     * Prefix for all config/runtime properties
     */
    String BUILD_INFO_CONFIG_PREFIX = "buildInfoConfig.";
    String PROP_PROPS_FILE = BUILD_INFO_CONFIG_PREFIX + "propertiesFile";
    String PROP_EXPORT_FILE_PATH = BUILD_INFO_CONFIG_PREFIX + "exportFile";

    /**
     * Property for whether to include all environment variables in the generic set of build info properties
     */
    String PROP_INCLUDE_ENV_VARS = BUILD_INFO_CONFIG_PREFIX + "includeEnvVars";
}