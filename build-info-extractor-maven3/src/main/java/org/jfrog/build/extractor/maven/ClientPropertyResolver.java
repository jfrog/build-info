package org.jfrog.build.extractor.maven;

import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.jfrog.build.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.client.ArtifactoryClientConfiguration;
import org.jfrog.build.client.ClientConfigurationFields;

import static org.jfrog.build.client.ClientProperties.PROP_CONTEXT_URL;
import static org.jfrog.build.client.ClientProperties.PROP_TIMEOUT;

/**
 * @author Noam Y. Tenne
 */
@Component(role = ClientPropertyResolver.class)
public class ClientPropertyResolver {

    @Requirement
    private Logger logger;

    public ArtifactoryBuildInfoClient resolveProperties(ArtifactoryClientConfiguration clientConf) {
        ArtifactoryBuildInfoClient client = resolveClientProps(clientConf);
        resolveTimeout(clientConf, client);
        resolveProxy(clientConf.proxy, client);
        return client;
    }

    private ArtifactoryBuildInfoClient resolveClientProps(ArtifactoryClientConfiguration clientConf) {
        String contextUrl = clientConf.getContextUrl();
        if (StringUtils.isBlank(contextUrl)) {
            throw new IllegalArgumentException(
                    "Unable to resolve Artifactory Build Info Client properties: no context URL was found.");
        }
        logResolvedProperty(PROP_CONTEXT_URL, contextUrl);

        String username = clientConf.publisher.getUserName();
        String password = clientConf.publisher.getPassword();

        if (StringUtils.isNotBlank(username)) {
            logResolvedProperty(ClientConfigurationFields.USERNAME, username);
            return new ArtifactoryBuildInfoClient(contextUrl, username, password, new Maven3BuildInfoLogger(logger));
        } else {
            return new ArtifactoryBuildInfoClient(contextUrl, new Maven3BuildInfoLogger(logger));
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

    private void resolveProxy(ArtifactoryClientConfiguration.ProxyHandler proxyConf,
            ArtifactoryBuildInfoClient client) {
        String proxyHost = proxyConf.getHost();

        if (StringUtils.isNotBlank(proxyHost)) {
            logResolvedProperty(ClientConfigurationFields.HOST, proxyHost);
            if (proxyConf.getPort() == null) {
                return;
            }
            String proxyUsername = proxyConf.getUserName();
            if (StringUtils.isNotBlank(proxyUsername)) {
                logResolvedProperty(ClientConfigurationFields.USERNAME, proxyUsername);
                client.setProxyConfiguration(proxyHost, proxyConf.getPort(), proxyUsername,
                        proxyConf.getPassword());
            } else {
                client.setProxyConfiguration(proxyHost, proxyConf.getPort());
            }
        }
    }

    private void logResolvedProperty(String key, String value) {
        logger.debug("Artifactory Client Property Resolver: " + key + " = " + value);
    }
}
