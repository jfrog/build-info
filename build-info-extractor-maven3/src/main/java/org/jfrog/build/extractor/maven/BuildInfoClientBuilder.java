package org.jfrog.build.extractor.maven;

import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields;

import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.*;

/**
 * Simple class to build {@link org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient} for deployment.
 *
 * @author Noam Y. Tenne
 */
@Component(role = BuildInfoClientBuilder.class)
public class BuildInfoClientBuilder {

    @Requirement
    private Logger logger;

    public ArtifactoryBuildInfoClient resolveProperties(ArtifactoryClientConfiguration clientConf) {
        ArtifactoryBuildInfoClient client = resolveClientProps(clientConf);
        resolveProxy(clientConf.proxy, client);
        resolveTimeout(clientConf, client);
        resolveSocketTimeout(clientConf, client);
        resolveMaxTotalConnection(clientConf, client);
        resolveMaxConnectionPerRoute(clientConf, client);

        return client;
    }

    private ArtifactoryBuildInfoClient resolveClientProps(ArtifactoryClientConfiguration clientConf) {
        String contextUrl = clientConf.publisher.getContextUrl();
        if (StringUtils.isBlank(contextUrl)) {
            throw new IllegalArgumentException(
                    "Unable to resolve Artifactory Build Info Client properties: no context URL was found.");
        }
        logResolvedProperty(clientConf.publisher.getPrefix() + "." + ClientConfigurationFields.CONTEXT_URL, contextUrl);

        String username = clientConf.publisher.getUsername();
        String password = clientConf.publisher.getPassword();

        if (StringUtils.isNotBlank(username)) {
            logResolvedProperty(ClientConfigurationFields.USERNAME, username);
            return new ArtifactoryBuildInfoClient(contextUrl, username, password, new Maven3BuildInfoLogger(logger));
        } else {
            return new ArtifactoryBuildInfoClient(contextUrl, new Maven3BuildInfoLogger(logger));
        }
    }

    private void resolveProxy(ArtifactoryClientConfiguration.ProxyHandler proxyConf,
                              ArtifactoryBuildInfoClient client) {
        String proxyHost = proxyConf.getHost();

        if (StringUtils.isNotBlank(proxyHost)) {
            logResolvedProperty(ClientConfigurationFields.HOST, proxyHost);
            if (proxyConf.getPort() == null) {
                return;
            }
            String proxyUsername = proxyConf.getUsername();
            if (StringUtils.isNotBlank(proxyUsername)) {
                logResolvedProperty(ClientConfigurationFields.USERNAME, proxyUsername);
                client.setProxyConfiguration(proxyHost, proxyConf.getPort(), proxyUsername,
                        proxyConf.getPassword());
            } else {
                client.setProxyConfiguration(proxyHost, proxyConf.getPort());
            }
        }
    }

    private void resolveTimeout(ArtifactoryClientConfiguration clientConf, ArtifactoryBuildInfoClient client) {
        if (clientConf.getTimeout() == null) {
            return;
        }
        String timeout = clientConf.getTimeout().toString();
        if (StringUtils.isNotBlank(timeout)) {
            logResolvedProperty(PROP_TIMEOUT, timeout);
            if (!StringUtils.isNumeric(timeout)) {
                logger.debug("Unable to resolve Artifactory Build Info Client timeout: value is non-numeric.");
                return;
            }
            client.setConnectionTimeout(Integer.valueOf(timeout));
        }
    }

    private void resolveSocketTimeout(ArtifactoryClientConfiguration clientConf, ArtifactoryBuildInfoClient client) {
        if (clientConf.getSocketTimeout() == null) {
            return;
        }
        String socketTimeout = clientConf.getSocketTimeout().toString();
        if (StringUtils.isNotBlank(socketTimeout)) {
            logResolvedProperty(PROP_SO_TIMEOUT, socketTimeout);
            if (!StringUtils.isNumeric(socketTimeout)) {
                logger.debug("Unable to resolve Artifactory Build Info Client socketTimeout: value is non-numeric.");
                return;
            }
            client.setSocketTimeout(Integer.valueOf(socketTimeout));
        }
    }

    private void resolveMaxTotalConnection(ArtifactoryClientConfiguration clientConf, ArtifactoryBuildInfoClient client) {
        if (clientConf.getMaxTotalConnection() == null) {
            return;
        }
        String maxTotalConnection = clientConf.getMaxTotalConnection().toString();
        if (StringUtils.isNotBlank(maxTotalConnection)) {
            logResolvedProperty(PROP_MAX_TOTAL_CO, maxTotalConnection);
            if (!StringUtils.isNumeric(maxTotalConnection)) {
                logger.debug("Unable to resolve Artifactory Build Info Client socketTimeout: value is non-numeric.");
                return;
            }
            client.setMaxTotalConnection(Integer.valueOf(maxTotalConnection));
        }
    }

    private void resolveMaxConnectionPerRoute(ArtifactoryClientConfiguration clientConf, ArtifactoryBuildInfoClient client) {
        if (clientConf.getMaxConnectionPerRoute() == null) {
            return;
        }
        String maxConnectionPerRoute = clientConf.getMaxConnectionPerRoute().toString();
        if (StringUtils.isNotBlank(maxConnectionPerRoute)) {
            logResolvedProperty(PROP_MAX_CO_PER_ROUTE, maxConnectionPerRoute);
            if (!StringUtils.isNumeric(maxConnectionPerRoute)) {
                logger.debug("Unable to resolve Artifactory Build Info Client socketTimeout: value is non-numeric.");
                return;
            }
            client.setMaxConnectionsPerRoute(Integer.valueOf(maxConnectionPerRoute));
        }
    }

    private void logResolvedProperty(String key, String value) {
        logger.debug("Artifactory Client Property Resolver: " + key + " = " + value);
    }
}
