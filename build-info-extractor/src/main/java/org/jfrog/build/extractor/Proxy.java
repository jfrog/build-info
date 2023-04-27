package org.jfrog.build.extractor;

import org.apache.commons.lang3.StringUtils;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.util.List;

import static java.net.Proxy.Type.HTTP;

public class Proxy {

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final boolean https;

    private Proxy(String host, int port, boolean https) {
        this.host = host;
        this.port = port;
        this.https = https;
        this.username = https ? System.getProperty("https.proxyUser") : System.getProperty("http.proxyUser");
        this.password = https ? System.getProperty("https.proxyPassword") : System.getProperty("http.proxyPassword");
    }

    public static Proxy createFromSystemProperties(String repositoryUrl) {
        if (isProxyPropertiesAvailable(true) && isProxyPropertiesAvailable(false)) {
            return null;
        }
        if (!isValidRepoUrl(repositoryUrl)) {
            return null;
        }
        List<java.net.Proxy> applicableProxies = ProxySelector.getDefault().select(URI.create(repositoryUrl));
        java.net.Proxy proxy = applicableProxies.stream().filter((java.net.Proxy p) -> p.type() == HTTP).findFirst().orElse(null);
        if (proxy == null) {
            return null;
        }
        if (proxy.address() instanceof InetSocketAddress) {
            InetSocketAddress inetAddr = (InetSocketAddress) proxy.address();
            return new Proxy(inetAddr.getHostName(), inetAddr.getPort(), StringUtils.startsWith(repositoryUrl.toLowerCase(), "https"));
        }
        return null;
    }

    /**
     * To use a proxy selector, ensure repository URL starts with http.
     */
    private static boolean isValidRepoUrl(String repoUrl) {
        return StringUtils.startsWith(repoUrl.toLowerCase(), "http");
    }

    private static boolean isProxyPropertiesAvailable(boolean https) {
        String prefix = https ? "https" : "http";
        String host = System.getProperty(prefix + ".proxyHost");
        String port = System.getProperty(prefix + ".proxyPort");
        return !StringUtils.isNotBlank(host) || !StringUtils.isNumeric(port);
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
