package org.jfrog.build.extractor;

import org.apache.commons.lang3.StringUtils;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import static java.net.Proxy.Type.HTTP;

public class ProxySelector {
    private static final String SYSTEM_PROPERTY_HTTP_PROXY_HOST = "http.proxyHost";
    private static final String SYSTEM_PROPERTY_HTTP_PROXY_PORT = "http.proxyPort";
    private static final String SYSTEM_PROPERTY_HTTP_PROXY_USERNAME = "http.proxyUser";
    private static final String SYSTEM_PROPERTY_HTTP_PROXY_PASSWORD = "http.proxyPassword";
    private static final String SYSTEM_PROPERTY_HTTPS_PROXY_HOST = "https.proxyHost";
    private static final String SYSTEM_PROPERTY_HTTPS_PROXY_PORT = "https.proxyPort";
    private static final String SYSTEM_PROPERTY_HTTPS_PROXY_USERNAME = "https.proxyUser";
    private static final String SYSTEM_PROPERTY_HTTPS_PROXY_PASSWORD = "https.proxyPassword";
    private static final String SYSTEM_PROPERTY_NO_PROXY = "http.nonProxyHosts";
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
        Properties propsBackup = setupProxySelector();
        List<java.net.Proxy> applicableProxies = java.net.ProxySelector.getDefault().select(URI.create(repositoryUrl));
        propsBackup.forEach((key, value) -> System.setProperty(key.toString(), value.toString()));
        return applicableProxies.stream().filter((java.net.Proxy p) -> p.type() == HTTP).findFirst().orElse(null);
    }

    /**
     * Overrides the proxy configuration's system properties.
     * This proxy selector can read these system properties and return the correct proxy based on no-proxy configuration if any exists.
     *
     * @return a copy of the previous proxy configuration, prior to the override.
     *
     * <p>Calling this method will modify the system properties for the proxy configuration, which affects all
     * subsequent network connections made by the JVM. The previous proxy configuration is saved and returned as
     * a copy to allow it to be restored later if needed.</p>
     */
    private Properties setupProxySelector() {
        Properties propsBackup = getProxyProperties();
        fillSelectorConfig(httpProxy, SYSTEM_PROPERTY_HTTP_PROXY_HOST, SYSTEM_PROPERTY_HTTP_PROXY_PORT, SYSTEM_PROPERTY_HTTP_PROXY_USERNAME, SYSTEM_PROPERTY_HTTP_PROXY_PASSWORD);
        fillSelectorConfig(httpsProxy, SYSTEM_PROPERTY_HTTPS_PROXY_HOST, SYSTEM_PROPERTY_HTTPS_PROXY_PORT, SYSTEM_PROPERTY_HTTPS_PROXY_USERNAME, SYSTEM_PROPERTY_HTTPS_PROXY_PASSWORD);
        if (StringUtils.isNotBlank(noProxy)) {
            System.setProperty(SYSTEM_PROPERTY_NO_PROXY, noProxy);
        }
        return propsBackup;
    }

    private Properties getProxyProperties() {
        Properties props = new Properties();
        Stream.of(SYSTEM_PROPERTY_HTTP_PROXY_HOST,
                SYSTEM_PROPERTY_HTTP_PROXY_PORT,
                SYSTEM_PROPERTY_HTTP_PROXY_USERNAME,
                SYSTEM_PROPERTY_HTTP_PROXY_PASSWORD,
                SYSTEM_PROPERTY_HTTPS_PROXY_HOST,
                SYSTEM_PROPERTY_HTTPS_PROXY_PORT,
                SYSTEM_PROPERTY_HTTPS_PROXY_USERNAME,
                SYSTEM_PROPERTY_HTTPS_PROXY_PASSWORD,
                SYSTEM_PROPERTY_NO_PROXY
        ).forEach(key -> {
            String value = StringUtils.isNotBlank(System.getProperty(key)) ? System.getProperty(key) : "";
            props.setProperty(key, value);
        });
        return props;
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
