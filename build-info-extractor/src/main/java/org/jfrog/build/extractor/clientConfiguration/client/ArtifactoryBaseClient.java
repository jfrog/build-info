package org.jfrog.build.extractor.clientConfiguration.client;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ArtifactoryHttpClient;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.client.ProxyConfiguration;

import java.io.IOException;

/**
 * Created by Tamirh on 21/04/2016.
 */
public abstract class ArtifactoryBaseClient implements AutoCloseable {
    private static final String API_REPOSITORIES = "/api/repositories";

    protected String artifactoryUrl;
    protected ArtifactoryHttpClient httpClient;
    protected final Log log;
    /**
     * Version of Artifactory we work with.
     */
    private ArtifactoryVersion artifactoryVersion;

    public ArtifactoryBaseClient(String artifactoryUrl, String username, String password, Log logger) {
        this.artifactoryUrl = StringUtils.stripEnd(artifactoryUrl, "/");
        httpClient = new ArtifactoryHttpClient(this.artifactoryUrl, username, password, logger);
        this.log = logger;
    }

    @Override
    public void close() {
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
     * Connection Retries to perform
     *
     * @param connectionRetries The number of max retries.
     */
    public void setConnectionRetries(int connectionRetries) {
        httpClient.setConnectionRetries(connectionRetries);
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

    /**
     * Log setter for the PreemptiveHttpClient for jobs like the Jenkins Generic job that uses NullLog by default.
     * @param log Log instance
     */
    public void setLog(Log log) {
        httpClient.getHttpClient().setLog(log);
    }

    public String getArtifactoryUrl() {
        return artifactoryUrl;
    }

    public ArtifactoryVersion getArtifactoryVersion() {
        if (artifactoryVersion == null) {
            try {
                artifactoryVersion = httpClient.getVersion();
            } catch (IOException e) {
                artifactoryVersion = ArtifactoryVersion.NOT_FOUND;
            }
        }
        return artifactoryVersion;
    }

    public boolean isRepoExist(String repo) {
        String fullItemUrl = artifactoryUrl + API_REPOSITORIES + "/" + repo;
        String encodedUrl = ArtifactoryHttpClient.encodeUrl(fullItemUrl);
        HttpRequestBase httpRequest = new HttpGet(encodedUrl);
        HttpResponse httpResponse = null;
        int connectionRetries = httpClient.getConnectionRetries();
        try {
            httpResponse = httpClient.getHttpClient().execute(httpRequest);
            StatusLine statusLine = httpResponse.getStatusLine();
            if (statusLine.getStatusCode() == HttpStatus.SC_BAD_REQUEST) {
                return false;
            }
        } catch (IOException e) {
            return false;
        } finally {
            // We are using the same client for multiple operations therefore we need to restore the connectionRetries configuration.
            httpClient.setConnectionRetries(connectionRetries);
            if (httpResponse != null) {
                EntityUtils.consumeQuietly(httpResponse.getEntity());
            }
        }
        return true;
    }
}
