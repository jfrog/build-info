package org.jfrog.build.client;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.protocol.HttpContext;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

/**
 * @author Lior Hasson
 */
public class HttpClientConfigurator {
    private final static String CLIENT_VERSION;
    private HttpClientBuilder builder = HttpClients.custom();
    private RequestConfig.Builder config = RequestConfig.custom();
    private String baseUrl;
    private BasicCredentialsProvider credsProvider;

    static {
        // initialize client version
        Properties properties = new Properties();
        InputStream is = HttpClientConfigurator.class.getResourceAsStream("/bi.client.properties");
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

    public HttpClientConfigurator() {
        String userAgent = "ArtifactoryBuildClient/" + CLIENT_VERSION;
        builder.setUserAgent(userAgent);

        /**
         Support system properties setup
         */
        builder.useSystemProperties();
        credsProvider = new BasicCredentialsProvider();
    }

    public HttpClientConfigurator defaultMaxConnectionsPerRoute(int maxConnectionsPerRoute) {
        builder.setMaxConnPerRoute(maxConnectionsPerRoute);
        return this;
    }

    public HttpClientConfigurator maxTotalConnections(int maxTotalConnections) {
        builder.setMaxConnTotal(maxTotalConnections);
        return this;
    }

    public HttpClientConfigurator connectionTimeout(int connectionTimeout) {
        config.setConnectTimeout(connectionTimeout);
        return this;
    }

    public HttpClientConfigurator soTimeout(int soTimeout) {
        config.setSocketTimeout(soTimeout);
        return this;
    }

    public CloseableHttpClient getClient() {
        if (hasCredentials()) {
            builder.setDefaultCredentialsProvider(credsProvider);
        }
        return builder.setDefaultRequestConfig(config.build()).build();
    }

    /**
     * May throw a runtime exception when the given URL is invalid.
     */
    public HttpClientConfigurator hostFromUrl(String urlStr) throws IllegalArgumentException {
        if (StringUtils.isNotBlank(urlStr)) {
            try {
                URL url = new URL(urlStr);
                host(url.getHost());
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Cannot parse the url " + urlStr, e);
            }
        }
        return this;
    }

    /**
     * Ignores blank getValues
     */
    public HttpClientConfigurator host(String host) {
        if (StringUtils.isNotBlank(host)) {
            this.baseUrl = host;
            builder.setRoutePlanner(new DefaultHostRoutePlanner(host));
        }
        return this;
    }

    private boolean hasCredentials() {
        return credsProvider.getCredentials(AuthScope.ANY) != null;
    }

    /**
     * Ignores blank username input
     */
    public HttpClientConfigurator authentication(String username, String password) {
        if (StringUtils.isNotBlank(username)) {
            if (StringUtils.isBlank(baseUrl)) {
                throw new IllegalStateException("Cannot configure authentication when host is not set.");
            }

            credsProvider.setCredentials(
                    new AuthScope(baseUrl, AuthScope.ANY_PORT, AuthScope.ANY_REALM),
                    new UsernamePasswordCredentials(username, password));

            builder.addInterceptorFirst(new PreemptiveAuthInterceptor());
        }

        return this;
    }

    public HttpClientConfigurator proxy(@Nullable ProxyConfiguration proxyConfiguration) {
        configureProxy(proxyConfiguration);
        return this;
    }

    private void configureProxy(ProxyConfiguration proxy) {
        if (proxy != null) {
            config.setProxy(new HttpHost(proxy.host, proxy.port));
            if (proxy.username != null) {
                Credentials creds = new UsernamePasswordCredentials(proxy.username, proxy.password);
                //TODO NTLM?
                credsProvider.setCredentials(new AuthScope(proxy.host, proxy.port, AuthScope.ANY_REALM), creds);
            }
        }
    }

    static class PreemptiveAuthInterceptor implements HttpRequestInterceptor {
        @Override
        public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
            HttpClientContext clientContext = HttpClientContext.adapt(context);
            AuthState authState = clientContext.getTargetAuthState();

            // If there's no auth scheme available yet, try to initialize it preemptively
            if (authState.getAuthScheme() == null) {
                CredentialsProvider credsProvider = clientContext.getCredentialsProvider();
                HttpHost targetHost = clientContext.getTargetHost();
                Credentials creds = credsProvider.getCredentials(
                        new AuthScope(targetHost.getHostName(), targetHost.getPort()));
                if (creds == null) {
                    throw new HttpException("No credentials for preemptive authentication");
                }
                authState.update(new BasicScheme(), creds);
            }
        }
    }

    static class DefaultHostRoutePlanner extends DefaultRoutePlanner {
        private final HttpHost defaultHost;

        public DefaultHostRoutePlanner(String defaultHost) {
            super(DefaultSchemePortResolver.INSTANCE);
            this.defaultHost = new HttpHost(defaultHost);
        }

        @Override
        public HttpRoute determineRoute(HttpHost host, HttpRequest request, HttpContext context) throws HttpException {
            if (host == null) {
                host = defaultHost;
            }

            return super.determineRoute(host, request, context);
        }

        public HttpHost getDefaultHost() {
            return defaultHost;
        }
    }
}
