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

    /**
     * Prefix for build info properties that are coming from the CI server.
     */
    String BUILD_INFO_ENVIRONMENT_PREFIX = "env.";

    /**
     * If this property is set the build info is persisted to this file.
     */
    String PROP_BUILD_INFO_OUTPUT_FILE = BUILD_INFO_PREFIX + "output.file";

    String PROP_BUILD_NAME = BUILD_INFO_PREFIX + "build.name";
    String PROP_BUILD_NUMBER = BUILD_INFO_PREFIX + "build.number";
    String PROP_BUILD_STARTED = BUILD_INFO_PREFIX + "build.started";
    String PROP_PARENT_BUILD_NAME = BUILD_INFO_PREFIX + "build.parentName";
    String PROP_PARENT_BUILD_NUMBER = BUILD_INFO_PREFIX + "build.parentNumber";
    String PROP_VCS_REVISION = BUILD_INFO_PREFIX + "vcs.revision";
    String PROP_PRINCIPAL = BUILD_INFO_PREFIX + "principal";
    String PROP_RELEASE_ENABLED = BUILD_INFO_PROP_PREFIX + "promotion.enabled";
    String PROP_RELEASE_COMMENT = BUILD_INFO_PROP_PREFIX + "promotion.comment";
    /**
     * A timestamp to add to deployed artifacts as matrix param. Usually same as build start time.
     */
    String PROP_BUILD_TIMESTAMP = BUILD_INFO_PREFIX + "build.timestamp";

    /**
     * Property to link the build back to the CI server that produced the build
     */
    String PROP_BUILD_URL = BUILD_INFO_PREFIX + "buildUrl";
    String PROP_BUILD_AGENT_NAME = BUILD_INFO_PREFIX + "buildAgent.name"; // maven, ivy, gradle...
    String PROP_BUILD_AGENT_VERSION = BUILD_INFO_PREFIX + "buildAgent.version";
    String PROP_AGENT_NAME = BUILD_INFO_PREFIX + "agent.name"; //hudson, teamcity...
    String PROP_AGENT_VERSION = BUILD_INFO_PREFIX + "agent.version"; //hudson, teamcity...

    String PROP_LICENSE_CONTROL_RUN_CHECKS = BUILD_INFO_PREFIX + "licenseControl.runChecks";
    String PROP_LICENSE_CONTROL_VIOLATION_RECIPIENTS = BUILD_INFO_PREFIX + "licenseControl.violationRecipients";
    String PROP_LICENSE_CONTROL_INCLUDE_PUBLISHED_ARTIFACTS =
            BUILD_INFO_PREFIX + "licenseControl.includePublishedArtifacts";
    String PROP_LICENSE_CONTROL_SCOPES = BUILD_INFO_PREFIX + "licenseControl.scopes";
    String PROP_LICENSE_CONTROL_AUTO_DISCOVER = BUILD_INFO_PREFIX + "licenseControl.autoDiscover";

    String PROP_BUILD_RETENTION_DAYS = BUILD_INFO_PREFIX + "buildRetention.daysToKeep";
    String PROP_BUILD_RETENTION_MINIMUM_DATE = BUILD_INFO_PREFIX + "buildRetention.minimumDate";
}