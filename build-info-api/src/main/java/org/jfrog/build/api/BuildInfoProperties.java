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
public interface BuildInfoProperties {

    /**
     * A prefix for all properties that should affect the build info model
     */
    String BUILD_INFO_PREFIX = "buildInfo.";

    /**
     * Prefix for properties that are dynamically added to build info
     */
    String BUILD_INFO_PROP_PREFIX = "buildInfo.property.";

    String PROP_BUILD_NAME = BUILD_INFO_PREFIX + "buildName";
    String PROP_BUILD_NUMBER = BUILD_INFO_PREFIX + "buildNumber";
    String PROP_PARENT_BUILD_NAME = BUILD_INFO_PREFIX + "parentBuildName";
    String PROP_PARENT_BUILD_NUMBER = BUILD_INFO_PREFIX + "parentBuildNumber";

    String PROP_VCS_REVISION = BUILD_INFO_PREFIX + "vcs.revision";

    /**
     * Property to link the build back to the CI server that produced the build
     */
    String PROP_BUILD_URL = BUILD_INFO_PREFIX + "buildUrl";
    String PROP_BUILD_AGENT = BUILD_INFO_PREFIX + "agent";
    String PROP_BUILD_BUILD_AGENT_NAME = BUILD_INFO_PREFIX + "buildAgent";
}