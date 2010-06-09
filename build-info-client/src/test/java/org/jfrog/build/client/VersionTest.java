package org.jfrog.build.client;

import org.testng.annotations.Test;

import static org.testng.Assert.*;


/**
 * @author Tomer Cohen
 */
@Test
public class VersionTest {

    public void testReleaseVersion() {
        ArtifactoryHttpClient.Version version = new ArtifactoryHttpClient.Version("2.2.3");
        assertFalse(version.isSnapshot());
        assertFalse(version.isNotFound());
        assertEquals(version.weight(), 223);
    }

    public void testSnapshotVersion() {
        ArtifactoryHttpClient.Version version = new ArtifactoryHttpClient.Version("2.2.3-SNAPSHOT");
        assertTrue(version.isSnapshot());
        assertFalse(version.isNotFound());
        assertEquals(version.weight(), 223);
    }

    public void testUnfoundReleaseVersion() {
        ArtifactoryHttpClient.Version version = new ArtifactoryHttpClient.Version("2.2.x");
        assertTrue(version.isSnapshot());
        assertFalse(version.isNotFound());
        assertEquals(version.weight(), 222);
    }

    public void testUnfoundSnapshotVersion() {
        ArtifactoryHttpClient.Version version = new ArtifactoryHttpClient.Version("2.2.x-SNAPSHOT");
        assertTrue(version.isSnapshot());
        assertFalse(version.isNotFound());
        assertEquals(version.weight(), 222);
    }
}
