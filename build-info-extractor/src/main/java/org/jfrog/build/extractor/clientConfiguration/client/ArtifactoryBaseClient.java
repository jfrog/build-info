package org.jfrog.build.extractor.clientConfiguration.client;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ArtifactoryHttpClient;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.clientConfiguration.util.JsonSerializer;
import org.jfrog.build.extractor.usageReport.UsageReporter;
import org.jfrog.build.util.VersionCompatibilityType;
import org.jfrog.build.util.VersionException;

import java.io.IOException;

/**
 * Created by Tamirh on 21/04/2016.
 */
public abstract class ArtifactoryBaseClient implements AutoCloseable {
    private static final String API_REPOSITORIES = "/api/repositories";
    private static final String USAGE_API = "/api/system/usage";
    private static final ArtifactoryVersion USAGE_ARTIFACTORY_MIN_VERSION = new ArtifactoryVersion("6.9.0");

    protected String artifactoryUrl;
    protected ArtifactoryHttpClient httpClient;
    protected final Log log;
    /**
     * Version of Artifactory we work with.
     */
    private ArtifactoryVersion artifactoryVersion;

    public ArtifactoryBaseClient(String artifactoryUrl, String username, String password, String accessToken, Log logger) {
        this.artifactoryUrl = StringUtils.stripEnd(artifactoryUrl, "/");
        if (StringUtils.isNotEmpty(accessToken)) {
            httpClient = new ArtifactoryHttpClient(this.artifactoryUrl, accessToken, logger);
        } else {
            httpClient = new ArtifactoryHttpClient(this.artifactoryUrl, username, password, logger);
        }
        this.log = logger;
    }

    public ArtifactoryBaseClient(String artifactoryUrl, ArtifactoryHttpClient httpClient, Log logger) {
        this.artifactoryUrl = StringUtils.stripEnd(artifactoryUrl, "/");
        this.httpClient = httpClient;
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

    public ProxyConfiguration getProxyConfiguration() {
        return httpClient.getProxyConfiguration();
    }

    /**
     * Log setter for the PreemptiveHttpClient for jobs like the Jenkins Generic job that uses NullLog by default.
     *
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

    public boolean isRepoExist(String repo) throws IOException {
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
        } finally {
            // We are using the same client for multiple operations therefore we need to restore the connectionRetries configuration.
            httpClient.setConnectionRetries(connectionRetries);
            if (httpResponse != null) {
                EntityUtils.consumeQuietly(httpResponse.getEntity());
            }
        }
        return true;
    }

    public void reportUsage(UsageReporter usageReporter) throws VersionException, IOException {
        // Check Artifactory supported version.
        ArtifactoryVersion version = getArtifactoryVersion();
        if (version.isNotFound()) {
            throw new VersionException("Could not get Artifactory version.", VersionCompatibilityType.NOT_FOUND);
        }
        if (!version.isAtLeast(USAGE_ARTIFACTORY_MIN_VERSION)) {
            String message = String.format("Expected Artifactory version %s or above for usage report, got %s.", USAGE_ARTIFACTORY_MIN_VERSION.toString(), version.toString());
            throw new VersionException(message, VersionCompatibilityType.INCOMPATIBLE);
        }

        // Build Artifactory URL.
        String fullItemUrl = artifactoryUrl + USAGE_API;
        String encodedUrl = ArtifactoryHttpClient.encodeUrl(fullItemUrl);

        // Create request.
        String requestBody = new JsonSerializer<UsageReporter>().toJSON(usageReporter);
        StringEntity entity = new StringEntity(requestBody, "UTF-8");
        entity.setContentType("application/json");
        HttpPost request = new HttpPost(encodedUrl);
        request.setEntity(entity);

        // Send request
        int connectionRetries = httpClient.getConnectionRetries();
        HttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.getHttpClient().execute(request);
            StatusLine statusLine = httpResponse.getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException(String.format("Artifactory response: %s %s", statusLine.getStatusCode(), statusLine.getReasonPhrase()));
            }
        } finally {
            // We are using the same client for multiple operations therefore we need to restore the connectionRetries configuration.
            httpClient.setConnectionRetries(connectionRetries);
            if (httpResponse != null) {
                EntityUtils.consumeQuietly(httpResponse.getEntity());
            }
        }
    }
}
