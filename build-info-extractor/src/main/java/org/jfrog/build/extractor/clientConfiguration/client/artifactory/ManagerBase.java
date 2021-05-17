package org.jfrog.build.extractor.clientConfiguration.client.artifactory;

import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.JFrogHttpClient;
import org.jfrog.build.client.ProxyConfiguration;

import javax.net.ssl.SSLContext;

public abstract class ManagerBase implements AutoCloseable {
    protected final JFrogHttpClient jfrogHttpClient;
    protected final Log log;

    protected ManagerBase(String url, String username, String password, String accessToken, Log logger) {
        if (StringUtils.isNotEmpty(accessToken)) {
            jfrogHttpClient = new JFrogHttpClient(url, accessToken, logger);
        } else {
            jfrogHttpClient = new JFrogHttpClient(url, username, password, logger);
        }
        log = logger;
    }

    /**
     * Network timeout in seconds to use both for connection establishment and for unanswered requests.
     *
     * @param connectionTimeout Timeout in seconds.
     */
    public void setConnectionTimeout(int connectionTimeout) {
        jfrogHttpClient.setConnectionTimeout(connectionTimeout);
    }

    /**
     * Connection Retries to perform
     *
     * @param connectionRetries The number of max retries.
     */
    public void setConnectionRetries(int connectionRetries) {
        jfrogHttpClient.setConnectionRetries(connectionRetries);
    }

    /**
     * Sets the proxy host and port.
     *
     * @param host Proxy host
     * @param port Proxy port
     */
    public void setProxyConfiguration(String host, int port) {
        jfrogHttpClient.setProxyConfiguration(host, port, null, null);
    }

    /**
     * Sets the proxy details.
     *
     * @param host     Proxy host
     * @param port     Proxy port
     * @param username Username to authenticate with the proxy
     * @param password Password to authenticate with the proxy
     */
    public void setProxyConfiguration(String host, int port, String username, String password) {
        jfrogHttpClient.setProxyConfiguration(host, port, username, password);
    }

    public ProxyConfiguration getProxyConfiguration() {
        return jfrogHttpClient.getProxyConfiguration();
    }

    /**
     * Sets full proxy details.
     *
     * @param proxy Proxy instance {@link org.jfrog.build.client.ProxyConfiguration}
     */
    public void setProxyConfiguration(ProxyConfiguration proxy) {
        jfrogHttpClient.setProxyConfiguration(proxy.host, proxy.port, proxy.username, proxy.password);
    }

    /**
     * Log setter for the PreemptiveHttpClient for jobs like the Jenkins Generic job that uses NullLog by default.
     *
     * @param log Log instance
     */
    public void setLog(Log log) {
        jfrogHttpClient.getHttpClient().setLog(log);
    }

    public void setInsecureTls(boolean insecureTls) {
        jfrogHttpClient.setInsecureTls(insecureTls);
    }

    public void setSslContext(SSLContext sslContext) {
        jfrogHttpClient.setSslContext(sslContext);
    }

    public String getUrl() {
        return jfrogHttpClient.getUrl();
    }

    @Override
    public void close() {
        if (jfrogHttpClient != null) {
            jfrogHttpClient.close();
        }
    }
}
