package org.jfrog.build.extractor.clientConfiguration;

/**
 * @author Tomer Cohen
 */
public interface ClientProperties {

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
     * The URL of the artifactory web application (typically ending with '/artifactory')
     *
     * @deprecated See org.jfrog.build.extractor.build.ArtifactoryClientConfiguration#getContextUrl(). Should not be used as a
     * top level property.
     */
    @Deprecated
    String ARTIFACTORY_PREFIX = "artifactory.";
    String DEPRECATED_PROP_DEPLOY_PARAM_PROP_PREFIX = ARTIFACTORY_PREFIX + "deploy.";

    /**
     * Property for whether to use relaxed ssl check and ignore issues with server certificate
     */
    String PROP_INSECURE_TLS = "insecureTls";
}