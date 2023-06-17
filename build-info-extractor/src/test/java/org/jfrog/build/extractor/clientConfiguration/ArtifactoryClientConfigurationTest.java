package org.jfrog.build.extractor.clientConfiguration;

import org.jfrog.build.api.util.NullLog;
import org.testng.annotations.Test;

import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

@Test
public class ArtifactoryClientConfigurationTest {
    @Test(description = "Test http and https proxy handler.")
    public void testProxyHandler() {
        ArtifactoryClientConfiguration client = createProxyClient();

        // Assert http proxy configuration.
        ArtifactoryClientConfiguration.ProxyHandler httpProxy = client.proxy;
        assertEquals(httpProxy.getHost(), "www.http-proxy-url.com");
        assertEquals(httpProxy.getPort(), Integer.valueOf(8888));
        assertEquals(httpProxy.getUsername(), "proxyUser");
        assertEquals(httpProxy.getPassword(), "proxyPassword");

        // Assert https proxy configuration.
        ArtifactoryClientConfiguration.ProxyHandler httpsProxy = client.httpsProxy;
        assertEquals(httpsProxy.getHost(), "www.https-proxy-url.com");
        assertEquals(httpsProxy.getPort(), Integer.valueOf(8889));
        assertEquals(httpsProxy.getUsername(), "proxyUser2");
        assertNull(httpsProxy.getPassword());
    }

    private ArtifactoryClientConfiguration createProxyClient() {
        ArtifactoryClientConfiguration client = new ArtifactoryClientConfiguration(new NullLog());
        Properties props = new Properties();
        props.put("proxy.host", "www.http-proxy-url.com");
        props.put("proxy.port", "8888");
        props.put("proxy.username", "proxyUser");
        props.put("proxy.password", "proxyPassword");
        props.put("proxy.noProxy", "noProxyDomain");
        props.put("proxy.https.host", "www.https-proxy-url.com");
        props.put("proxy.https.port", "8889");
        props.put("proxy.https.username", "proxyUser2");
        client.fillFromProperties(props);
        return client;
    }
}
