package org.jfrog.build.client;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.jfrog.build.api.util.NullLog;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

@Test
public class PreemptiveHttpClientBuilderTest {
    public void testCredentialsMaintainedWithProxy() {
        String rtUser = "rt-user";
        String rtPassword = "rt-password";

        String proxyHost = "127.0.0.1";
        int proxyPort = 8000;
        String proxyUser = "proxy-user";
        String proxyPassword = "proxy-password";
        boolean https = false;
        String noProxyDomain = "noProxyDomain";

        PreemptiveHttpClientBuilder clientBuilder = new PreemptiveHttpClientBuilder()
                .setConnectionRetries(3)
                .setInsecureTls(false)
                .setTimeout(300)
                .setLog(new NullLog())
                .setProxyConfiguration(createProxyConfiguration(proxyHost, proxyPort, proxyUser, proxyPassword, https, noProxyDomain))
                .setUserName(rtUser)
                .setPassword(rtPassword);
        PreemptiveHttpClient deployClient = clientBuilder.build();

        // Assert both Artifactory and proxy credentials exist in the credentials provider.
        BasicCredentialsProvider credentialsProvider = deployClient.basicCredentialsProvider;
        Credentials rtCredentials = credentialsProvider.getCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT));
        assertNotNull(rtCredentials);
        assertEquals(rtCredentials, new UsernamePasswordCredentials(rtUser, rtPassword));

        Credentials portCredentials = credentialsProvider.getCredentials(new AuthScope(proxyHost, proxyPort));
        assertNotNull(portCredentials);
        assertEquals(portCredentials, new UsernamePasswordCredentials(proxyUser, proxyPassword));
    }

    private ProxyConfiguration createProxyConfiguration(String host, int port, String proxyUser, String proxyPassword, boolean https, String noProxyDomain) {
        ProxyConfiguration proxyConfiguration = new ProxyConfiguration();
        proxyConfiguration.host = host;
        proxyConfiguration.port = port;
        proxyConfiguration.username = proxyUser;
        proxyConfiguration.password = proxyPassword;
        return proxyConfiguration;
    }
}
