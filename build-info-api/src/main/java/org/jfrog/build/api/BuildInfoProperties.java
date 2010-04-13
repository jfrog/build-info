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
    String BUILD_INFO_PROP_PREFIX = "buildInfo.";
    String BUILD_INFO_DEPLOY_PROP_PREFIX = BUILD_INFO_PROP_PREFIX + "deploy.";

    String PROP_PROPS_FILE = BUILD_INFO_PROP_PREFIX + "propertiesFile";
    String PROP_EXPORT_FILE_PATH = BUILD_INFO_PROP_PREFIX + "propertiesFile";
    String PROP_BUILD_NAME = BUILD_INFO_PROP_PREFIX + "buildName";
    String PROP_BUILD_NUMBER = BUILD_INFO_PROP_PREFIX + "buildNumber";
    String PROP_PARENT_BUILD_NAME = BUILD_INFO_PROP_PREFIX + "parentBuildName";
    String PROP_PARENT_BUILD_NUMBER = BUILD_INFO_PROP_PREFIX + "parentBuildNumber";
}
