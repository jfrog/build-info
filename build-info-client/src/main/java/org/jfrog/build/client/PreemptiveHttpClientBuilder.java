package org.jfrog.build.client;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.util.PublicSuffixMatcher;
import org.apache.http.conn.util.PublicSuffixMatcherLoader;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.DefaultCookieSpecProvider;
import org.apache.http.impl.cookie.IgnoreSpecProvider;
import org.apache.http.impl.cookie.NetscapeDraftSpecProvider;
import org.apache.http.impl.cookie.RFC6265CookieSpecProvider;
import org.apache.http.ssl.SSLContextBuilder;;import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import org.jfrog.build.api.util.Log;


public class PreemptiveHttpClientBuilder {

    public static final int CONNECTION_POOL_SIZE = 10;
    private static final String CLIENT_VERSION;

    private int timeout = 0;
    private int connectionRetries = 0;
    private boolean insecureTls = false;
    private String userName = StringUtils.EMPTY;
    private String password = StringUtils.EMPTY;
    private String accessToken = StringUtils.EMPTY;
    private Log log;
    private ProxyConfiguration proxyConfiguration = null;

    private PoolingHttpClientConnectionManager connectionManager;
    private HttpHost proxy = null;
    private BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    private AuthCache authCache = new BasicAuthCache();

    static {
        // initialize client version
        Properties properties = new Properties();
        try (InputStream is = PreemptiveHttpClient.class.getResourceAsStream("/bi.client.properties")) {
            properties.load(is);
        } catch (IOException e) {
            // ignore, use the default value
        }
        CLIENT_VERSION = properties.getProperty("client.version", "unknown");
    }

    public PreemptiveHttpClientBuilder setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public PreemptiveHttpClientBuilder setPassword(String password) {
        this.password = password;
        return this;
    }

    public PreemptiveHttpClientBuilder setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    public PreemptiveHttpClientBuilder setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public PreemptiveHttpClientBuilder setProxyConfiguration(ProxyConfiguration proxyConfiguration) {
        this.proxyConfiguration = proxyConfiguration;
        this.proxy = new HttpHost(proxyConfiguration.host, proxyConfiguration.port);
        return this;
    }

    public PreemptiveHttpClientBuilder setConnectionRetries(int connectionRetries) {
        this.connectionRetries = connectionRetries;
        return this;
    }

    public PreemptiveHttpClientBuilder setInsecureTls(boolean insecureTls) {
        this.insecureTls = insecureTls;
        return this;
    }

    public PreemptiveHttpClientBuilder setLog(Log log) {
        this.log = log;
        return this;
    }

    public PreemptiveHttpClient build() {
        buildConnectionManager();
        HttpClientBuilder httpClientBuilder = createHttpClientBuilder();
        createCredentialsAndAuthCache();
        return new PreemptiveHttpClient(connectionManager, credentialsProvider, accessToken, authCache, httpClientBuilder, connectionRetries, log);
    }

    private void createCredentialsAndAuthCache() {

        if (proxyConfiguration != null && proxyConfiguration.username != null) {
            credentialsProvider.setCredentials(new AuthScope(proxyConfiguration.host, proxyConfiguration.port),
                    new UsernamePasswordCredentials(proxyConfiguration.username, proxyConfiguration.password));
            // Create AuthCache instance
            authCache = new BasicAuthCache();
            // Generate BASIC scheme object and add it to the local auth cache
            authCache.put(proxy, new BasicScheme());
            return;
        }

        if (StringUtils.isEmpty(accessToken)) {
            if (StringUtils.isEmpty(userName)) {
                userName = "anonymous";
                password = "";
            }

            credentialsProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                    new UsernamePasswordCredentials(userName, password));
        }
    }

    private void buildConnectionManager() {
        connectionManager = insecureTls ? createInsecureTlsConnectionManager() : new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(CONNECTION_POOL_SIZE);
        connectionManager.setDefaultMaxPerRoute(CONNECTION_POOL_SIZE);
    }

    private PoolingHttpClientConnectionManager createInsecureTlsConnectionManager() {
        try {
            // Use the TrustSelfSignedStrategy to allow Self Signed Certificates.
            SSLContext sslContext = SSLContextBuilder
                    .create()
                    .loadTrustMaterial(new TrustSelfSignedStrategy())
                    .build();
            // Disable hostname verification.
            HostnameVerifier allowAllHosts = new NoopHostnameVerifier();
            // Create an SSL Socket Factory to use the SSLContext with the trust self signed certificate strategy.
            SSLConnectionSocketFactory connectionFactory = new SSLConnectionSocketFactory(sslContext, allowAllHosts);
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
                    .<ConnectionSocketFactory>create().register("https", connectionFactory)
                    .build();
            return new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
           throw new RuntimeException(e);
        }
    }

    private HttpClientBuilder createHttpClientBuilder() {
        int timeoutMilliSeconds = timeout * 1000;
        RequestConfig requestConfig = RequestConfig
                .custom()
                .setSocketTimeout(timeoutMilliSeconds)
                .setConnectTimeout(timeoutMilliSeconds)
                .setCircularRedirectsAllowed(true)
                .build();

        HttpClientBuilder builder = HttpClientBuilder
                .create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig);

        // Add as the first request interceptor
        builder.addInterceptorFirst(new PreemptiveHttpClient.PreemptiveAuth());

        // Set the following user agent with each request
        String userAgent = "ArtifactoryBuildClient/" + CLIENT_VERSION;
        builder.setUserAgent(userAgent);

        setDefaultCookieSpecRegistry(builder);
        builder.setProxy(proxy);
        return builder;
    }

    /**
     * This method configures the http client builder cookie spec, to avoid log messages like:
     * Invalid cookie header: "Set-Cookie: AWSALB=jgFuoBrtnHLZCOr1B07ulLBEGSXLWcGZO8rTzzuuORNDpTubaDixX30r9N3F3Hy9xAlFgXhVghWJHE4V8uNQSNUsz7Wx7geQ8zrlG8mPva2yeCyuKDVm4iO6/IdP; Expires=Tue, 25 Jun 2019 22:20:19 GMT; Path=/". Invalid 'expires' attribute: Tue, 25 Jun 2019 22:20:19 GMT
     */
    private void setDefaultCookieSpecRegistry(HttpClientBuilder clientBuilder) {
        PublicSuffixMatcher publicSuffixMatcher = PublicSuffixMatcherLoader.getDefault();
        clientBuilder.setPublicSuffixMatcher(publicSuffixMatcher);

        final CookieSpecProvider defaultProvider = new DefaultCookieSpecProvider(
                DefaultCookieSpecProvider.CompatibilityLevel.DEFAULT, publicSuffixMatcher, new String[]{
                "EEE, dd-MMM-yy HH:mm:ss z", // Netscape expires pattern
                DateUtils.PATTERN_RFC1036,
                DateUtils.PATTERN_ASCTIME,
                DateUtils.PATTERN_RFC1123
        }, false);

        final CookieSpecProvider laxStandardProvider = new RFC6265CookieSpecProvider(
                RFC6265CookieSpecProvider.CompatibilityLevel.RELAXED, publicSuffixMatcher);
        final CookieSpecProvider strictStandardProvider = new RFC6265CookieSpecProvider(
                RFC6265CookieSpecProvider.CompatibilityLevel.STRICT, publicSuffixMatcher);

        clientBuilder.setDefaultCookieSpecRegistry(RegistryBuilder.<CookieSpecProvider>create()
                .register(CookieSpecs.DEFAULT, defaultProvider)
                .register("best-match", defaultProvider)
                .register("compatibility", defaultProvider)
                .register(CookieSpecs.STANDARD, laxStandardProvider)
                .register(CookieSpecs.STANDARD_STRICT, strictStandardProvider)
                .register(CookieSpecs.NETSCAPE, new NetscapeDraftSpecProvider())
                .register(CookieSpecs.IGNORE_COOKIES, new IgnoreSpecProvider())
                .build());
    }
}
