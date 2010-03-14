package org.jfrog.build.api;

import com.google.common.collect.Lists;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * Tests the behavior of the dependency class
 *
 * @author Noam Y. Tenne
 */
@Test
public class DependencyTest {

    /**
     * Validates the dependency values after initializing the default constructor
     */
    public void testEmptyConstructor() {
        Dependency dependency = new Dependency();

        assertNull(dependency.getId(), "Dependency ID should have not been initialized.");
        assertNull(dependency.getType(), "Dependency type should have not been initialized.");
        assertNull(dependency.getScopes(), "Dependency scopes should have not been initialized.");
        assertNull(dependency.getSha1(), "Dependency SHA1 checksum should have not been initialized.");
        assertNull(dependency.getMd5(), "Dependency MD5 checksum should have not been initialized.");
        assertNull(dependency.getRequiredBy(), "Dependency required by should have not been initialized.");
    }

    /**
     * Validates the dependency values after using the dependency setters
     */
    public void testSetters() {
        String id = "moo";
        String type = "bob";
        List<String> scopes = Lists.newArrayList("mitzi");
        String sha1 = "pop";
        String md5 = "shmop";
        List<String> requiredBy = Lists.newArrayList("pitzi");

        Dependency dependency = new Dependency();
        dependency.setId(id);
        dependency.setType(type);
        dependency.setScopes(scopes);
        dependency.setSha1(sha1);
        dependency.setMd5(md5);
        dependency.setRequiredBy(requiredBy);

        assertEquals(dependency.getId(), id, "Unexpected dependency ID.");
        assertEquals(dependency.getType(), type, "Unexpected dependency type.");
        assertEquals(dependency.getSha1(), sha1, "Unexpected dependency SHA1 checksum.");
        assertEquals(dependency.getMd5(), md5, "Unexpected dependency MD5 checksum.");
        assertEquals(dependency.getScopes(), scopes, "Unexpected dependency scopes.");
        assertEquals(dependency.getRequiredBy(), requiredBy, "Unexpected dependency required by.");
    }
}