package org.jfrog.build.client;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.jfrog.build.api.util.CommonUtils;
import org.jfrog.build.api.util.Log;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

/**
 * Wrapper of HttpClient that forces preemptive BASIC authentication if user credentials exist.
 *
 * @author Yossi Shaul
 */
public class PreemptiveHttpClient implements AutoCloseable {

    private static final boolean REQUEST_SENT_RETRY_ENABLED = true;
    /**
     * Used for storing the original host name, before a redirect to a new URL, on the request context.
     */
    private static final String ORIGINAL_HOST_CONTEXT_PARAM = "original.host.context.param";
    BasicCredentialsProvider basicCredentialsProvider;
    private final PoolingHttpClientConnectionManager connectionManager;
    private final String accessToken;
    private final AuthCache authCache;
    private final CloseableHttpClient httpClient;
    private final int connectionRetries;
    private Log log;

    public PreemptiveHttpClient(PoolingHttpClientConnectionManager connectionManager, BasicCredentialsProvider credentialsProvider, String accessToken, AuthCache authCache, HttpClientBuilder clientBuilder, int connectionRetries, Log log) {
        this.connectionManager = connectionManager;
        this.basicCredentialsProvider = credentialsProvider;
        this.accessToken = accessToken;
        this.authCache = authCache;
        this.connectionRetries = connectionRetries;
        this.log = log;

        int retryCount = connectionRetries < 0 ? JFrogHttpClient.DEFAULT_CONNECTION_RETRY : connectionRetries;
        clientBuilder.setRetryHandler(new PreemptiveHttpClient.PreemptiveRetryHandler(retryCount));
        clientBuilder.setServiceUnavailableRetryStrategy(new PreemptiveHttpClient.PreemptiveRetryStrategy());
        clientBuilder.setRedirectStrategy(new PreemptiveHttpClient.PreemptiveRedirectStrategy());
        this.httpClient = clientBuilder.build();
    }

    public CloseableHttpResponse execute(HttpUriRequest request) throws IOException {
        HttpClientContext clientContext = HttpClientContext.create();
        return execute(request, clientContext);
    }

    public CloseableHttpResponse execute(HttpUriRequest request, HttpClientContext clientContext) throws IOException {
        if (StringUtils.isNotEmpty(accessToken)) {
            clientContext.setUserToken(accessToken);
        } else {
            clientContext.setCredentialsProvider(basicCredentialsProvider);
        }
        if (authCache != null) {
            clientContext.setAuthCache(authCache);
        }
        return httpClient.execute(request, clientContext);
    }

    @Override
    public void close() {
        try {
            httpClient.close();
        } catch (IOException e) {
            // Do nothing
        }
        connectionManager.close();
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
        Set<Class<? extends IOException>> classSet = new HashSet<>();
        classSet.add(SSLException.class);
        return classSet;
    }

    static class PreemptiveAuth implements HttpRequestInterceptor {
        public void process(final HttpRequest request, final HttpContext context) throws HttpException {
            if (!shouldSetAuthScheme(request, context)) {
                return;
            }

            HttpClientContext finalContext = (HttpClientContext) context;
            AuthState authState = finalContext.getTargetAuthState();
            // If no auth scheme available yet, try to initialize it preemptively
            if (authState.getAuthScheme() == null) {
                String accessToken = finalContext.getUserToken(String.class);
                if (StringUtils.isNotEmpty(accessToken)) {
                    request.addHeader("Authorization", "Bearer " + accessToken);
                } else {
                    CredentialsProvider credsProvider = finalContext.getCredentialsProvider();
                    HttpHost targetHost = finalContext.getTargetHost();
                    Credentials creds = credsProvider.getCredentials(
                            new AuthScope(targetHost.getHostName(), targetHost.getPort()));
                    if (creds != null) {
                        BasicScheme authScheme = new BasicScheme();
                        authState.update(authScheme, creds);
                    }
                }
            }
        }

        /**
         * Used to determine whether preemptive authentication should be performed.
         * In the case of a redirect to a different host, preemptive authentication should not be performed.
         */
        private boolean shouldSetAuthScheme(final HttpRequest request, final HttpContext context) {
            // Get the original host name (before the redirect).
            String originalHost = (String) context.getAttribute(ORIGINAL_HOST_CONTEXT_PARAM);
            if (originalHost == null) {
                // No redirect was performed.
                return true;
            }
            String host;
            try {
                // In case of a redirect, get the new target host.
                host = new URI(((HttpRequestWrapper) request).getOriginal().getRequestLine().getUri()).getHost();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            // Return true if the original host and the target host are identical.
            return host.equals(originalHost);
        }
    }

    /**
     * Class to handle retries when 5xx errors occurs.
     */

    private class PreemptiveRetryStrategy implements ServiceUnavailableRetryStrategy {

        @Override
        public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
            if (response.getStatusLine().getStatusCode() >= 500) {
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
            super(connectionRetries, REQUEST_SENT_RETRY_ENABLED, getNonRetriableClasses());
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

        private final Set<String> redirectableMethods = CommonUtils.newHashSet(
                HttpGet.METHOD_NAME.toLowerCase(),
                HttpPost.METHOD_NAME.toLowerCase(),
                HttpHead.METHOD_NAME.toLowerCase(),
                HttpDelete.METHOD_NAME.toLowerCase(),
                HttpPut.METHOD_NAME.toLowerCase());

        @Override
        public HttpUriRequest getRedirect(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException {
            // Get the original host name (before the redirect) and save it on the context.
            String originalHost = getHost(request);
            context.setAttribute(ORIGINAL_HOST_CONTEXT_PARAM, originalHost);
            URI uri = getLocationURI(request, response, context);
            log.debug("Redirecting to " + uri);
            return RequestBuilder.copy(request).setUri(uri).build();
        }

        private String getHost(HttpRequest request) {
            URI uri;
            try {
                uri = new URI(request.getRequestLine().getUri());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            return uri.getHost();
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

