package org.jfrog.build.extractor.maven.resolver;

import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.jfrog.build.extractor.Proxy.SYSTEM_PROPERTY_HTTPS_PROXY_HOST;
import static org.jfrog.build.extractor.Proxy.SYSTEM_PROPERTY_HTTPS_PROXY_PASSWORD;
import static org.jfrog.build.extractor.Proxy.SYSTEM_PROPERTY_HTTPS_PROXY_PORT;
import static org.jfrog.build.extractor.Proxy.SYSTEM_PROPERTY_HTTPS_PROXY_USERNAME;
import static org.jfrog.build.extractor.Proxy.SYSTEM_PROPERTY_HTTP_PROXY_HOST;
import static org.jfrog.build.extractor.Proxy.SYSTEM_PROPERTY_HTTP_PROXY_PASSWORD;
import static org.jfrog.build.extractor.Proxy.SYSTEM_PROPERTY_HTTP_PROXY_PORT;
import static org.jfrog.build.extractor.Proxy.SYSTEM_PROPERTY_HTTP_PROXY_USERNAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@Test
public class ArtifactoryResolutionTest {

    final String RELEASE_URL = "www.release-url.com";
    final String HTTP_RELEASE_URL = "http://" + RELEASE_URL;
    final String HTTPS_RELEASE_URL = "https://" + RELEASE_URL;
    final String SNAPSHOT_URL = "https://www.snapshot-url.com";
    final String USERNAME = "User";
    final String PASSWORD = "123";
    final String HTTP_PROXY_URL = "www.http-proxy-url.com";
    final String HTTPS_PROXY_URL = "www.https-proxy-url.com";
    final String PROXY_USERNAME = "proxy-user";
    final String PROXY_PASSWORD = "proxy-pass";
    final int PROXY_HTTP_PORT = 8888;
    final int PROXY_HTTPS_PORT = 8889;
    final String NO_PROXY_PATTERN = "www.http-no-proxy-url.com";
    ArtifactoryResolution artifactoryResolution;
    ArtifactoryResolution artifactoryResolutionOnlyRelease;
    RemoteRepository snapshotRepository;
    RemoteRepository releaseRepository;
    RemoteRepository releaseSnapshotRepository;

    @BeforeClass
    public void setup() {
        // Set https proxy
        System.setProperty(SYSTEM_PROPERTY_HTTPS_PROXY_HOST, HTTPS_PROXY_URL);
        System.setProperty(SYSTEM_PROPERTY_HTTPS_PROXY_PORT, Integer.toString(PROXY_HTTPS_PORT));
        System.setProperty(SYSTEM_PROPERTY_HTTPS_PROXY_USERNAME, PROXY_USERNAME);
        System.setProperty(SYSTEM_PROPERTY_HTTPS_PROXY_PASSWORD, PROXY_PASSWORD);

        // Set http proxy
        System.setProperty(SYSTEM_PROPERTY_HTTP_PROXY_HOST, HTTP_PROXY_URL);
        System.setProperty(SYSTEM_PROPERTY_HTTP_PROXY_PORT, Integer.toString(PROXY_HTTP_PORT));
        System.setProperty(SYSTEM_PROPERTY_HTTP_PROXY_USERNAME, PROXY_USERNAME);
        System.setProperty(SYSTEM_PROPERTY_HTTP_PROXY_PASSWORD, PROXY_PASSWORD);

        // Set non proxy
        System.setProperty("http.nonProxyHosts", NO_PROXY_PATTERN);

        artifactoryResolution = new ArtifactoryResolution(HTTPS_RELEASE_URL, SNAPSHOT_URL, USERNAME, PASSWORD, new NullPlexusLog());
        snapshotRepository = artifactoryResolution.createSnapshotRepository();
        releaseRepository = artifactoryResolution.createReleaseRepository();
        artifactoryResolutionOnlyRelease = new ArtifactoryResolution(HTTPS_RELEASE_URL, null, USERNAME, PASSWORD, new NullPlexusLog());
        releaseSnapshotRepository = artifactoryResolutionOnlyRelease.createReleaseRepository();
    }

    @AfterClass
    public void tearDown() {
        Arrays.asList("http", "https").forEach(type -> Arrays.asList(".proxyHost", ".proxyHost", ".proxyUser", ".proxyPassword", ".nonProxyHosts").forEach(System::clearProperty));
    }

    @Test(description = "Check repository URL is configured.")
    public void testUrl() {
        assertEquals(SNAPSHOT_URL, snapshotRepository.getUrl());
        assertEquals(HTTPS_RELEASE_URL, releaseRepository.getUrl());
        assertEquals(HTTPS_RELEASE_URL, releaseSnapshotRepository.getUrl());
    }

    @Test(description = "Check repository authentication is configured.")
    public void testAuth() {
        assertEquals("username=User, password=***", snapshotRepository.getAuthentication().toString());
        assertEquals("username=User, password=***", releaseRepository.getAuthentication().toString());
        assertEquals("username=User, password=***", releaseSnapshotRepository.getAuthentication().toString());
    }

    @Test(description = "Check repository id is configured.")
    public void testId() {
        assertEquals("artifactory-snapshot", snapshotRepository.getId());
        assertEquals("artifactory-release", releaseRepository.getId());
        assertEquals("artifactory-release-snapshot", releaseSnapshotRepository.getId());
    }

    @Test(description = "Check repository policy is configured for release and snapshot repositories.")
    public void testPolicy() {
        // Snapshot Repository
        assertTrue(snapshotRepository.getPolicy(true).isEnabled());
        assertEquals(snapshotRepository.getPolicy(true).getUpdatePolicy(), RepositoryPolicy.UPDATE_POLICY_DAILY);
        assertEquals(snapshotRepository.getPolicy(true).getChecksumPolicy(), RepositoryPolicy.CHECKSUM_POLICY_WARN);
        assertFalse(snapshotRepository.getPolicy(false).isEnabled());
        assertEquals(snapshotRepository.getPolicy(false).getUpdatePolicy(), RepositoryPolicy.UPDATE_POLICY_DAILY);
        assertEquals(snapshotRepository.getPolicy(false).getChecksumPolicy(), RepositoryPolicy.CHECKSUM_POLICY_WARN);

        // Release
        assertFalse(releaseRepository.getPolicy(true).isEnabled());
        assertEquals(releaseRepository.getPolicy(true).getUpdatePolicy(), RepositoryPolicy.UPDATE_POLICY_DAILY);
        assertEquals(releaseRepository.getPolicy(true).getChecksumPolicy(), RepositoryPolicy.CHECKSUM_POLICY_WARN);
        assertTrue(releaseRepository.getPolicy(false).isEnabled());
        assertEquals(releaseRepository.getPolicy(false).getUpdatePolicy(), RepositoryPolicy.UPDATE_POLICY_DAILY);
        assertEquals(releaseRepository.getPolicy(false).getChecksumPolicy(), RepositoryPolicy.CHECKSUM_POLICY_WARN);

        // Snapshot and release
        assertTrue(releaseSnapshotRepository.getPolicy(true).isEnabled());
        assertEquals(releaseSnapshotRepository.getPolicy(true).getUpdatePolicy(), RepositoryPolicy.UPDATE_POLICY_DAILY);
        assertEquals(releaseSnapshotRepository.getPolicy(true).getChecksumPolicy(), RepositoryPolicy.CHECKSUM_POLICY_WARN);
        assertTrue(releaseSnapshotRepository.getPolicy(false).isEnabled());
        assertEquals(releaseSnapshotRepository.getPolicy(false).getUpdatePolicy(), RepositoryPolicy.UPDATE_POLICY_DAILY);
        assertEquals(releaseSnapshotRepository.getPolicy(false).getChecksumPolicy(), RepositoryPolicy.CHECKSUM_POLICY_WARN);
    }

    @Test(description = "Check HTTPS proxy is configured.")
    public void testProxyHttpsConfiguration() {
        // Snapshot Repository.
        assertEquals(HTTPS_PROXY_URL, snapshotRepository.getProxy().getHost());
        assertEquals(PROXY_HTTPS_PORT, snapshotRepository.getProxy().getPort());
        assertEquals("https", snapshotRepository.getProxy().getType());
        assertEquals("username=proxy-user, password=***", snapshotRepository.getProxy().getAuthentication().toString());

        // Release Repository.
        assertEquals(HTTPS_PROXY_URL, releaseRepository.getProxy().getHost());
        assertEquals(PROXY_HTTPS_PORT, releaseRepository.getProxy().getPort());
        assertEquals("https", releaseRepository.getProxy().getType());
        assertEquals("username=proxy-user, password=***", releaseRepository.getProxy().getAuthentication().toString());

        // Snapshot & release Repository.
        assertEquals(HTTPS_PROXY_URL, releaseSnapshotRepository.getProxy().getHost());
        assertEquals(PROXY_HTTPS_PORT, releaseSnapshotRepository.getProxy().getPort());
        assertEquals("https", releaseSnapshotRepository.getProxy().getType());
        assertEquals("username=proxy-user, password=***", releaseSnapshotRepository.getProxy().getAuthentication().toString());
    }

    @Test(description = "Check HTTP proxy is configured.")
    public void testHttpProxyConfiguration() {
        ArtifactoryResolution artifactoryResolutionWithNoHttp = new ArtifactoryResolution(HTTP_RELEASE_URL, SNAPSHOT_URL, USERNAME, PASSWORD, new NullPlexusLog());
        RemoteRepository repo = artifactoryResolutionWithNoHttp.createReleaseRepository();
        assertEquals(HTTP_PROXY_URL, repo.getProxy().getHost());
        assertEquals(PROXY_HTTP_PORT, repo.getProxy().getPort());
        assertEquals("http", repo.getProxy().getType());
        assertEquals("username=proxy-user, password=***", repo.getProxy().getAuthentication().toString());
    }

    @Test(description = "Check null proxy is returned if repository url does not contains http(s) prefix.")
    public void testNullProxyForRepoWithNoHttpInUrl() {
        ArtifactoryResolution repository = new ArtifactoryResolution(RELEASE_URL, SNAPSHOT_URL, USERNAME, PASSWORD, new NullPlexusLog());
        assertNull(repository.createReleaseRepository().getProxy());
    }

    @Test(description = "In the case of 'http.nonProxyHosts' matching the repository URL with 'http' prefix, null is returned from getProxy().")
    public void testNullProxyOnNoProxyPatternMatch() {
        ArtifactoryResolution artifactoryResolutionWithNoHttp = new ArtifactoryResolution(HTTP_RELEASE_URL, "http://" + NO_PROXY_PATTERN, USERNAME, PASSWORD, new NullPlexusLog());
        assertNull(artifactoryResolutionWithNoHttp.createSnapshotRepository().getProxy());
    }

    @Test(description = "In the case of 'http.nonProxyHosts' matching the repository URL with 'https' prefix, null is returned from getProxy().")
    public void TestNullProxyHttps() {
        ArtifactoryResolution artifactoryResolutionWithNoHttp = new ArtifactoryResolution(HTTP_RELEASE_URL, "https://" + NO_PROXY_PATTERN, USERNAME, PASSWORD, new NullPlexusLog());
        assertNull(artifactoryResolutionWithNoHttp.createSnapshotRepository().getProxy());
    }

    @Test(description = "HTTP proxy is configured, but HTTPS isn't => a valid proxy return.")
    public void testOnlyHttpProxyConfigured() {
        // Prepare
        String proxyUrl = System.getProperty(SYSTEM_PROPERTY_HTTP_PROXY_HOST);
        System.clearProperty(SYSTEM_PROPERTY_HTTP_PROXY_HOST);
        // Act
        ArtifactoryResolution artifactoryResolutionWithNoHttp = new ArtifactoryResolution(HTTP_RELEASE_URL, SNAPSHOT_URL, USERNAME, PASSWORD, new NullPlexusLog());
        // Assert
        assertNotNull(artifactoryResolutionWithNoHttp.createSnapshotRepository().getProxy());
        System.setProperty(SYSTEM_PROPERTY_HTTP_PROXY_HOST, proxyUrl);
    }

    @Test(description = "HTTPS proxy is configured, but HTTP isn't => a valid proxy return.")
    public void testOnlyHttpsProxyConfigured() {
        // Prepare
        String proxyUrl = System.getProperty(SYSTEM_PROPERTY_HTTPS_PROXY_HOST);
        System.clearProperty(SYSTEM_PROPERTY_HTTPS_PROXY_HOST);
        // Act
        ArtifactoryResolution artifactoryResolutionWithNoHttp = new ArtifactoryResolution(HTTP_RELEASE_URL, SNAPSHOT_URL, USERNAME, PASSWORD, new NullPlexusLog());
        // Assert
        assertNotNull(artifactoryResolutionWithNoHttp.createReleaseRepository().getProxy());
        System.setProperty(SYSTEM_PROPERTY_HTTPS_PROXY_HOST, proxyUrl);
    }

    @Test(description = "HTTP proxy is configured, HTTPS is not, and repository URL starts with HTTPS => no proxy return.")
    public void testHttpProxyWithRepoHttps() {
        // Prepare
        String proxyUrl = System.getProperty(SYSTEM_PROPERTY_HTTPS_PROXY_HOST);
        System.clearProperty(SYSTEM_PROPERTY_HTTPS_PROXY_HOST);
        // Act
        ArtifactoryResolution resolutionRepository = new ArtifactoryResolution(HTTP_RELEASE_URL, SNAPSHOT_URL, USERNAME, PASSWORD, new NullPlexusLog());
        // Assert
        assertNull(resolutionRepository.createSnapshotRepository().getProxy());
        System.setProperty(SYSTEM_PROPERTY_HTTPS_PROXY_HOST, proxyUrl);
    }
}
