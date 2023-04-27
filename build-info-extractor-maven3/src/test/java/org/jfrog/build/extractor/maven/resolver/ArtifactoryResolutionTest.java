package org.jfrog.build.extractor.maven.resolver;

import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@Test
public class ArtifactoryResolutionTest {

    String releaseUrl = "www.release-url.com";
    String httpReleaseUrl = "http://" + releaseUrl;
    String httpsReleaseUrl = "https://" + releaseUrl;
    String snapshotUrl = "https://www.snapshot-url.com";
    String username = "User";
    String password = "123";
    String httpProxyUrl = "www.http-proxy-url.com";
    String httpsProxyUrl = "www.https-proxy-url.com";
    String proxyUsername = "proxy-user";
    String ProxyPassword = "proxy-pass";
    int httpProxyPort = 8888;
    int httpsProxyPort = 8889;
    String httpNoProxy = "www.http-no-proxy-url.com";
    ArtifactoryResolution artifactoryResolution;
    ArtifactoryResolution artifactoryResolutionOnlyRelease;
    RemoteRepository snapShotRepository;
    RemoteRepository releaseRepository;
    RemoteRepository releaseSnapshotRepository;

    @BeforeClass
    public void setup() {
        // Set https proxy
        System.setProperty("https.proxyHost", httpsProxyUrl);
        System.setProperty("https.proxyPort", Integer.toString(httpsProxyPort));
        System.setProperty("https.proxyUser", proxyUsername);
        System.setProperty("https.proxyPassword", ProxyPassword);

        // Set http proxy
        System.setProperty("http.proxyHost", httpProxyUrl);
        System.setProperty("http.proxyPort", Integer.toString(httpProxyPort));
        System.setProperty("http.proxyUser", proxyUsername);
        System.setProperty("http.proxyPassword", ProxyPassword);

        // Set non proxy
        System.setProperty("http.nonProxyHosts", httpNoProxy);

        artifactoryResolution = new ArtifactoryResolution(httpsReleaseUrl, snapshotUrl, username, password, new NullPlexusLog());
        snapShotRepository = artifactoryResolution.createSnapshotRepository();
        releaseRepository = artifactoryResolution.createReleaseRepository();
        artifactoryResolutionOnlyRelease = new ArtifactoryResolution(httpsReleaseUrl, null, username, password, new NullPlexusLog());
        releaseSnapshotRepository = artifactoryResolutionOnlyRelease.createReleaseRepository();
    }

    @AfterClass
    public void tearDown() {
        Arrays.asList("http", "https").forEach(type -> Arrays.asList(".proxyHost", ".proxyHost", ".proxyUser", ".proxyPassword", ".nonProxyHosts").forEach(System::clearProperty));
    }

    @Test(description = "Check repository URL is configured.")
    public void testUrl() {
        assertEquals(snapshotUrl, snapShotRepository.getUrl());
        assertEquals(httpsReleaseUrl, releaseRepository.getUrl());
        assertEquals(httpsReleaseUrl, releaseSnapshotRepository.getUrl());
    }

    @Test(description = "Check repository authentication is configured.")
    public void testAuth() {
        assertEquals("username=User, password=***", snapShotRepository.getAuthentication().toString());
        assertEquals("username=User, password=***", releaseRepository.getAuthentication().toString());
        assertEquals("username=User, password=***", releaseSnapshotRepository.getAuthentication().toString());
    }

    @Test(description = "Check repository id is configured.")
    public void testId() {
        assertEquals("artifactory-snapshot", snapShotRepository.getId());
        assertEquals("artifactory-release", releaseRepository.getId());
        assertEquals("artifactory-release-snapshot", releaseSnapshotRepository.getId());
    }

    @Test(description = "Check repository policy is configured for release and snapshot repositories.")
    public void testPolicy() {
        // Snapshot Repository
        assertTrue(snapShotRepository.getPolicy(true).isEnabled());
        assertEquals(snapShotRepository.getPolicy(true).getUpdatePolicy(), RepositoryPolicy.UPDATE_POLICY_DAILY);
        assertEquals(snapShotRepository.getPolicy(true).getChecksumPolicy(), RepositoryPolicy.CHECKSUM_POLICY_WARN);
        assertFalse(snapShotRepository.getPolicy(false).isEnabled());
        assertEquals(snapShotRepository.getPolicy(false).getUpdatePolicy(), RepositoryPolicy.UPDATE_POLICY_DAILY);
        assertEquals(snapShotRepository.getPolicy(false).getChecksumPolicy(), RepositoryPolicy.CHECKSUM_POLICY_WARN);

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
        assertEquals(httpsProxyUrl, snapShotRepository.getProxy().getHost());
        assertEquals(httpsProxyPort, snapShotRepository.getProxy().getPort());
        assertEquals("https", snapShotRepository.getProxy().getType());
        assertEquals("username=proxy-user, password=***", snapShotRepository.getProxy().getAuthentication().toString());

        // Release Repository.
        assertEquals(httpsProxyUrl, releaseRepository.getProxy().getHost());
        assertEquals(httpsProxyPort, releaseRepository.getProxy().getPort());
        assertEquals("https", releaseRepository.getProxy().getType());
        assertEquals("username=proxy-user, password=***", releaseRepository.getProxy().getAuthentication().toString());

        // Snapshot & release Repository.
        assertEquals(httpsProxyUrl, releaseSnapshotRepository.getProxy().getHost());
        assertEquals(httpsProxyPort, releaseSnapshotRepository.getProxy().getPort());
        assertEquals("https", releaseSnapshotRepository.getProxy().getType());
        assertEquals("username=proxy-user, password=***", releaseSnapshotRepository.getProxy().getAuthentication().toString());
    }

    @Test(description = "Check HTTP proxy is configured.")
    public void testHttpProxyConfiguration() {
        ArtifactoryResolution artifactoryResolutionWithNoHttp = new ArtifactoryResolution(httpReleaseUrl, snapshotUrl, username, password, new NullPlexusLog());
        RemoteRepository repo = artifactoryResolutionWithNoHttp.createReleaseRepository();
        assertEquals(httpProxyUrl, repo.getProxy().getHost());
        assertEquals(httpProxyPort, repo.getProxy().getPort());
        assertEquals("http", repo.getProxy().getType());
        assertEquals("username=proxy-user, password=***", repo.getProxy().getAuthentication().toString());
    }

    @Test(description = "Check null proxy is returned if repository url does not contains http(s) prefix.")
    public void testNullProxyForRepoWithNoHttpInUrl() {
        ArtifactoryResolution repository = new ArtifactoryResolution(releaseUrl, snapshotUrl, username, password, new NullPlexusLog());
        assertNull(repository.createReleaseRepository().getProxy());
    }

    @Test(description = "Check whenever system property 'http.nonProxyHosts' exists and match the repo http://url, no proxy is returned.")
    public void testNullProxyOnNoProxyPatternMatch() {
        ArtifactoryResolution artifactoryResolutionWithNoHttp = new ArtifactoryResolution(httpReleaseUrl, "http://" + httpNoProxy, username, password, new NullPlexusLog());
        assertNull(artifactoryResolutionWithNoHttp.createSnapshotRepository().getProxy());
    }

    @Test(description = "Check whenever system property 'http.nonProxyHosts' exists and match the repo https://url, no proxy is returned.")
    public void TestNullProxyHttps() {
        ArtifactoryResolution artifactoryResolutionWithNoHttp = new ArtifactoryResolution(httpReleaseUrl, "https://" + httpNoProxy, username, password, new NullPlexusLog());
        assertNull(artifactoryResolutionWithNoHttp.createSnapshotRepository().getProxy());
    }
}
