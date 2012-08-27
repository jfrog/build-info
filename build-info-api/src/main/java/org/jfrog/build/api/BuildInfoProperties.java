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
    String BUILD_INFO_PROP_PREFIX = BUILD_INFO_PREFIX + "property.";
    String BUILD_INFO_LICENSE_CONTROL_PREFIX = BUILD_INFO_PREFIX + "licenseControl.";
    String BUILD_INFO_ISSUES_TRACKER_PREFIX = BUILD_INFO_PREFIX + "issues.";

    /**
     * Prefix for build info properties that are coming from the CI server.
     */
    String BUILD_INFO_ENVIRONMENT_PREFIX = BUILD_INFO_PREFIX + "env.";
}