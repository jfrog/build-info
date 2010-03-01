package org.artifactory.build.api;

import com.google.common.collect.Lists;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

/**
 * Tests the behavior of the module class
 *
 * @author Noam Y. Tenne
 */
@Test
public class ModuleTest {

    /**
     * Validates the module values after initializing the default constructor
     */
    public void testEmptyConstructor() {
        Module module = new Module();

        assertNull(module.getId(), "Module ID should have not been initialized.");
        assertNull(module.getArtifacts(), "Module artifacts should have not been initialized.");
        assertNull(module.getDependencies(), "Module dependencies should have not been initialized.");
    }

    /**
     * Validates the module values after using the module setters
     */
    public void testSetters() {
        String id = "moo";
        List<Artifact> artifacts = Lists.newArrayList();
        List<Dependency> dependencies = Lists.newArrayList();

        Module module = new Module();
        module.setId(id);
        module.setArtifacts(artifacts);
        module.setDependencies(dependencies);

        assertEquals(module.getId(), id, "Unexpected module ID.");
        assertEquals(module.getArtifacts(), artifacts, "Unexpected module artifacts.");
        assertTrue(module.getArtifacts().isEmpty(), "Module artifact list should not have been populated.");
        assertEquals(module.getDependencies(), dependencies, "Unexpected module dependencies.");
        assertTrue(module.getDependencies().isEmpty(), "Module dependencies list should not have been populated.");
    }
}