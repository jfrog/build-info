package org.artifactory.build.api;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Properties;

import static org.testng.Assert.assertNull;

/**
 * Tests the behavior of the artifact class
 *
 * @author Noam Y. Tenne
 */
@Test
public class ArtifactTest {

    /**
     * Validates the artifact values after initializing the default constructor
     */
    public void testEmptyConstructor() {
        Artifact artifact = new Artifact();
        assertNull(artifact.getName(), "Artifact name should have not been initialized.");
        assertNull(artifact.getType(), "Artifact type should have not been initialized.");
        assertNull(artifact.getSha1(), "Artifact SHA1 checksum should have not been initialized.");
        assertNull(artifact.getMd5(), "Artifact MD5 checksum should have not been initialized.");
    }

    /**
     * Validates the artifact values after using the artifact setters
     */
    public void testSetters() {
        String name = "moo";
        String type = "bob";
        String sha1 = "pop";
        String md5 = "gog";
        Properties properties = new Properties();

        Artifact artifact = new Artifact();
        artifact.setName(name);
        artifact.setType(type);
        artifact.setSha1(sha1);
        artifact.setMd5(md5);
        artifact.setProperties(properties);

        Assert.assertEquals(artifact.getName(), name, "Unexpected artifact name.");
        Assert.assertEquals(artifact.getType(), type, "Unexpected artifact type.");
        Assert.assertEquals(artifact.getSha1(), sha1, "Unexpected artifact SHA1 checksum.");
        Assert.assertEquals(artifact.getMd5(), md5, "Unexpected artifact MD5 checksum.");
        Assert.assertEquals(artifact.getProperties(), properties, "Unexpected artifact properties.");
    }
}