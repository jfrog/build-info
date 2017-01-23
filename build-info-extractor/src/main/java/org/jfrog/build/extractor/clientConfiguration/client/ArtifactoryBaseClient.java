package org.jfrog.build.extractor.clientConfiguration.client;

import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ArtifactoryHttpClient;
import org.jfrog.build.client.ProxyConfiguration;

import java.io.IOException;

/**
 * Created by Tamirh on 21/04/2016.
 */
public abstract class ArtifactoryBaseClient {

    protected String artifactoryUrl;
    protected ArtifactoryHttpClient httpClient;
    protected final Log log;

    public ArtifactoryBaseClient(String artifactoryUrl, String username, String password, Log logger) {
        this.artifactoryUrl = StringUtils.stripEnd(artifactoryUrl, "/");
        httpClient = new ArtifactoryHttpClient(this.artifactoryUrl, username, password, logger);
        this.log = logger;
    }

    public void close() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
    }

    /**
     * Network timeout in seconds to use both for connection establishment and for unanswered requests.
     *
     * @param connectionTimeout Timeout in seconds.
     */
    public void setConnectionTimeout(int connectionTimeout) {
        httpClient.setConnectionTimeout(connectionTimeout);
    }

    /**
     * Sets the proxy host and port.
     *
     * @param host Proxy host
     * @param port Proxy port
     */
    public void setProxyConfiguration(String host, int port) {
        httpClient.setProxyConfiguration(host, port, null, null);
    }

    /**
     * Sets the proxy details.
     *
     * @param host     Proxy host
     * @param port     Proxy port
     * @param username Username to authenticate with the proxy
     * @param password Password to authenticate with the proxy
     */
    public void setProxyConfiguration(String host, int port, String username, String password) {
        httpClient.setProxyConfiguration(host, port, username, password);
    }

    /**
     * Sets full proxy details.
     *
     * @param proxy Proxy instance {@link org.jfrog.build.client.ProxyConfiguration}
     */
    public void setProxyConfiguration(ProxyConfiguration proxy) {
        httpClient.setProxyConfiguration(proxy.host, proxy.port, proxy.username, proxy.password);
    }
}
