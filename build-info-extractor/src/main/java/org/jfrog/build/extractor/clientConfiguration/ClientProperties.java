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
    String PROP_CONTEXT_URL = ARTIFACTORY_PREFIX + "contextUrl";

    String PROP_CONNECTION_RETRIES = ARTIFACTORY_PREFIX + "connectionRetries";

    String PROP_TIMEOUT = ARTIFACTORY_PREFIX + "timeout";

    String PROP_SO_TIMEOUT = ARTIFACTORY_PREFIX + "timeout.socket";

    String PROP_MAX_CO_PER_ROUTE = ARTIFACTORY_PREFIX + "maxConPerRoute";

    String PROP_MAX_TOTAL_CO = ARTIFACTORY_PREFIX + "maxTotalCon";

    String PROP_PROXY_PREFIX = ARTIFACTORY_PREFIX + "proxy.";

    String PROP_PACKAGE_MANAGER_PREFIX = ARTIFACTORY_PREFIX + "package.manager.";

    String PROP_NPM_PREFIX = ARTIFACTORY_PREFIX + "npm.";

    String PROP_PIP_PREFIX = ARTIFACTORY_PREFIX + "pip.";

    String PROP_DOTNET_PREFIX = ARTIFACTORY_PREFIX + "dotnet.";

    String PROP_DOCKER_PREFIX = ARTIFACTORY_PREFIX + "docker.";

    String PROP_KANIKO_PREFIX = ARTIFACTORY_PREFIX + "kaniko.";

    String PROP_GO_PREFIX = ARTIFACTORY_PREFIX + "go.";

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

    /**
     * Property for whether to use relaxed ssl check and ignore issues with server certificate
     */
    String PROP_INSECURE_TLS = ARTIFACTORY_PREFIX + "insecureTls";
}