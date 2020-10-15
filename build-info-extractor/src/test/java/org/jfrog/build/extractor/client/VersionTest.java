package org.jfrog.build.extractor.client;

import org.jfrog.build.client.Version;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;


/**
 * @author Noam Y. Tenne
 */
@Test
public class VersionTest {

    @Test
    public void testOnlyText() {
        Version version = new Version("gwhwrthw");
        assertTrue(version.isAtLeast(Version.NOT_FOUND));
        assertTrue(version.isAtLeast(new Version("1.1")));
        assertTrue(version.isAtLeast(new Version("1.1.2")));
        assertTrue(version.isAtLeast(new Version("1.2.3")));
        assertTrue(version.isAtLeast(new Version("1.4.5")));
        assertTrue(version.isAtLeast(new Version("1.4.5-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("1.4.x")));
        assertTrue(version.isAtLeast(new Version("1.4.x-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("1.4")));
        assertTrue(version.isAtLeast(new Version("1.4.4.6")));

        assertTrue(version.isAtLeast(new Version("2.1")));
        assertTrue(version.isAtLeast(new Version("2.1.2")));
        assertTrue(version.isAtLeast(new Version("2.1.2-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("2.1.x-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("2.1.x")));
        assertTrue(version.isAtLeast(new Version("2.2.3")));
        assertTrue(version.isAtLeast(new Version("2.2.3-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("2.2.x")));
        assertTrue(version.isAtLeast(new Version("2.2.x-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("2.4.5")));
        assertTrue(version.isAtLeast(new Version("2.4")));
        assertTrue(version.isAtLeast(new Version("2.4.2.4")));
        assertTrue(version.isAtLeast(new Version("2.momo")));
        assertTrue(version.isAtLeast(new Version("2-momo")));

        assertTrue(version.isAtLeast(new Version("3.1")));
        assertTrue(version.isAtLeast(new Version("3.1.2")));
        assertTrue(version.isAtLeast(new Version("3.1.2-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("3.1.x-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("3.1.x")));
        assertTrue(version.isAtLeast(new Version("3.2.3")));
        assertTrue(version.isAtLeast(new Version("3.4.5")));
        assertTrue(version.isAtLeast(new Version("3.4")));
        assertTrue(version.isAtLeast(new Version("asdfasg")));
    }

    @Test
    public void testShortNumericRelease() {
        Version version = new Version("2.2");
        assertTrue(version.isAtLeast(Version.NOT_FOUND));
        assertTrue(version.isAtLeast(new Version("1.1")));
        assertTrue(version.isAtLeast(new Version("1.1.2")));
        assertTrue(version.isAtLeast(new Version("1.2.3")));
        assertTrue(version.isAtLeast(new Version("1.4.5")));
        assertTrue(version.isAtLeast(new Version("1.4.5-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("1.4.x")));
        assertTrue(version.isAtLeast(new Version("1.4.x-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("1.4")));
        assertTrue(version.isAtLeast(new Version("1.4.4.6")));

        assertTrue(version.isAtLeast(new Version("2.1")));
        assertTrue(version.isAtLeast(new Version("2.1.2")));
        assertTrue(version.isAtLeast(new Version("2.1.2-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("2.1.x-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("2.1.x")));
        assertFalse(version.isAtLeast(new Version("2.2.3")));
        assertFalse(version.isAtLeast(new Version("2.2.3-SNAPSHOT")));
        assertFalse(version.isAtLeast(new Version("2.2.x")));
        assertFalse(version.isAtLeast(new Version("2.2.x-SNAPSHOT")));
        assertFalse(version.isAtLeast(new Version("2.4.5")));
        assertFalse(version.isAtLeast(new Version("2.4")));
        assertFalse(version.isAtLeast(new Version("2.4.2.4")));
        assertFalse(version.isAtLeast(new Version("2.momo")));
        assertFalse(version.isAtLeast(new Version("2-momo")));

        assertFalse(version.isAtLeast(new Version("3.1")));
        assertFalse(version.isAtLeast(new Version("3.1.2")));
        assertFalse(version.isAtLeast(new Version("3.1.2-SNAPSHOT")));
        assertFalse(version.isAtLeast(new Version("3.1.x-SNAPSHOT")));
        assertFalse(version.isAtLeast(new Version("3.1.x")));
        assertFalse(version.isAtLeast(new Version("3.2.3")));
        assertFalse(version.isAtLeast(new Version("3.4.5")));
        assertFalse(version.isAtLeast(new Version("3.4")));
        assertFalse(version.isAtLeast(new Version("asdfasg")));
    }

    @Test
    public void testNumericRelease() {
        Version version = new Version("2.2.3");
        assertTrue(version.isAtLeast(Version.NOT_FOUND));
        assertTrue(version.isAtLeast(new Version("1.1")));
        assertTrue(version.isAtLeast(new Version("1.1.2")));
        assertTrue(version.isAtLeast(new Version("1.2.3")));
        assertTrue(version.isAtLeast(new Version("1.4.5")));
        assertTrue(version.isAtLeast(new Version("1.4.5-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("1.4.x")));
        assertTrue(version.isAtLeast(new Version("1.4.x-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("1.4")));
        assertTrue(version.isAtLeast(new Version("1.4.4.6")));

        assertTrue(version.isAtLeast(new Version("2.1")));
        assertTrue(version.isAtLeast(new Version("2.1.2")));
        assertTrue(version.isAtLeast(new Version("2.1.2-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("2.1.x-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("2.1.x")));
        assertTrue(version.isAtLeast(new Version("2.2.3")));
        assertFalse(version.isAtLeast(new Version("2.2.3-SNAPSHOT")));
        assertFalse(version.isAtLeast(new Version("2.2.x")));
        assertFalse(version.isAtLeast(new Version("2.2.x-SNAPSHOT")));
        assertFalse(version.isAtLeast(new Version("2.4.5")));
        assertFalse(version.isAtLeast(new Version("2.4")));
        assertFalse(version.isAtLeast(new Version("2.4.2.4")));
        assertFalse(version.isAtLeast(new Version("2.momo")));
        assertFalse(version.isAtLeast(new Version("2-momo")));

        assertFalse(version.isAtLeast(new Version("3.1")));
        assertFalse(version.isAtLeast(new Version("3.1.2")));
        assertFalse(version.isAtLeast(new Version("3.1.2-SNAPSHOT")));
        assertFalse(version.isAtLeast(new Version("3.1.x-SNAPSHOT")));
        assertFalse(version.isAtLeast(new Version("3.1.x")));
        assertFalse(version.isAtLeast(new Version("3.2.3")));
        assertFalse(version.isAtLeast(new Version("3.4.5")));
        assertFalse(version.isAtLeast(new Version("3.4")));
        assertFalse(version.isAtLeast(new Version("asdfasg")));
    }

    @Test
    public void testNumericSnapshot() {
        Version version = new Version("2.2.3-SNAPSHOT");
        assertTrue(version.isAtLeast(Version.NOT_FOUND));
        assertTrue(version.isAtLeast(new Version("1.1")));
        assertTrue(version.isAtLeast(new Version("1.1.2")));
        assertTrue(version.isAtLeast(new Version("1.2.3")));
        assertTrue(version.isAtLeast(new Version("1.4.5")));
        assertTrue(version.isAtLeast(new Version("1.4.5-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("1.4.x")));
        assertTrue(version.isAtLeast(new Version("1.4.x-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("1.4")));
        assertTrue(version.isAtLeast(new Version("1.4.4.6")));

        assertTrue(version.isAtLeast(new Version("2.1")));
        assertTrue(version.isAtLeast(new Version("2.1.2")));
        assertTrue(version.isAtLeast(new Version("2.1.2-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("2.1.x-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("2.1.x")));
        assertTrue(version.isAtLeast(new Version("2.2.3")));
        assertTrue(version.isAtLeast(new Version("2.2.3-SNAPSHOT")));
        assertFalse(version.isAtLeast(new Version("2.2.x")));
        assertFalse(version.isAtLeast(new Version("2.2.x-SNAPSHOT")));
        assertFalse(version.isAtLeast(new Version("2.4.5")));
        assertFalse(version.isAtLeast(new Version("2.4")));
        assertFalse(version.isAtLeast(new Version("2.4.2.4")));
        assertFalse(version.isAtLeast(new Version("2.momo")));
        assertFalse(version.isAtLeast(new Version("2-momo")));

        assertFalse(version.isAtLeast(new Version("3.1")));
        assertFalse(version.isAtLeast(new Version("3.1.2")));
        assertFalse(version.isAtLeast(new Version("3.1.2-SNAPSHOT")));
        assertFalse(version.isAtLeast(new Version("3.1.x-SNAPSHOT")));
        assertFalse(version.isAtLeast(new Version("3.1.x")));
        assertFalse(version.isAtLeast(new Version("3.2.3")));
        assertFalse(version.isAtLeast(new Version("3.4.5")));
        assertFalse(version.isAtLeast(new Version("3.4")));
        assertFalse(version.isAtLeast(new Version("asdfasg")));
    }

    @Test
    public void testAlphaNumeric() {
        Version version = new Version("2.2.x");
        assertTrue(version.isAtLeast(Version.NOT_FOUND));
        assertTrue(version.isAtLeast(new Version("1.1")));
        assertTrue(version.isAtLeast(new Version("1.1.2")));
        assertTrue(version.isAtLeast(new Version("1.2.3")));
        assertTrue(version.isAtLeast(new Version("1.4.5")));
        assertTrue(version.isAtLeast(new Version("1.4.5-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("1.4.x")));
        assertTrue(version.isAtLeast(new Version("1.4.x-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("1.4")));
        assertTrue(version.isAtLeast(new Version("1.4.4.6")));

        assertTrue(version.isAtLeast(new Version("2.1")));
        assertTrue(version.isAtLeast(new Version("2.1.2")));
        assertTrue(version.isAtLeast(new Version("2.1.2-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("2.1.x-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("2.1.x")));
        assertTrue(version.isAtLeast(new Version("2.2.3")));
        assertTrue(version.isAtLeast(new Version("2.2.3-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("2.2.x")));
        assertFalse(version.isAtLeast(new Version("2.2.x-SNAPSHOT")));
        assertFalse(version.isAtLeast(new Version("2.4.5")));
        assertFalse(version.isAtLeast(new Version("2.4")));
        assertFalse(version.isAtLeast(new Version("2.4.2.4")));
        assertFalse(version.isAtLeast(new Version("2.momo")));
        assertFalse(version.isAtLeast(new Version("2-momo")));

        assertFalse(version.isAtLeast(new Version("3.1")));
        assertFalse(version.isAtLeast(new Version("3.1.2")));
        assertFalse(version.isAtLeast(new Version("3.1.2-SNAPSHOT")));
        assertFalse(version.isAtLeast(new Version("3.1.x-SNAPSHOT")));
        assertFalse(version.isAtLeast(new Version("3.1.x")));
        assertFalse(version.isAtLeast(new Version("3.2.3")));
        assertFalse(version.isAtLeast(new Version("3.4.5")));
        assertFalse(version.isAtLeast(new Version("3.4")));
        assertFalse(version.isAtLeast(new Version("asdfasg")));
    }

    @Test
    public void testAlphaNumericSnapshot() {
        Version version = new Version("2.2.x-SNAPSHOT");
        assertTrue(version.isAtLeast(Version.NOT_FOUND));
        assertTrue(version.isAtLeast(new Version("1.1")));
        assertTrue(version.isAtLeast(new Version("1.1.2")));
        assertTrue(version.isAtLeast(new Version("1.2.3")));
        assertTrue(version.isAtLeast(new Version("1.4.5")));
        assertTrue(version.isAtLeast(new Version("1.4.5-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("1.4.x")));
        assertTrue(version.isAtLeast(new Version("1.4.x-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("1.4")));
        assertTrue(version.isAtLeast(new Version("1.4.4.6")));

        assertTrue(version.isAtLeast(new Version("2.1")));
        assertTrue(version.isAtLeast(new Version("2.1.2")));
        assertTrue(version.isAtLeast(new Version("2.1.2-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("2.1.x-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("2.1.x")));
        assertTrue(version.isAtLeast(new Version("2.2.3")));
        assertTrue(version.isAtLeast(new Version("2.2.3-SNAPSHOT")));
        assertTrue(version.isAtLeast(new Version("2.2.x")));
        assertTrue(version.isAtLeast(new Version("2.2.x-SNAPSHOT")));
        assertFalse(version.isAtLeast(new Version("2.4.5")));
        assertFalse(version.isAtLeast(new Version("2.4")));
        assertFalse(version.isAtLeast(new Version("2.4.2.4")));
        assertFalse(version.isAtLeast(new Version("2.momo")));
        assertFalse(version.isAtLeast(new Version("2-momo")));

        assertFalse(version.isAtLeast(new Version("3.1")));
        assertFalse(version.isAtLeast(new Version("3.1.2")));
        assertFalse(version.isAtLeast(new Version("3.1.2-SNAPSHOT")));
        assertFalse(version.isAtLeast(new Version("3.1.x-SNAPSHOT")));
        assertFalse(version.isAtLeast(new Version("3.1.x")));
        assertFalse(version.isAtLeast(new Version("3.2.3")));
        assertFalse(version.isAtLeast(new Version("3.4.5")));
        assertFalse(version.isAtLeast(new Version("3.4")));
        assertFalse(version.isAtLeast(new Version("asdfasg")));
    }
}
