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
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Wrapper of HttpClient that forces preemptive BASIC authentication if user credentials exist.
 *
 * @author Yossi Shaul
 */
public class PreemptiveHttpClient {

    private final static String CLIENT_VERSION;
    private CloseableHttpClient httpClient;
    private BasicHttpContext localContext;

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
        this(null, null, timeout, null);
    }

    public PreemptiveHttpClient(String userName, String password, int timeout) {
        this(userName, password, timeout, null);
    }

    public PreemptiveHttpClient(String userName, String password, int timeout, ProxyConfiguration proxyConfiguration) {
        HttpClientBuilder httpClientBuilder = createHttpClientBuilder(userName, password, timeout);
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
}
