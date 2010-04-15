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
public interface BuildInfoConfigProperties {

    /**
     * Prefix for all config/runtime properties
     */
    String BUILD_INFO_CONFIG_PREFIX = "buildInfoConfig.";
    /**
     * Prefix for properties that are dynamically added to deployment (as matrix params)
     */
    String BUILD_INFO_DEPLOY_PROP_PREFIX = BUILD_INFO_CONFIG_PREFIX + "deploy.";
    String PROP_PROPS_FILE = BUILD_INFO_CONFIG_PREFIX + "propertiesFile";
    String PROP_EXPORT_FILE_PATH = BUILD_INFO_CONFIG_PREFIX + "exportFile";
}