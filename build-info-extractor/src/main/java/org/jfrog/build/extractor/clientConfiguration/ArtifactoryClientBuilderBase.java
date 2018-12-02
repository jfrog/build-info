package org.jfrog.build.extractor.clientConfiguration;

import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBaseClient;

import java.io.Serializable;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class ArtifactoryClientBuilderBase<T extends ArtifactoryClientBuilderBase<T>> implements Serializable {
    private static final long serialVersionUID = 1L;

    protected ProxyConfiguration proxyConfiguration;
    protected int connectionTimeout = -1;
    protected int connectionRetry = -1;
    protected String artifactoryUrl;
    protected String username;
    protected String password;
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

    public T setUsername(String username) {
        this.username = username;
        return self();
    }

    public T setPassword(String password) {
        this.password = password;
        return self();
    }

    public T setLog(Log log) {
        this.log = log;
        return self();
    }


    protected ArtifactoryBaseClient build(ArtifactoryBaseClient client) {
        if (proxyConfiguration != null) {
            client.setProxyConfiguration(proxyConfiguration.host,
                    proxyConfiguration.port,
                    proxyConfiguration.username,
                    proxyConfiguration.password);
        }

        if (connectionTimeout != -1) {
            client.setConnectionTimeout(connectionTimeout);
        }

        if (connectionRetry != -1) {
            client.setConnectionRetries(connectionRetry);
        }
        return client;
    }

    public abstract ArtifactoryBaseClient build();

    protected abstract T self();
}
