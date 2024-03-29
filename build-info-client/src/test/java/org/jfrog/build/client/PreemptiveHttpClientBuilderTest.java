package org.jfrog.build.client;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.jfrog.build.api.util.NullLog;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

@Test
public class PreemptiveHttpClientBuilderTest {
    private static final AuthScope ANY_AUTH = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT);

    public void testCredentialsMaintainedWithProxy() {
        String rtUser = "rt-user";
        String rtPassword = "rt-password";

        String proxyHost = "127.0.0.1";
        int proxyPort = 8000;
        String proxyUser = "proxy-user";
        String proxyPassword = "proxy-password";

        PreemptiveHttpClientBuilder clientBuilder = new PreemptiveHttpClientBuilder()
                .setConnectionRetries(3)
                .setInsecureTls(false)
                .setTimeout(300)
                .setLog(new NullLog())
                .setProxyConfiguration(createProxyConfiguration(proxyHost, proxyPort, proxyUser, proxyPassword))
                .setUserName(rtUser)
                .setPassword(rtPassword);
        try (PreemptiveHttpClient deployClient = clientBuilder.build()) {
            // Assert both Artifactory and proxy credentials exist in the credentials provider.
            BasicCredentialsProvider credentialsProvider = deployClient.basicCredentialsProvider;
            Credentials rtCredentials = credentialsProvider.getCredentials(ANY_AUTH);
            assertNotNull(rtCredentials);
            assertEquals(rtCredentials, new UsernamePasswordCredentials(rtUser, rtPassword));

            Credentials portCredentials = credentialsProvider.getCredentials(new AuthScope(proxyHost, proxyPort));
            assertNotNull(portCredentials);
            assertEquals(portCredentials, new UsernamePasswordCredentials(proxyUser, proxyPassword));
        }
    }

    public void testAnonymousUser() {
        try (PreemptiveHttpClient clientWithAnonymous = new PreemptiveHttpClientBuilder().build();
             PreemptiveHttpClient clientWithoutAnonymous = new PreemptiveHttpClientBuilder().setNoAnonymousUser(true).build()) {
            assertEquals("anonymous", clientWithAnonymous.basicCredentialsProvider.getCredentials(ANY_AUTH).getUserPrincipal().getName());
            assertNull(clientWithoutAnonymous.basicCredentialsProvider.getCredentials(ANY_AUTH));
        }
    }

    private ProxyConfiguration createProxyConfiguration(String host, int port, String proxyUser, String proxyPassword) {
        ProxyConfiguration proxyConfiguration = new ProxyConfiguration();
        proxyConfiguration.host = host;
        proxyConfiguration.port = port;
        proxyConfiguration.username = proxyUser;
        proxyConfiguration.password = proxyPassword;
        return proxyConfiguration;
    }
}
