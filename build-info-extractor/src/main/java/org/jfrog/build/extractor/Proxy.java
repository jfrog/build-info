package org.jfrog.build.extractor;

import org.apache.commons.lang3.StringUtils;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.util.List;

import static java.net.Proxy.Type.HTTP;

public class Proxy {
    public static String SYSTEM_PROPERTY_HTTP_PROXY_HOST = "http.proxyHost";
    public static String SYSTEM_PROPERTY_HTTP_PROXY_PORT = "http.proxyPort";
    public static String SYSTEM_PROPERTY_HTTP_PROXY_USERNAME = "http.proxyUser";
    public static String SYSTEM_PROPERTY_HTTP_PROXY_PASSWORD = "http.proxyPassword";
    public static String SYSTEM_PROPERTY_HTTPS_PROXY_HOST = "https.proxyHost";
    public static String SYSTEM_PROPERTY_HTTPS_PROXY_PORT = "https.proxyPort";
    public static String SYSTEM_PROPERTY_HTTPS_PROXY_USERNAME = "https.proxyUser";
    public static String SYSTEM_PROPERTY_HTTPS_PROXY_PASSWORD = "https.proxyPassword";
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final boolean https;

    private Proxy(String host, int port) {
        this.host = host;
        this.port = port;
        this.https = isHttpsProxy(host);
        this.username = https ? System.getProperty(SYSTEM_PROPERTY_HTTP_PROXY_USERNAME) : System.getProperty(SYSTEM_PROPERTY_HTTPS_PROXY_USERNAME);
        this.password = https ? System.getProperty(SYSTEM_PROPERTY_HTTP_PROXY_PASSWORD) : System.getProperty(SYSTEM_PROPERTY_HTTPS_PROXY_PASSWORD);
    }

    public static Proxy createFromSystemProperties(String repositoryUrl) {
        if (!isProxyPropertiesAvailable()) {
            return null;
        }
        if (!isValidRepoUrl(repositoryUrl)) {
            return null;
        }
        java.net.Proxy proxy = getProxyFromSelector(repositoryUrl);
        if (proxy == null) {
            return null;
        }
        if (proxy.address() instanceof InetSocketAddress) {
            InetSocketAddress inetAddr = (InetSocketAddress) proxy.address();
            return new Proxy(inetAddr.getHostName(), inetAddr.getPort());
        }
        return null;
    }

    private static java.net.Proxy getProxyFromSelector(String repositoryUrl) {
        List<java.net.Proxy> applicableProxies = ProxySelector.getDefault().select(URI.create(repositoryUrl));
        return applicableProxies.stream().filter((java.net.Proxy p) -> p.type() == HTTP).findFirst().orElse(null);
    }

    private static boolean isHttpsProxy(String proxyUrl) {
        return StringUtils.equals(System.getProperty(SYSTEM_PROPERTY_HTTPS_PROXY_HOST), proxyUrl);
    }

    /**
     * To use a proxy selector, ensure repository URL starts with http.
     */
    private static boolean isValidRepoUrl(String repoUrl) {
        return StringUtils.startsWith(repoUrl.toLowerCase(), "http");
    }

    private static boolean isProxyPropertiesAvailable() {
        return StringUtils.isNotBlank(System.getProperty(SYSTEM_PROPERTY_HTTP_PROXY_HOST) + System.getProperty(SYSTEM_PROPERTY_HTTPS_PROXY_HOST));
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isHttps() {
        return https;
    }
}
