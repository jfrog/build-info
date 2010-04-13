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
package org.jfrog.build.client;

/**
 * @author Tomer Cohen
 */
public interface ClientProperties {
    String ARTIFACTORY_PREFIX = "artifactory.";

    /**
     * The URL of the artifactory web application (typically ending with '/artifactory')
     */
    String PROP_CONTEXT_URL = ARTIFACTORY_PREFIX + "contextUrl";

    /**
     * The repo key in Artifactory from where to resolve artifacts.
     */
    String PROP_RESOLVE_REPOKEY = ARTIFACTORY_PREFIX + "artifactory.resolve.repoKey";

    /**
     * The repo key in Artifactory to where to publish artifacts.
     */
    String PROP_PUBLISH_REPOKEY = ARTIFACTORY_PREFIX + "artifactory.publish.repoKey";
    /**
     * The username to use when publishing artifacts to Artifactory.
     */
    String PROP_PUBLISH_USERNAME = ARTIFACTORY_PREFIX + "artifactory.publish.username";

    /**
     * The password to use when publishing artifacts to Artifactory.
     */
    String PROP_PUBLISH_PASSWORD = ARTIFACTORY_PREFIX + "artifactory.publish.password";

}