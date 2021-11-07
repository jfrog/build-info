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
package org.jfrog.build.extractor.clientConfiguration;

/**
 * @author Tomer Cohen
 */
public interface ClientProperties {
    String ARTIFACTORY_PREFIX = "artifactory.";

    /**
     * The URL of the artifactory web application (typically ending with '/artifactory')
     *
     * @deprecated See org.jfrog.build.extractor.build.ArtifactoryClientConfiguration#getContextUrl(). Should not be used as a
     * top level property.
     */
    @Deprecated
    String PROP_CONTEXT_URL = "contextUrl";

    String PROP_CONNECTION_RETRIES = "connectionRetries";

    String PROP_TIMEOUT = "timeout";

    String PROP_SO_TIMEOUT = "timeout.socket";

    String PROP_MAX_CO_PER_ROUTE = "maxConPerRoute";

    String PROP_MAX_TOTAL_CO = "maxTotalCon";

    String PROP_PROXY_PREFIX = "proxy.";

    String PROP_PACKAGE_MANAGER_PREFIX = "package.manager.";

    String PROP_NPM_PREFIX = "npm.";

    String PROP_PIP_PREFIX = "pip.";

    String PROP_DOTNET_PREFIX = "dotnet.";

    String PROP_DOCKER_PREFIX = "docker.";

    String PROP_KANIKO_PREFIX = "kaniko.";

    String PROP_GO_PREFIX = "go.";

    /**
     * The repo key in Artifactory from where to resolve artifacts.
     */
    String PROP_RESOLVE_PREFIX = "resolve.";

    /**
     * The repo key in Artifactory to where to publish release artifacts.
     */
    String PROP_PUBLISH_PREFIX = "publish.";

    /**
     * Property for whether to publish the artifacts even if the build is unstable
     */
    String PROP_PUBLISH_EVEN_UNSTABLE = "publish.unstable";


    /**
     * Prefix for properties that are dynamically added to deployment (as matrix params)
     */
    String PROP_DEPLOY_PARAM_PROP_PREFIX = "deploy.";

    /**
     * Property for whether to use relaxed ssl check and ignore issues with server certificate
     */
    String PROP_INSECURE_TLS = "insecureTls";
}