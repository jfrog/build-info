package org.jfrog.build.extractor.ci;

import org.apache.commons.lang3.ArrayUtils;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.util.CommonUtils;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

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
        assertNull(dependency.getSha256(), "Dependency SHA256 checksum should have not been initialized.");
        assertNull(dependency.getMd5(), "Dependency MD5 checksum should have not been initialized.");
        assertNull(dependency.getRequestedBy(), "Dependency requested by should have not been initialized.");
    }

    /**
     * Validates the dependency values after using the dependency setters
     */
    public void testSetters() {
        String id = "moo";
        String type = "bob";
        Set<String> scopes = CommonUtils.newHashSet("mitzi");
        String sha1 = "pop";
        String sha256 = "lol";
        String md5 = "shmop";
        String[][] requestedBy = new String[][]{{"pitzi"}};

        Dependency dependency = new Dependency();
        dependency.setId(id);
        dependency.setType(type);
        dependency.setScopes(scopes);
        dependency.setSha1(sha1);
        dependency.setSha256(sha256);
        dependency.setMd5(md5);
        dependency.setRequestedBy(requestedBy);

        assertEquals(dependency.getId(), id, "Unexpected dependency ID.");
        assertEquals(dependency.getType(), type, "Unexpected dependency type.");
        assertEquals(dependency.getSha1(), sha1, "Unexpected dependency SHA1 checksum.");
        assertEquals(dependency.getSha256(), sha256, "Unexpected dependency SHA256 checksum.");
        assertEquals(dependency.getMd5(), md5, "Unexpected dependency MD5 checksum.");
        assertEquals(dependency.getScopes(), scopes, "Unexpected dependency scopes.");
        assertEquals(dependency.getRequestedBy(), requestedBy, "Unexpected dependency requested by.");
    }

    public void testAddRequestedBy() {
        // Create a dependency
        Dependency dependency = new Dependency();
        assertTrue(ArrayUtils.isEmpty(dependency.getRequestedBy()));

        // Add requested by A
        String[] parentA = new String[]{"a", "b", "c"};
        dependency.addRequestedBy(parentA);
        assertEquals(ArrayUtils.getLength(dependency.getRequestedBy()), 1);
        assertEquals(dependency.getRequestedBy()[0], parentA);

        // Add requested by B
        String[] parentB = new String[]{"b", "c", "d", "e"};
        dependency.addRequestedBy(parentB);
        assertEquals(ArrayUtils.getLength(dependency.getRequestedBy()), 2);
        assertEquals(dependency.getRequestedBy()[0], parentA);
        assertEquals(dependency.getRequestedBy()[1], parentB);

        // Add requested by C
        String[] parentC = new String[]{"c", "d"};
        dependency.addRequestedBy(parentC);
        assertEquals(ArrayUtils.getLength(dependency.getRequestedBy()), 3);
        assertEquals(dependency.getRequestedBy()[0], parentA);
        assertEquals(dependency.getRequestedBy()[1], parentB);
        assertEquals(dependency.getRequestedBy()[2], parentC);
    }

    public void testEqualsAndHash() {
        //Properties are not included in equals\hash
        Properties properties = new Properties();

        Dependency dependency1 = new Dependency();
        dependency1.setId("1");
        dependency1.setType("11");
        dependency1.setSha1("111");
        dependency1.setSha256("11111");
        dependency1.setMd5("1111");
        dependency1.setRequestedBy(new String[][]{{"11111"}});
        dependency1.setScopes(CommonUtils.newHashSet("1", "11"));
        dependency1.setProperties(properties);

        Dependency dependency2 = new Dependency();
        dependency2.setId("1");
        dependency2.setType("11");
        dependency2.setSha1("111");
        dependency2.setSha256("11111");
        dependency2.setMd5("1111");
        dependency2.setRequestedBy(new String[][]{{"11111"}});
        dependency2.setScopes(CommonUtils.newHashSet("1", "11"));
        dependency2.setProperties(properties);

        Dependency dependency3 = new Dependency();
        dependency3.setId("3");
        dependency3.setType("33");
        dependency3.setSha1("333");
        dependency3.setSha256("33333");
        dependency3.setMd5("3333");
        dependency3.setRequestedBy(new String[][]{{"33333"}});
        dependency3.setScopes(CommonUtils.newHashSet("333333", "3333333"));
        dependency3.setProperties(properties);

        assertEquals(dependency1, dependency2, "Expected equals == true for equivalent artifacts");
        assertNotEquals(dependency3, dependency1, "Expected equals == false for non-equivalent artifacts");

        HashMap<Dependency, String> testMap = new HashMap<>();
        testMap.put(dependency1, "1");
        testMap.put(dependency2, "2");
        assertEquals(testMap.size(), 1, "Expected same hashcode for equal artifacts");
        assertEquals(testMap.values().iterator().next(), "2", "Expected insertion of equivalent artifact to" +
                " map to replace old one");

        testMap.put(dependency3, "3");
        assertEquals(testMap.size(), 2, "Expected artifact to not overwrite existing non-equivalent artifact");
    }
}