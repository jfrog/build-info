package org.jfrog.build.extractor.maven.resolver;

import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.testng.annotations.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ArtifactoryResolutionTest {

    String releaseUrl = "https://www.release-url.com";
    String snapshotUrl = "https://www.snapshot-url.com";
    String username = "User";
    String password = "123";
    String proxyUrl = "www.proxy-url.com";
    String proxyUsername = "proxy-user";
    String ProxyPassword = "proxy-pass";
    int proxyPort = 8888;
    String noProxy = "www.no-proxy-url.com";
    ArtifactoryResolution artifactoryResolution = new ArtifactoryResolution(releaseUrl, snapshotUrl, username, password, proxyUrl, proxyUsername, ProxyPassword, proxyPort, true, noProxy, new NullPlexusLog());
    ArtifactoryResolution artifactoryResolutionOnlyRelease = new ArtifactoryResolution(releaseUrl, null, username, password, proxyUrl, proxyUsername, ProxyPassword, proxyPort, true, noProxy, new NullPlexusLog());
    RemoteRepository snapShotRepository = artifactoryResolution.createSnapshotRepository();
    RemoteRepository releaseRepository = artifactoryResolution.createReleaseRepository();
    RemoteRepository releaseSnapshotRepository = artifactoryResolutionOnlyRelease.createReleaseRepository();

    /**
     * Check the url of snapshot repository is configured successful.
     */
    @Test
    public void repositoryUrl() {
        assertEquals(snapshotUrl, snapShotRepository.getUrl());
        assertEquals(releaseUrl, releaseRepository.getUrl());
        assertEquals(releaseUrl, releaseSnapshotRepository.getUrl());
    }

    @Test
    public void repositoryAuthentication() {
        assertEquals("username=User, password=***", snapShotRepository.getAuthentication().toString());
        assertEquals("username=User, password=***", releaseRepository.getAuthentication().toString());
        assertEquals("username=User, password=***", releaseSnapshotRepository.getAuthentication().toString());

    }

    @Test
    public void repositoryId() {
        assertEquals("artifactory-snapshot", snapShotRepository.getId());
        assertEquals("artifactory-release", releaseRepository.getId());
        assertEquals("artifactory-release-snapshot", releaseSnapshotRepository.getId());
    }

    @Test
    public void repositoryPolicy() {
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

    @Test
    public void repositoryProxy() {
        // Snapshot Repository.
        assertEquals(proxyUrl, snapShotRepository.getProxy().getHost());
        assertEquals(proxyPort, snapShotRepository.getProxy().getPort());
        assertEquals("https", snapShotRepository.getProxy().getType());

        // Release Repository.
        assertEquals(proxyUrl, releaseRepository.getProxy().getHost());
        assertEquals(proxyPort, releaseRepository.getProxy().getPort());
        assertEquals("https", releaseRepository.getProxy().getType());

        // Snapshot & release Repository.
        assertEquals(proxyUrl, releaseSnapshotRepository.getProxy().getHost());
        assertEquals(proxyPort, releaseSnapshotRepository.getProxy().getPort());
        assertEquals("https", releaseSnapshotRepository.getProxy().getType());
    }

    @Test
    public void repositoryHttpProxy() {
        ArtifactoryResolution artifactoryResolutionWithNoHttp = new ArtifactoryResolution(releaseUrl, snapshotUrl, username, password, proxyUrl, proxyUsername, ProxyPassword, proxyPort, false, noProxy, new NullPlexusLog());
        assertEquals("http", (artifactoryResolutionWithNoHttp.createSnapshotRepository().getProxy().getType()));
    }

    @Test
    public void repositoryWithNoProxy() {
        ArtifactoryResolution artifactoryResolutionWithNoHttp = new ArtifactoryResolution(releaseUrl, snapshotUrl, username, password, null, proxyUsername, ProxyPassword, proxyPort, true, noProxy, new NullPlexusLog());
        assertNull(artifactoryResolutionWithNoHttp.createSnapshotRepository().getProxy());
    }

    @Test
    public void ProxyAuthentication() {
        assertEquals("username=proxy-user, password=***", snapShotRepository.getProxy().getAuthentication().toString());
        assertEquals("username=proxy-user, password=***", releaseRepository.getProxy().getAuthentication().toString());
        assertEquals("username=proxy-user, password=***", releaseSnapshotRepository.getProxy().getAuthentication().toString());
    }

    /**
     * Check that no proxy is returned if the snapshot repository URL does match the no-proxy pattern.
     */
    @Test
    public void NoProxyMatch() {
        ArtifactoryResolution noProxyResolution = new ArtifactoryResolution(releaseUrl, snapshotUrl, username, password, proxyUrl, proxyUsername, ProxyPassword, proxyPort, true, "www.snapshot-url.com", new NullPlexusLog());
        assertNull(noProxyResolution.createSnapshotRepository().getProxy());
    }

    /**
     * Check that no proxy is returned if the snapshot repository URL does not match the no-proxy pattern.
     */
    @Test
    public void NoProxyEmpty() {
        ArtifactoryResolution noProxyResolution = new ArtifactoryResolution(releaseUrl, snapshotUrl, username, password, proxyUrl, proxyUsername, ProxyPassword, proxyPort, true, null, new NullPlexusLog());
        assertNotNull(noProxyResolution.createSnapshotRepository().getProxy());
    }
}
