package org.jfrog.build.extractor.clientConfiguration;

import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.ci.BuildInfoConfigProperties;
import org.jfrog.build.extractor.clientConfiguration.util.encryption.EncryptionKeyPair;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration.decryptPropertiesFromFile;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

@Test
public class ArtifactoryClientConfigurationTest {
    private Path tempFile;

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

    @BeforeMethod
    private void setUp() throws IOException {
        tempFile = Files.createTempFile("BuildInfoExtractorUtilsTest", "").toAbsolutePath();
    }

    @AfterMethod
    private void tearDown() throws IOException {
        Files.deleteIfExists(tempFile);
    }

    @Test(description = "Test read encrypted property file")
    public void testReadEncryptedPropertyFile() throws IOException {
        // Prepare
        ArtifactoryClientConfiguration client = createProxyClient();
        tempFile = Files.createTempFile("BuildInfoExtractorUtilsTest", "").toAbsolutePath();
        client.root.props.put(BuildInfoConfigProperties.PROP_PROPS_FILE, tempFile.toString());

        try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile.toFile())) {
            // Save encrypted file.
            EncryptionKeyPair keyPair = client.persistToEncryptedPropertiesFile(fileOutputStream);
            // Assert decrypted successfully.
            Properties props = decryptPropertiesFromFile(tempFile.toString(), keyPair);
            assertEquals(props.size(), 18);
            assertEquals(props.getProperty("proxy.host"), client.getAllProperties().get("proxy.host"));
        }
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
