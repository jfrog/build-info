/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
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
    /**
     * Property to link the build back to the CI server that produced the build
     */
    String PROP_BUILD_URL = BUILD_INFO_PREFIX + "buildUrl";
    String PROP_BUILD_AGENT = BUILD_INFO_PREFIX + "agent";
    String PROP_BUILD_BUILD_AGENT_NAME = BUILD_INFO_PREFIX + "buildAgent";
}