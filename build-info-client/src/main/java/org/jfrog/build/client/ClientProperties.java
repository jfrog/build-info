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
    String PROP_RESOLVE_REPOKEY = ARTIFACTORY_PREFIX + "resolve.repoKey";

    /**
     * The repo key in Artifactory to where to publish artifacts.
     */
    String PROP_PUBLISH_REPOKEY = ARTIFACTORY_PREFIX + "publish.repoKey";
    /**
     * The username to use when publishing artifacts to Artifactory.
     */
    String PROP_PUBLISH_USERNAME = ARTIFACTORY_PREFIX + "publish.username";

    /**
     * The password to use when publishing artifacts to Artifactory.
     */
    String PROP_PUBLISH_PASSWORD = ARTIFACTORY_PREFIX + "publish.password";

    /**
     * Property whether to publish Ivy descriptors.
     */
    String PROP_PUBLISH_IVY = ARTIFACTORY_PREFIX + "publish.ivy";

    /**
     * Property whether to publish Maven POMs.
     */
    String PROP_PUBLISH_MAVEN = ARTIFACTORY_PREFIX + "publish.maven";

    /**
     * Property whether to publish the generated build artifacts.
     */
    String PROP_PUBLISH_ARTIFACT = ARTIFACTORY_PREFIX + "publish.artifact";

}