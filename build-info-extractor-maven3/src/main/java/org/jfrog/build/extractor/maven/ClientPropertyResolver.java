package org.jfrog.build.extractor.maven;

import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.jfrog.build.client.ArtifactoryBuildInfoClient;

import java.util.Properties;

import static org.jfrog.build.client.ClientProperties.*;

/**
 * @author Noam Y. Tenne
 */
@Component(role = ClientPropertyResolver.class)
public class ClientPropertyResolver extends AbstractPropertyResolver<ArtifactoryBuildInfoClient> {

    @Requirement
    private Logger logger;

    @Override
    public ArtifactoryBuildInfoClient resolveProperties(Properties clientProperties) {
        ArtifactoryBuildInfoClient client = resolveClientProps(clientProperties);
        resolveTimeout(clientProperties, client);
        resolveProxy(clientProperties, client);
        return client;
    }

    private ArtifactoryBuildInfoClient resolveClientProps(Properties clientProperties) {
        String contextUrl = clientProperties.getProperty(PROP_CONTEXT_URL);
        if (StringUtils.isBlank(contextUrl)) {
            throw new IllegalArgumentException(
                    "Unable to resolve Artifactory Build Info Client properties: no context URL was found.");
        }
        logResolvedProperty(PROP_CONTEXT_URL, contextUrl);

        String username = clientProperties.getProperty(PROP_PUBLISH_USERNAME);
        String password = clientProperties.getProperty(PROP_PUBLISH_PASSWORD);

        if (StringUtils.isNotBlank(username)) {
            logResolvedProperty(PROP_PUBLISH_USERNAME, username);
            return new ArtifactoryBuildInfoClient(contextUrl, username, password);
        } else {
            return new ArtifactoryBuildInfoClient(contextUrl);
        }
    }

    private void resolveTimeout(Properties clientProperties, ArtifactoryBuildInfoClient client) {
        String timeout = clientProperties.getProperty(PROP_TIMEOUT);
        if (StringUtils.isNotBlank(timeout)) {
            logResolvedProperty(PROP_TIMEOUT, timeout);
            if (!StringUtils.isNumeric(timeout)) {
                logger.debug("Unable to resolve Artifactory Build Info Client timeout: value is non-numeric.");
                return;
            }
            client.setConnectionTimeout(Integer.valueOf(timeout));
        }
    }

    private void resolveProxy(Properties clientProperties, ArtifactoryBuildInfoClient client) {
        String proxyHost = clientProperties.getProperty(PROP_PROXY_HOST);

        if (StringUtils.isNotBlank(proxyHost)) {
            logResolvedProperty(PROP_PROXY_HOST, proxyHost);

            String proxyPort = clientProperties.getProperty(PROP_PROXY_PORT);
            if (!StringUtils.isNumeric(proxyPort)) {
                logger.debug("Unable to resolve Artifactory Build Info Client proxy port: value is non-numeric.");
                return;
            }

            String proxyUsername = clientProperties.getProperty(PROP_PROXY_USERNAME);
            if (StringUtils.isNotBlank(proxyUsername)) {
                logResolvedProperty(PROP_PROXY_USERNAME, proxyUsername);
                client.setProxyConfiguration(proxyHost, Integer.valueOf(proxyPort), proxyUsername,
                        clientProperties.getProperty(PROP_PROXY_PASSWORD));
            } else {
                client.setProxyConfiguration(proxyHost, Integer.valueOf(proxyPort));
            }
        }
    }

    private void logResolvedProperty(String key, String value) {
        logger.debug("Artifactory Client Property Resolver: " + key + " = " + value);
    }
}
