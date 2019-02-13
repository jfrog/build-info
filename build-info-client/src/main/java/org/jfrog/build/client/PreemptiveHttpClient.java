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
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.*;
import org.apache.http.protocol.HttpContext;
import org.jfrog.build.api.util.CommonUtils;
import org.jfrog.build.api.util.Log;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
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
    private final boolean requestSentRetryEnabled = true;
    private CloseableHttpClient httpClient;
    private HttpClientContext localContext = HttpClientContext.create();
    private BasicCredentialsProvider basicCredentialsProvider = new BasicCredentialsProvider();
    private int connectionRetries;
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

    public PreemptiveHttpClient(String userName, String password, int timeout, ProxyConfiguration proxyConfiguration, int connectionRetries) {
        HttpClientBuilder httpClientBuilder = createHttpClientBuilder(userName, password, timeout, connectionRetries);
        if (proxyConfiguration != null) {
            setProxyConfiguration(httpClientBuilder, proxyConfiguration);
        }
        httpClient = httpClientBuilder.build();
    }

    private void setProxyConfiguration(HttpClientBuilder httpClientBuilder, ProxyConfiguration proxyConfiguration) {
        HttpHost proxy = new HttpHost(proxyConfiguration.host, proxyConfiguration.port);
        httpClientBuilder.setProxy(proxy);

        if (proxyConfiguration.username != null) {
            basicCredentialsProvider.setCredentials(new AuthScope(proxyConfiguration.host, proxyConfiguration.port),
                    new UsernamePasswordCredentials(proxyConfiguration.username, proxyConfiguration.password));
            localContext.setCredentialsProvider(basicCredentialsProvider);
            // Create AuthCache instance
            AuthCache authCache = new BasicAuthCache();
            // Generate BASIC scheme object and add it to the local auth cache
            BasicScheme basicAuth = new BasicScheme();
            authCache.put(proxy, basicAuth);
            localContext.setAuthCache(authCache);
        }
    }

    public HttpResponse execute(HttpUriRequest request) throws IOException {
        if (localContext != null) {
            return httpClient.execute(request, localContext);
        } else {
            return httpClient.execute(request);
        }
    }

    private HttpClientBuilder createHttpClientBuilder(String userName, String password, int timeout, int connectionRetries) {
        this.connectionRetries = connectionRetries;
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

        if (StringUtils.isEmpty(userName)) {
            userName = "anonymous";
            password = "";
        }

        basicCredentialsProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                new UsernamePasswordCredentials(userName, password));
        localContext.setCredentialsProvider(basicCredentialsProvider);

        // Add as the first request interceptor
        builder.addInterceptorFirst(new PreemptiveAuth());

        int retryCount = connectionRetries < 0 ? ArtifactoryHttpClient.DEFAULT_CONNECTION_RETRY : connectionRetries;
        builder.setRetryHandler(new PreemptiveRetryHandler(retryCount));
        builder.setServiceUnavailableRetryStrategy(new PreemptiveRetryStrategy());
        builder.setRedirectStrategy(new PreemptiveRedirectStrategy());

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
            HttpClientContext finalContext = (HttpClientContext) context;
            AuthState authState = finalContext.getTargetAuthState();
            // If no auth scheme available yet, try to initialize it preemptively
            if (authState.getAuthScheme() == null) {
                CredentialsProvider credsProvider = finalContext.getCredentialsProvider();
                HttpHost targetHost = finalContext.getTargetHost();
                Credentials creds = credsProvider.getCredentials(
                        new AuthScope(targetHost.getHostName(), targetHost.getPort()));
                if (creds == null) {
                    throw new HttpException("No credentials for preemptive authentication");
                }
                BasicScheme authScheme = new BasicScheme();
                authState.update(authScheme, creds);
            }
        }
    }

    /**
     * Class to handle retries when 5xx errors occurs.
     */

    private class PreemptiveRetryStrategy implements ServiceUnavailableRetryStrategy {

        @Override
        public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
            // Code 500 means an unexpected behavior of Artifactory, thus we should not retry.
            if (response.getStatusLine().getStatusCode() > 500) {
                HttpClientContext clientContext = HttpClientContext.adapt(context);
                log.warn("Error occurred for request " + clientContext.getRequest().getRequestLine().toString() +
                        ". Received status code " + response.getStatusLine().getStatusCode() +
                        " and message: " + response.getStatusLine().getReasonPhrase() + ".");
                if (executionCount <= connectionRetries) {
                    log.warn("Attempting retry #" + executionCount);
                    return true;
                }
            }

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

        PreemptiveRetryHandler(int connectionRetries) {
            super(connectionRetries, requestSentRetryEnabled, getNonRetriableClasses());
        }

        @Override
        public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
            HttpClientContext clientContext = HttpClientContext.adapt(context);
            log.warn("Error occurred for request " + clientContext.getRequest().getRequestLine().toString() + ": " + exception.getMessage() + ".");
            if (executionCount > connectionRetries) {
                return false;
            }
            boolean shouldRetry = super.retryRequest(exception, executionCount, context);
            if (shouldRetry) {
                log.warn("Attempting retry #" + executionCount);
                return true;
            }

            return false;
        }
    }

    /**
     * Class for performing redirection for the following status codes:
     * SC_MOVED_PERMANENTLY (301)
     * SC_MOVED_TEMPORARILY (302)
     * SC_SEE_OTHER (303)
     * SC_TEMPORARY_REDIRECT (307)
     */

    private class PreemptiveRedirectStrategy extends DefaultRedirectStrategy {

        private Set<String> redirectableMethods = CommonUtils.newHashSet(
                HttpGet.METHOD_NAME.toLowerCase(),
                HttpPost.METHOD_NAME.toLowerCase(),
                HttpHead.METHOD_NAME.toLowerCase(),
                HttpDelete.METHOD_NAME.toLowerCase(),
                HttpPut.METHOD_NAME.toLowerCase());

        @Override
        public HttpUriRequest getRedirect(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException {
            URI uri = getLocationURI(request, response, context);
            log.debug("Redirecting to " + uri);
            return RequestBuilder.copy(request).setUri(uri).build();
        }

        @Override
        protected boolean isRedirectable(String method) {
            String message = "The method " + method;
            if (redirectableMethods.contains(method.toLowerCase())) {
                log.debug(message + " can be redirected.");
                return true;
            }
            log.error(message + " cannot be redirected.");
            return false;
        }
    }
}

