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
package org.jfrog.build.client;

/**
 * @author Tomer Cohen
 */
public interface ClientProperties {
    String ARTIFACTORY_PREFIX = "artifactory.";

    /**
     * The URL of the artifactory web application (typically ending with '/artifactory')
     *
     * @deprecated See org.jfrog.build.client.ArtifactoryClientConfiguration#getContextUrl(). Should not be used as a
     *             top level property.
     */
    @Deprecated
    String PROP_CONTEXT_URL = ARTIFACTORY_PREFIX + "contextUrl";


    String PROP_TIMEOUT = ARTIFACTORY_PREFIX + "timeout";

    String PROP_PROXY_PREFIX = ARTIFACTORY_PREFIX + "proxy.";

    /**
     * The repo key in Artifactory from where to resolve artifacts.
     */
    String PROP_RESOLVE_PREFIX = ARTIFACTORY_PREFIX + "resolve.";

    /**
     * The repo key in Artifactory to where to publish release artifacts.
     */
    String PROP_PUBLISH_PREFIX = ARTIFACTORY_PREFIX + "publish.";

    /**
     * Property for whether to publish the artifacts even if the build is unstable
     */
    String PROP_PUBLISH_EVEN_UNSTABLE = ARTIFACTORY_PREFIX + "publish.unstable";


    /**
     * Prefix for properties that are dynamically added to deployment (as matrix params)
     */
    String PROP_DEPLOY_PARAM_PROP_PREFIX = ARTIFACTORY_PREFIX + "deploy.";
}