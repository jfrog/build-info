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

import org.apache.http.*;
import org.apache.http.auth.*;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.jfrog.build.api.util.Log;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Wrapper of HttpClient that forces preemptive BASIC authentication if user credentials exist.
 *
 * @author Yossi Shaul
 */
public class PreemptiveHttpClient {

    private final static String CLIENT_VERSION;
    private final static String REQUEST_SENT_RETRY_ENABLED = "requestSentRetryEnabled";
    private CloseableHttpClient httpClient;
    private BasicHttpContext localContext;
    private boolean retryRequestsAlreadySent = false;
    private int maxRetries;
    private int retryCounter;
    private Log log;

    static {
        // initialize client version
        Properties properties = new Properties();
        InputStream is = PreemptiveHttpClient.class.getResourceAsStream("/bi.client.properties");
        if (is != null) {
            try {
                properties.load(is);
                is.close();
            } catch (IOException e) {
                // ignore, use the default value
            }
        }
        CLIENT_VERSION = properties.getProperty("client.version", "unknown");
    }

    public PreemptiveHttpClient(int timeout) {
        this(null, null, timeout, null, ArtifactoryHttpClient.DEFAULT_MAX_RETRY, false);
    }

    public PreemptiveHttpClient(String userName, String password, int timeout) {
        this(userName, password, timeout, null, ArtifactoryHttpClient.DEFAULT_MAX_RETRY, false);
    }

    public PreemptiveHttpClient(String userName, String password, int timeout, ProxyConfiguration proxyConfiguration, int maxRetries, boolean retryRequestsAlreadySent) {
        this.retryRequestsAlreadySent = retryRequestsAlreadySent;
        HttpClientBuilder httpClientBuilder = createHttpClientBuilder(userName, password, timeout, maxRetries);
        if (proxyConfiguration != null) {
            setProxyConfiguration(httpClientBuilder, proxyConfiguration);
        }
        httpClient = httpClientBuilder.build();
    }

    private void setProxyConfiguration(HttpClientBuilder httpClientBuilder, ProxyConfiguration proxyConfiguration) {
        HttpHost proxy = new HttpHost(proxyConfiguration.host, proxyConfiguration.port);
        httpClientBuilder.setProxy(proxy);

        if (proxyConfiguration.username != null) {
            BasicCredentialsProvider basicCredentialsProvider = new BasicCredentialsProvider();
            basicCredentialsProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                    new UsernamePasswordCredentials(proxyConfiguration.username, proxyConfiguration.password));
            httpClientBuilder.setDefaultCredentialsProvider(basicCredentialsProvider);
        }
    }

    public HttpResponse execute(HttpUriRequest request) throws IOException {
        if (localContext != null) {
            return httpClient.execute(request, localContext);
        } else {
            return httpClient.execute(request);
        }
    }

    private HttpClientBuilder createHttpClientBuilder(String userName, String password, int timeout) {
        return createHttpClientBuilder(userName, password, timeout, ArtifactoryHttpClient.DEFAULT_MAX_RETRY);
    }

    private HttpClientBuilder createHttpClientBuilder(String userName, String password, int timeout, int maxRetries) {
        this.maxRetries = maxRetries;
        int timeoutMilliSeconds = timeout * 1000;
        RequestConfig requestConfig = RequestConfig
                .custom()
                .setSocketTimeout(timeoutMilliSeconds)
                .setConnectTimeout(timeoutMilliSeconds)
                .setCircularRedirectsAllowed(true)
                .build();

        HttpClientBuilder builder = HttpClientBuilder
                .create()
                .setDefaultRequestConfig(requestConfig);

        if (userName != null && !"".equals(userName)) {
            BasicCredentialsProvider basicCredentialsProvider = new BasicCredentialsProvider();
            basicCredentialsProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                    new UsernamePasswordCredentials(userName, password));

            builder.setDefaultCredentialsProvider(basicCredentialsProvider);
            localContext = new BasicHttpContext();

            // Generate BASIC scheme object and stick it to the local execution context
            BasicScheme basicAuth = new BasicScheme();
            localContext.setAttribute("preemptive-auth", basicAuth);

            // Add as the first request interceptor
            builder.addInterceptorFirst(new PreemptiveAuth());
        }
        boolean retryRequestsAlreadySent = Boolean.parseBoolean(System.getProperty(REQUEST_SENT_RETRY_ENABLED));
        if (retryRequestsAlreadySent || this.retryRequestsAlreadySent) {
            retryRequestsAlreadySent = true;
        }

        int retryCount = maxRetries < 1 ? ArtifactoryHttpClient.DEFAULT_MAX_RETRY : maxRetries;
        builder.setRetryHandler(new PreemptiveRetryHandler(retryCount, retryRequestsAlreadySent));
        builder.setServiceUnavailableRetryStrategy(new PreemptiveRetryStrategy());

        // set the following user agent with each request
        String userAgent = "ArtifactoryBuildClient/" + CLIENT_VERSION;
        builder.setUserAgent(userAgent);
        return builder;
    }

    public void close() {
        try {
            httpClient.close();
        } catch (IOException e) {
            // Do nothing
        }
    }

    public void setLog(Log log) {
        this.log = log;
    }

    /**
     * Sets the Exceptions that would not be retried if those exceptions are thrown.
     *
     * @return set of Exceptions that will not be retired
     */
    private Set<Class<? extends IOException>> getNonRetriableClasses() {
        Set<Class<? extends IOException>> classSet = new HashSet<Class<? extends IOException>>();
        classSet.add(SSLException.class);
        return classSet;
    }

    static class PreemptiveAuth implements HttpRequestInterceptor {
        public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {

            AuthState authState = (AuthState) context.getAttribute(HttpClientContext.TARGET_AUTH_STATE);

            // If no auth scheme available yet, try to initialize it preemptively
            if (authState.getAuthScheme() == null) {
                AuthScheme authScheme = (AuthScheme) context.getAttribute("preemptive-auth");
                CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(
                        HttpClientContext.CREDS_PROVIDER);
                HttpHost targetHost = (HttpHost) context.getAttribute(HttpCoreContext.HTTP_TARGET_HOST);
                if (authScheme != null) {
                    Credentials creds = credsProvider.getCredentials(
                            new AuthScope(targetHost.getHostName(), targetHost.getPort()));
                    if (creds == null) {
                        throw new HttpException("No credentials for preemptive authentication");
                    }
                    authState.update(authScheme, creds);
                }
            }
        }
    }

    /**
     * Class to handle retries when 5xx errors occurs.
     */

    private class PreemptiveRetryStrategy implements ServiceUnavailableRetryStrategy {

        @Override
        public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {

            retryCounter++;
            if (response.getStatusLine().getStatusCode() > 500) {
                HttpClientContext clientContext = HttpClientContext.adapt(context);
                log.warn("Error occurred for request " + clientContext.getRequest().getRequestLine().toString());
                log.warn("Received status code " + response.getStatusLine().getStatusCode() +
                        " and message: " + response.getStatusLine().getReasonPhrase() + ".");
                if (retryCounter <= maxRetries) {
                    log.warn("Trying retry #" + retryCounter);
                    return true;
                }
            }

            retryCounter = 0;
            return false;
        }

        @Override
        public long getRetryInterval() {
            return 0;
        }
    }

    /**
     * Class to handle retries when exception occurs.
     */

    private class PreemptiveRetryHandler extends DefaultHttpRequestRetryHandler {

        PreemptiveRetryHandler(int maxRetries, boolean retryRequestsAlreadySent) {
            super(maxRetries, retryRequestsAlreadySent, getNonRetriableClasses());
        }

        @Override
        public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {

            retryCounter++;
            HttpClientContext clientContext = HttpClientContext.adapt(context);
            log.warn("Error occurred for request " + clientContext.getRequest().getRequestLine().toString() + ": " + exception.getMessage() + ".");
            if (retryCounter > maxRetries) {
                retryCounter = 0;
                return false;
            }
            boolean shouldRetry = super.retryRequest(exception, retryCounter, context);
            if (shouldRetry) {
                log.warn("Trying retry #" + retryCounter);
                return true;
            }
            if (clientContext.isRequestSent()) {
                log.info("Request previously was send. Retry is not allowed for requests that already been send.\n" +
                        "This can be modified by setting a system property: requestSentRetryEnabled to true.");
            }
            retryCounter = 0;
            return false;
        }
    }
}

