package org.jfrog.build.extractor.clientConfiguration;

import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;

/**
 * Created by Bar Belity on 10/10/2018.
 */
public class ArtifactoryBuildInfoClientBuilder {

    private String username;
    private String password;
    private String artifactoryUrl;
    private Log log;
    private ProxyConfiguration proxyConfiguration;
    private int connectionTimeout = -1;
    private int connectionRetry = -1;

    public ArtifactoryBuildInfoClientBuilder setUsername(String username) {
        this.username = username;
        return this;
    }

    public ArtifactoryBuildInfoClientBuilder setPassword(String password) {
        this.password = password;
        return this;
    }

    public ArtifactoryBuildInfoClientBuilder setArtifactoryUrl(String artifactoryUrl) {
        this.artifactoryUrl = artifactoryUrl;
        return this;
    }

    public ArtifactoryBuildInfoClientBuilder setLog(Log log) {
        this.log = log;
        return this;
    }

    public ArtifactoryBuildInfoClientBuilder setProxyConfiguration(ProxyConfiguration proxyConfiguration) {
        this.proxyConfiguration = proxyConfiguration;
        return this;
    }

    public ArtifactoryBuildInfoClientBuilder setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public ArtifactoryBuildInfoClientBuilder setConnectionRetry(int connectionRetry) {
        this.connectionRetry = connectionRetry;
        return this;
    }

    public ArtifactoryBuildInfoClient build() {
        ArtifactoryBuildInfoClient client = new ArtifactoryBuildInfoClient(artifactoryUrl, username, password, log);
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
}
