package org.artifactory.build.api.builder;

import com.google.common.collect.Lists;
import org.artifactory.build.api.Dependency;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Properties;

import static org.testng.Assert.*;

/**
 * Tests the behavior of the dependency builder class
 *
 * @author Noam Y. Tenne
 */
@Test
public class DependencyBuilderTest {

    /**
     * Validates the dependency values when using the defaults
     */
    public void testDefaultBuild() {
        Dependency dependency = new DependencyBuilder().build();

        assertNull(dependency.getId(), "Unexpected default dependency ID.");
        assertNull(dependency.getType(), "Unexpected default dependency type.");
        assertNull(dependency.getScopes(), "Default dependency scopes should not have been initialized.");
        assertEquals(dependency.getSha1(), "", "Unexpected default dependency SHA1 checksum.");
        assertEquals(dependency.getMd5(), "", "Unexpected default dependency MD5 checksum.");
        assertNull(dependency.getRequiredBy(), "Default dependency required by should not have been initialized.");
        assertNull(dependency.getProperties(), "Default dependency properties should be null.");
    }

    /**
     * Validates the dependency values after using the builder setters
     */
    public void testBuilderSetters() {
        String id = "moo";
        String type = "bob";
        List<String> scopes = Lists.newArrayList("mitzi");
        String sha1 = "pop";
        String md5 = "shmop";
        List<String> requiredBy = Lists.newArrayList("pitzi");
        Properties properties = new Properties();

        Dependency dependency = new DependencyBuilder().id(id).type(type).scopes(scopes).sha1(sha1).md5(md5).
                requiredBy(requiredBy).properties(properties).build();

        assertEquals(dependency.getId(), id, "Unexpected dependency ID.");
        assertEquals(dependency.getType(), type, "Unexpected dependency type.");
        assertEquals(dependency.getScopes(), scopes, "Unexpected dependency scopes.");
        assertEquals(dependency.getSha1(), sha1, "Unexpected dependency SHA1 checksum.");
        assertEquals(dependency.getMd5(), md5, "Unexpected dependency SHA1 checksum.");
        assertEquals(dependency.getRequiredBy(), requiredBy, "Unexpected dependency required by.");
        assertEquals(dependency.getProperties(), properties, "Unexpected dependency properties.");
        assertTrue(dependency.getProperties().isEmpty(), "Dependency properties list should not have been populated.");
    }

    /**
     * Validates the dependency values after using the builder add methods
     */
    public void testBuilderAddMethods() {
        String requiredBy = "requiredMoo";
        String propertyKey = "key";
        String propertyValue = "value";

        Dependency dependency = new DependencyBuilder().addRequiredBy(requiredBy).
                addProperty(propertyKey, propertyValue).build();
        List<String> requiredByList = dependency.getRequiredBy();
        assertFalse(requiredByList.isEmpty(), "A dependency requirement should have been added.");
        assertEquals(requiredByList.get(0), requiredBy, "Unexpected dependency requirement.");
        assertTrue(dependency.getProperties().containsKey(propertyKey),
                "A dependency property should have been added.");
        assertEquals(dependency.getProperties().get(propertyKey), propertyValue,
                "Unexpected dependency property value.");
    }
}