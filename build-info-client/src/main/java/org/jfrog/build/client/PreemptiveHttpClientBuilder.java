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
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
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
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.jfrog.build.api.util.Log;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Properties;

public class PreemptiveHttpClientBuilder {

    public static final int CONNECTION_POOL_SIZE = 10;
    private static final String CLIENT_VERSION;

    protected final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    protected PoolingHttpClientConnectionManager connectionManager;
    protected AuthCache authCache = new BasicAuthCache();
    protected String accessToken = StringUtils.EMPTY;
    protected int connectionRetries;
    protected Log log;

    private ProxyConfiguration proxyConfiguration;
    private String userAgent = StringUtils.EMPTY;
    private String userName = StringUtils.EMPTY;
    private String password = StringUtils.EMPTY;
    private SSLContext sslContext;
    private boolean insecureTls;
    private HttpHost proxy;
    private int timeout;

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

    public PreemptiveHttpClientBuilder setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
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
        if (proxyConfiguration != null) {
            this.proxy = new HttpHost(proxyConfiguration.host, proxyConfiguration.port);
        }
        return this;
    }
    public ProxyConfiguration getProxyConfiguration(){
        return proxyConfiguration;
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

    public PreemptiveHttpClientBuilder setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
        return this;
    }

    public PreemptiveHttpClient build() {
        buildConnectionManager();
        HttpClientBuilder httpClientBuilder = createHttpClientBuilder();
        createCredentialsAndAuthCache();
        return new PreemptiveHttpClient(connectionManager, credentialsProvider, accessToken, authCache, httpClientBuilder, connectionRetries, log);
    }

    /**
     * Create the credentials provider and the auth cache from username and password.
     */
    protected void createCredentialsAndAuthCache() {
        if (proxyConfiguration != null && proxyConfiguration.username != null) {
            credentialsProvider.setCredentials(new AuthScope(proxyConfiguration.host, proxyConfiguration.port),
                    new UsernamePasswordCredentials(proxyConfiguration.username, proxyConfiguration.password));
            // Create AuthCache instance
            authCache = new BasicAuthCache();
            // Generate BASIC scheme object and add it to the local auth cache
            authCache.put(proxy, new BasicScheme());
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

    /**
     * Create and configure the connections manager.
     */
    protected void buildConnectionManager() {
        try {
            connectionManager = createConnectionManager();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
        connectionManager.setMaxTotal(CONNECTION_POOL_SIZE);
        connectionManager.setDefaultMaxPerRoute(CONNECTION_POOL_SIZE);
    }

    /**
     * Create and configure an http client builder.
     *
     * @return HttpClientBuilder
     */
    protected HttpClientBuilder createHttpClientBuilder() {
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
        String userAgent = StringUtils.defaultIfEmpty(this.userAgent, "Artifactory2BuildClient/" + CLIENT_VERSION);
        builder.setUserAgent(userAgent);

        setDefaultCookieSpecRegistry(builder);
        builder.setProxy(proxy);
        return builder;
    }

    /**
     * Create the pooling connection manager. Use one of the following 3 strategies:
     * 1. Default - Check all certificates and use the default trust manager.
     * 2. Insecure TLS - Trust all certifications, including self signed.
     * 3. Custom SSL context - Use custom trust manager strategy.
     *
     * @return PoolingHttpClientConnectionManager
     * @throws GeneralSecurityException - In case of an error during the creation of the SSL context of the insecure TLS strategy
     */
    private PoolingHttpClientConnectionManager createConnectionManager() throws GeneralSecurityException {
        if (!insecureTls && sslContext == null) {
            // Return default connection manager
            return new PoolingHttpClientConnectionManager();
        }
        SSLConnectionSocketFactory sslConnectionSocketFactory;
        HostnameVerifier hostnameVerifier = new DefaultHostnameVerifier();
        SSLContext sslContext = this.sslContext;
        if (insecureTls) {
            TrustStrategy strategy = TrustAllStrategy.INSTANCE;
            sslContext = SSLContextBuilder.create().loadTrustMaterial(strategy).build();
            // Disable hostname verification.
            hostnameVerifier = NoopHostnameVerifier.INSTANCE;
        }

        sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslConnectionSocketFactory)
                .build();
        return new PoolingHttpClientConnectionManager(socketFactoryRegistry);
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
