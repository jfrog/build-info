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


    String PROP_TIMEOUT = ARTIFACTORY_PREFIX + "timeout";

    String PROP_PROXY_HOST = ARTIFACTORY_PREFIX + "proxy.host";

    String PROP_PROXY_PORT = ARTIFACTORY_PREFIX + "proxy.port";

    String PROP_PROXY_USERNAME = ARTIFACTORY_PREFIX + "proxy.username";

    String PROP_PROXY_PASSWORD = ARTIFACTORY_PREFIX + "proxy.password";

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
     * Property for whether to publish the generated build artifacts.
     */
    String PROP_PUBLISH_ARTIFACT = ARTIFACTORY_PREFIX + "publish.artifacts";

    /**
     * Property for whether to publish the generated build info.
     */
    String PROP_PUBLISH_BUILD_INFO = ARTIFACTORY_PREFIX + "publish.buildInfo";

    /**
     * Prefix for properties that are dynamically added to deployment (as matrix params)
     */
    String PROP_DEPLOY_PARAM_PROP_PREFIX = ARTIFACTORY_PREFIX + "deploy.";
}