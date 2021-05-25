package org.jfrog.build.extractor.maven;

import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;

import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.PROP_CONNECTION_RETRIES;
import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.PROP_TIMEOUT;

/**
 * Simple class to build {@link ArtifactoryManager} for deployment.
 *
 * @author Noam Y. Tenne
 */
@Component(role = ArtifactoryManagerBuilder.class)
public class ArtifactoryManagerBuilder {

    @Requirement
    private Logger logger;

    public ArtifactoryManager resolveProperties(ArtifactoryClientConfiguration clientConf) {
        ArtifactoryManager artifactoryManager = resolveClientProps(clientConf);
        resolveTimeout(clientConf, artifactoryManager);
        resolveProxy(clientConf.proxy, artifactoryManager);
        resolveRetriesParams(clientConf, artifactoryManager);
        resolveInsecureTls(clientConf, artifactoryManager);
        return artifactoryManager;
    }

    private ArtifactoryManager resolveClientProps(ArtifactoryClientConfiguration clientConf) {
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
            return new ArtifactoryManager(contextUrl, username, password, new Maven3BuildInfoLogger(logger));
        } else {
            return new ArtifactoryManager(contextUrl, new Maven3BuildInfoLogger(logger));
        }
    }

    private void resolveTimeout(ArtifactoryClientConfiguration clientConf, ArtifactoryManager artifactoryManager) {
        if (clientConf.getTimeout() == null) {
            return;
        }
        int timeout = clientConf.getTimeout();
        logResolvedProperty(PROP_TIMEOUT, String.valueOf(timeout));
        artifactoryManager.setConnectionTimeout(timeout);
    }

    private void resolveRetriesParams(ArtifactoryClientConfiguration clientConf, ArtifactoryManager artifactoryManager) {
        if (clientConf.getConnectionRetries() == null) {
            return;
        }
        int configMaxRetries = clientConf.getConnectionRetries();
        logResolvedProperty(PROP_CONNECTION_RETRIES, String.valueOf(configMaxRetries));
        artifactoryManager.setConnectionRetries(configMaxRetries);
    }

    private void resolveInsecureTls(ArtifactoryClientConfiguration clientConf, ArtifactoryManager artifactoryManager) {
        artifactoryManager.setInsecureTls(clientConf.getInsecureTls());
    }

    private void resolveProxy(ArtifactoryClientConfiguration.ProxyHandler proxyConf,
                              ArtifactoryManager artifactoryManager) {
        String proxyHost = proxyConf.getHost();

        if (StringUtils.isNotBlank(proxyHost)) {
            logResolvedProperty(ClientConfigurationFields.HOST, proxyHost);
            if (proxyConf.getPort() == null) {
                return;
            }
            String proxyUsername = proxyConf.getUsername();
            if (StringUtils.isNotBlank(proxyUsername)) {
                logResolvedProperty(ClientConfigurationFields.USERNAME, proxyUsername);
                artifactoryManager.setProxyConfiguration(proxyHost, proxyConf.getPort(), proxyUsername,
                        proxyConf.getPassword());
            } else {
                artifactoryManager.setProxyConfiguration(proxyHost, proxyConf.getPort());
            }
        }
    }

    private void logResolvedProperty(String key, String value) {
        logger.debug("Artifactory Client Property Resolver: " + key + " = " + value);
    }
}
