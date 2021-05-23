/*
 * Copyright (C) 2011 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.build.client;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;

/**
 * @author Noam Y. Tenne
 */
public class JFrogHttpClient implements AutoCloseable {

    public static final ArtifactoryVersion STANDALONE_BUILD_RETENTION_SUPPORTED_ARTIFACTORY_VERSION =
            new ArtifactoryVersion("5.2.1");
    private static final int DEFAULT_CONNECTION_TIMEOUT_SECS = 300;    // 5 Minutes in seconds
    public static final int DEFAULT_CONNECTION_RETRY = 3;
    private final String url;
    private final PreemptiveHttpClientBuilder clientBuilder;

    private PreemptiveHttpClient deployClient;
    private Log log;

    private JFrogHttpClient(String url, String username, String password, String accessToken, Log log) {
        this.url = StringUtils.removeEnd(url, "/");
        this.log = log;
        clientBuilder = new PreemptiveHttpClientBuilder()
                .setConnectionRetries(DEFAULT_CONNECTION_RETRY)
                .setInsecureTls(false)
                .setTimeout(DEFAULT_CONNECTION_TIMEOUT_SECS)
                .setLog(log);
        if (StringUtils.isNotEmpty(accessToken)) {
            clientBuilder.setAccessToken(accessToken);
        } else {
            clientBuilder.setUserName(username).setPassword(password);
        }
    }

    public JFrogHttpClient(String artifactoryUrl, String username, String password, Log log) {
        this(artifactoryUrl, username, password, StringUtils.EMPTY, log);
    }

    public JFrogHttpClient(String artifactoryUrl, String accessToken, Log log) {
        this(artifactoryUrl, StringUtils.EMPTY, StringUtils.EMPTY, accessToken, log);
    }

    /**
     * Sets the proxy host and port.
     *
     * @param host Proxy host
     * @param port Proxy port
     */
    public void setProxyConfiguration(String host, int port) {
        setProxyConfiguration(host, port, null, null);
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
        ProxyConfiguration proxyConfiguration = new ProxyConfiguration();
        proxyConfiguration.host = host;
        proxyConfiguration.port = port;
        proxyConfiguration.username = username;
        proxyConfiguration.password = password;
        clientBuilder.setProxyConfiguration(proxyConfiguration);
    }

    /**
     * Network timeout in seconds to use both for connection establishment and for unanswered requests.
     *
     * @param connectionTimeout Timeout in seconds.
     */
    public void setConnectionTimeout(int connectionTimeout) {
        clientBuilder.setTimeout(connectionTimeout);
    }

    public void setInsecureTls(boolean insecureTls) {
        clientBuilder.setInsecureTls(insecureTls);
    }

    public void setSslContext(SSLContext sslContext) {
        clientBuilder.setSslContext(sslContext);
    }

    public int getConnectionRetries() {
        return clientBuilder.connectionRetries;
    }

    /**
     * Max Retries to perform
     *
     * @param connectionRetries The number of max retries.
     */
    public void setConnectionRetries(int connectionRetries) {
        clientBuilder.setConnectionRetries(connectionRetries);
    }

    public ProxyConfiguration getProxyConfiguration() {
        return clientBuilder.getProxyConfiguration();
    }

    /**
     * Release all connection and cleanup resources.
     */
    @Override
    public void close() {
        if (deployClient != null) {
            deployClient.close();
        }
    }

    public PreemptiveHttpClient getHttpClient() {
        if (deployClient == null) {
            deployClient = clientBuilder.build();
        }
        return deployClient;
    }

    public CloseableHttpResponse sendRequest(HttpRequestBase request) throws IOException {
        log.debug("Base URL: " + request.getURI().toString());
        PreemptiveHttpClient client = getHttpClient();
        String url = request.getURI().toString();
        request.setURI(URI.create((this.url + "/" + StringUtils.removeStart(url, "/"))));
        return client.execute(request);
    }

    public Log getLog() {
        return log;
    }

    public void setLog(Log log) {
        this.log = log;
    }

    public String getUrl() {
        return url;
    }

}
