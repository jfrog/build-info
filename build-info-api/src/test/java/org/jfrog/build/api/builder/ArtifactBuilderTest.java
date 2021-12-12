package org.jfrog.build.api.builder;

import org.jfrog.build.api.Artifact;
import org.testng.annotations.Test;

import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Tests the behavior of the artifact builder class
 *
 * @author Noam Y. Tenne
 */
@Test
public class ArtifactBuilderTest {

    /**
     * Validates the artifact values when using the defaults
     */
    public void testDefaultBuild() {
        Artifact artifact = new ArtifactBuilder("name").build();

        assertEquals(artifact.getName(), "name", "Unexpected artifact name.");
        assertNull(artifact.getRemotePath(), "Default artifact remote path should be null");
        assertNull(artifact.getType(), "Default artifact type.");
        assertNull(artifact.getSha1(), "Default artifact SHA1 checksum should be null.");
        assertNull(artifact.getSha256(), "Default artifact SHA256 checksum should be null.");
        assertNull(artifact.getMd5(), "Default artifact MD5 checksum should be null.");
        assertNull(artifact.getProperties(), "Default artifact properties should be null.");
    }

    /**
     * Validates the artifact values after using the builder setters
     */
    public void testBuilderSetters() {
        String name = "moo";
        String type = "bob";
        String sha1 = "pop";
        String sha256 = "lol";
        String md5 = "shmop";
        String localPath = "blip";
        String remotePath = "blop";
        Properties properties = new Properties();

        Artifact artifact = new ArtifactBuilder(name).type(type).sha1(sha1).sha256(sha256).md5(md5)
                .remotePath(remotePath).properties(properties)
                .build();

        assertEquals(artifact.getName(), name, "Unexpected artifact ID.");
        assertEquals(artifact.getType(), type, "Unexpected artifact type.");
        assertEquals(artifact.getSha1(), sha1, "Unexpected artifact SHA1 checksum.");
        assertEquals(artifact.getSha256(), sha256, "Unexpected artifact SHA256 checksum.");
        assertEquals(artifact.getMd5(), md5, "Unexpected artifact SHA1 checksum.");
        assertEquals(artifact.getRemotePath(), remotePath, "Unexpected artifact remote path.");
        assertEquals(artifact.getProperties(), properties, "Unexpected artifact properties.");
        assertTrue(artifact.getProperties().isEmpty(), "Artifact properties list should not have been populated.");
    }

    /**
     * Validates the artifact values after using the builder add methods
     */
    public void testBuilderAddMethods() {
        String propertyKey = "key";
        String propertyValue = "value";

        Artifact artifact = new ArtifactBuilder("name").addProperty(propertyKey, propertyValue).build();
        assertTrue(artifact.getProperties().containsKey(propertyKey), "An artifact property should have been added.");
        assertEquals(artifact.getProperties().get(propertyKey), propertyValue, "Unexpected artifact property value.");
    }
}