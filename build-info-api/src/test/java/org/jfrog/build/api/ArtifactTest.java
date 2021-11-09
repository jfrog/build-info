package org.jfrog.build.api;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
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
        assertNull(artifact.getRemotePath(), "Artifact remote path should have not been initialized.");
        assertNull(artifact.getType(), "Artifact type should have not been initialized.");
        assertNull(artifact.getSha1(), "Artifact SHA1 checksum should have not been initialized.");
        assertNull(artifact.getSha256(), "Artifact SHA256 checksum should have not been initialized.");
        assertNull(artifact.getMd5(), "Artifact MD5 checksum should have not been initialized.");
    }

    /**
     * Validates the artifact values after using the artifact setters
     */
    public void testSetters() {
        String name = "moo";
        String type = "bob";
        String sha1 = "pop";
        String sha256 = "lol";
        String md5 = "gog";
        String localPath = "blip";
        String remotePath = "blop";
        Properties properties = new Properties();

        Artifact artifact = new Artifact();
        artifact.setName(name);
        artifact.setType(type);
        artifact.setSha1(sha1);
        artifact.setSha256(sha256);
        artifact.setMd5(md5);
        artifact.setRemotePath(remotePath);
        artifact.setProperties(properties);

        Assert.assertEquals(artifact.getName(), name, "Unexpected artifact name.");
        Assert.assertEquals(artifact.getType(), type, "Unexpected artifact type.");
        Assert.assertEquals(artifact.getSha1(), sha1, "Unexpected artifact SHA1 checksum.");
        Assert.assertEquals(artifact.getSha256(), sha256, "Unexpected artifact SHA256 checksum.");
        Assert.assertEquals(artifact.getMd5(), md5, "Unexpected artifact MD5 checksum.");
        Assert.assertEquals(artifact.getRemotePath(), remotePath, "Unexpected artifact remote path.");
        Assert.assertEquals(artifact.getProperties(), properties, "Unexpected artifact properties.");
    }

    public void testEqualsAndHash() {
        //Properties are not included in equals\hash
        Properties properties = new Properties();

        Artifact artifact1 = new Artifact();
        artifact1.setName("1");
        artifact1.setType("11");
        artifact1.setSha1("111");
        artifact1.setSha256("11111");
        artifact1.setMd5("1111");
        artifact1.setProperties(properties);

        Artifact artifact2 = new Artifact();
        artifact2.setName("1");
        artifact2.setType("11");
        artifact2.setSha1("111");
        artifact2.setSha256("11111");
        artifact2.setMd5("1111");
        artifact2.setProperties(properties);

        Artifact artifact3 = new Artifact();
        artifact3.setName("13");
        artifact3.setType("113");
        artifact3.setSha1("1113");
        artifact3.setSha256("11133");
        artifact3.setMd5("11113");
        artifact3.setProperties(properties);

        Assert.assertEquals(artifact1, artifact2, "Expected equals == true for equivalent artifacts");
        Assert.assertFalse(artifact1.equals(artifact3), "Expected equals == false for non-equivalent artifacts");

        HashMap<Artifact, String> testMap = new HashMap<>();
        testMap.put(artifact1, "1");
        testMap.put(artifact2, "2");
        Assert.assertEquals(testMap.size(), 1, "Expected same hashcode for equal artifacts");
        Assert.assertEquals(testMap.values().iterator().next(), "2", "Expected insertion of equivalent artifact to" +
                " map to replace old one");

        testMap.put(artifact3, "3");
        Assert.assertEquals(testMap.size(), 2, "Expected artifact to not overwrite existing non-equivalent artifact");
    }
}