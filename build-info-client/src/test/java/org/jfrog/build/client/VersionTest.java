package org.jfrog.build.client;

import org.testng.annotations.Test;

import static org.testng.Assert.*;


/**
 * @author Tomer Cohen
 */
@Test
public class VersionTest {

    public void testReleaseVersion() {
        ArtifactoryBuildInfoClient.Version version = new ArtifactoryBuildInfoClient.Version("2.2.3");
        assertFalse(version.isSnapshot());
        assertFalse(version.isNotFound());
        assertEquals(version.weight(), 223);
    }

    public void testSnapshotVersion() {
        ArtifactoryBuildInfoClient.Version version = new ArtifactoryBuildInfoClient.Version("2.2.3-SNAPSHOT");
        assertTrue(version.isSnapshot());
        assertFalse(version.isNotFound());
        assertEquals(version.weight(), 223);
    }

    public void testUnfoundReleaseVersion() {
        ArtifactoryBuildInfoClient.Version version = new ArtifactoryBuildInfoClient.Version("2.2.x");
        assertTrue(version.isSnapshot());
        assertFalse(version.isNotFound());
        assertEquals(version.weight(), 222);
    }

    public void testUnfoundSnapshotVersion() {
        ArtifactoryBuildInfoClient.Version version = new ArtifactoryBuildInfoClient.Version("2.2.x-SNAPSHOT");
        assertTrue(version.isSnapshot());
        assertFalse(version.isNotFound());
        assertEquals(version.weight(), 222);
    }
}
