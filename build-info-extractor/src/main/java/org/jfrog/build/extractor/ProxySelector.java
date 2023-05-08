package org.jfrog.build.extractor;

import org.apache.commons.lang3.StringUtils;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;

import static java.net.Proxy.Type.HTTP;

public class ProxySelector {
    public static String SYSTEM_PROPERTY_HTTP_PROXY_HOST = "http.proxyHost";
    public static String SYSTEM_PROPERTY_HTTP_PROXY_PORT = "http.proxyPort";
    public static String SYSTEM_PROPERTY_HTTP_PROXY_USERNAME = "http.proxyUser";
    public static String SYSTEM_PROPERTY_HTTP_PROXY_PASSWORD = "http.proxyPassword";
    public static String SYSTEM_PROPERTY_HTTPS_PROXY_HOST = "https.proxyHost";
    public static String SYSTEM_PROPERTY_HTTPS_PROXY_PORT = "https.proxyPort";
    public static String SYSTEM_PROPERTY_HTTPS_PROXY_USERNAME = "https.proxyUser";
    public static String SYSTEM_PROPERTY_HTTPS_PROXY_PASSWORD = "https.proxyPassword";
    public static String SYSTEM_PROPERTY_NO_PROXY = "http.nonProxyHosts";
    private final String noProxy;
    private Proxy httpProxy;
    private Proxy httpsProxy;

    public ProxySelector(String httpHost, int httpPort, String httpUsername, String httpPassword, String httpsHost, int httpsPort, String httpsUsername, String httpsPassword, String noProxy) {
        this.noProxy = noProxy;
        if (StringUtils.isNotBlank(httpHost)) {
            this.httpProxy = new Proxy(httpHost, httpPort, httpUsername, httpPassword, false);
        }
        if (StringUtils.isNotBlank(httpsHost)) {
            this.httpsProxy = new Proxy(httpsHost, httpsPort, httpsUsername, httpsPassword, true);
        }
    }

    public Proxy getProxy(String repositoryUrl) {
        if (!isProxyAvailable()) {
            return null;
        }
        if (!isValidRepoUrl(repositoryUrl)) {
            return null;
        }
        java.net.Proxy proxy = selectProxy(repositoryUrl);
        if (proxy == null) {
            return null;
        }
        InetSocketAddress inetAddr = (InetSocketAddress) proxy.address();
        if (httpsProxy != null && StringUtils.equals(inetAddr.getHostName(), httpsProxy.getHost())) {
            return httpsProxy;
        }
        if (httpProxy != null && StringUtils.equals(inetAddr.getHostName(), httpProxy.getHost())) {
            return httpProxy;
        }
        return null;
    }

    private boolean isProxyAvailable() {
        return this.httpProxy != null || this.httpsProxy != null;
    }

    /**
     * To use a proxy selector, ensure repository URL starts with http.
     */
    private boolean isValidRepoUrl(String repoUrl) {
        return StringUtils.startsWith(repoUrl.toLowerCase(), "http");
    }

    private java.net.Proxy selectProxy(String repositoryUrl) {
        setupProxySelector();
        List<java.net.Proxy> applicableProxies = java.net.ProxySelector.getDefault().select(URI.create(repositoryUrl));
        return applicableProxies.stream().filter((java.net.Proxy p) -> p.type() == HTTP).findFirst().orElse(null);
    }

    private void setupProxySelector() {
        fillSelectorConfig(httpProxy, SYSTEM_PROPERTY_HTTP_PROXY_HOST, SYSTEM_PROPERTY_HTTP_PROXY_PORT, SYSTEM_PROPERTY_HTTP_PROXY_USERNAME, SYSTEM_PROPERTY_HTTP_PROXY_PASSWORD);
        fillSelectorConfig(httpsProxy, SYSTEM_PROPERTY_HTTPS_PROXY_HOST, SYSTEM_PROPERTY_HTTPS_PROXY_PORT, SYSTEM_PROPERTY_HTTPS_PROXY_USERNAME, SYSTEM_PROPERTY_HTTPS_PROXY_PASSWORD);
        if (StringUtils.isNotBlank(noProxy)) {
            System.setProperty(SYSTEM_PROPERTY_NO_PROXY, noProxy);
        }
    }

    private void fillSelectorConfig(Proxy proxy, String hostKey, String portKey, String usernameKey, String passwordKey) {
        if (proxy != null) {
            if (proxy.getPort() <= 0 || StringUtils.isBlank(proxy.getHost())) {
                return;
            }
            System.setProperty(hostKey, proxy.getHost());
            System.setProperty(portKey, Integer.toString(proxy.getPort()));
            if (StringUtils.isNotBlank(proxy.getUsername())) {
                System.setProperty(usernameKey, proxy.getUsername());
            }
            if (StringUtils.isNotBlank(proxy.getPassword())) {
                System.setProperty(passwordKey, proxy.getPassword());
            }
        }
    }
}
