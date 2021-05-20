package org.jfrog.build.extractor.clientConfiguration;

import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.clientConfiguration.client.ManagerBase;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;

import javax.net.ssl.SSLContext;
import java.io.Serializable;

/**
 * @author Yahav Itzhak
 */
@SuppressWarnings({"unused", "WeakerAccess", "UnusedReturnValue"})
public abstract class ManagerBuilderBase<T extends ManagerBuilderBase<T>> implements Serializable {
    private static final long serialVersionUID = 1L;

    protected ProxyConfiguration proxyConfiguration;
    protected int connectionTimeout = -1;
    protected int connectionRetry = -1;
    protected String artifactoryUrl;
    protected SSLContext sslContext;
    protected String username;
    protected String password;
    protected String accessToken;
    protected Log log;

    public T setProxyConfiguration(ProxyConfiguration proxyConfiguration) {
        this.proxyConfiguration = proxyConfiguration;
        return self();
    }

    public T setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return self();
    }

    public T setConnectionRetry(int connectionRetry) {
        this.connectionRetry = connectionRetry;
        return self();
    }

    public T setArtifactoryUrl(String artifactoryUrl) {
        this.artifactoryUrl = artifactoryUrl;
        return self();
    }

    public T setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
        return self();
    }

    public T setUsername(String username) {
        this.username = username;
        return self();
    }

    public T setPassword(String password) {
        this.password = password;
        return self();
    }

    public T setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        return self();
    }

    public T setLog(Log log) {
        this.log = log;
        return self();
    }

    /**
     * Set a complete client configuration from ArtifactoryClientConfiguration.
     *
     * @param clientConfiguration     - The client configuration
     * @param repositoryConfiguration - Deployer or resolver configuration
     * @return self
     */
    public T setClientConfiguration(ArtifactoryClientConfiguration clientConfiguration,
                                    ArtifactoryClientConfiguration.RepositoryConfiguration repositoryConfiguration) {
        setArtifactoryUrl(repositoryConfiguration.getContextUrl());
        setUsername(repositoryConfiguration.getUsername());
        setPassword(repositoryConfiguration.getPassword());
        setLog(repositoryConfiguration.getLog());

        ArtifactoryClientConfiguration.ProxyHandler proxyHandler = clientConfiguration.proxy;
        if (proxyHandler != null && StringUtils.isNotBlank(proxyHandler.getHost())) {
            ProxyConfiguration proxyConfiguration = new ProxyConfiguration();
            proxyConfiguration.host = proxyHandler.getHost();
            proxyConfiguration.port = proxyHandler.getPort();
            proxyConfiguration.username = proxyHandler.getUsername();
            proxyConfiguration.password = proxyHandler.getPassword();
            setProxyConfiguration(proxyConfiguration);
        }

        if (clientConfiguration.getConnectionRetries() != null) {
            setConnectionRetry(clientConfiguration.getConnectionRetries());
        }

        if (clientConfiguration.getTimeout() != null) {
            setConnectionTimeout(clientConfiguration.getTimeout());
        }

        return self();
    }

    protected ArtifactoryManager build(ArtifactoryManager client) {
        if (proxyConfiguration != null) {
            client.setProxyConfiguration(proxyConfiguration.host,
                    proxyConfiguration.port,
                    proxyConfiguration.username,
                    proxyConfiguration.password);
        }

        client.setSslContext(sslContext);

        if (connectionTimeout != -1) {
            client.setConnectionTimeout(connectionTimeout);
        }

        if (connectionRetry != -1) {
            client.setConnectionRetries(connectionRetry);
        }
        return client;
    }

    public abstract ManagerBase build();

    protected abstract T self();
}
