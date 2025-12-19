package org.jfrog.build.extractor.maven;

import org.codehaus.plexus.util.StringUtils;
import org.jfrog.build.extractor.Proxy;
import org.jfrog.build.extractor.ProxySelector;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;

import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.PROP_CONNECTION_RETRIES;
import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.PROP_TIMEOUT;

/**
 * Simple class to build {@link ArtifactoryManager} for deployment.
 *
 * @author Noam Y. Tenne
 */
@Singleton
@Named
public class ArtifactoryManagerBuilder {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public ArtifactoryManager resolveProperties(ArtifactoryClientConfiguration clientConf) {
        ArtifactoryManager artifactoryManager = resolveClientProps(clientConf);
        resolveTimeout(clientConf, artifactoryManager);
        resolveProxy(clientConf, artifactoryManager);
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

    private void resolveProxy(ArtifactoryClientConfiguration clientConf, ArtifactoryManager artifactoryManager) {
        ProxySelector proxySelector = new ProxySelector(
                clientConf.proxy.getHost(),
                clientConf.proxy.getPort(),
                clientConf.proxy.getUsername(),
                clientConf.proxy.getPassword(),
                clientConf.httpsProxy.getHost(),
                clientConf.httpsProxy.getPort(),
                clientConf.httpsProxy.getUsername(),
                clientConf.httpsProxy.getPassword(),
                clientConf.proxy.getNoProxy()
        );
        Proxy proxy = proxySelector.getProxy(clientConf.publisher.getContextUrl());
        if (proxy != null) {
            artifactoryManager.setProxyConfiguration(proxy.getHost(), proxy.getPort(), proxy.getUsername(), proxy.getPassword());
        }
    }

    private void logResolvedProperty(String key, String value) {
        logger.debug("Artifactory Client Property Resolver: " + key + " = " + value);
    }
}
