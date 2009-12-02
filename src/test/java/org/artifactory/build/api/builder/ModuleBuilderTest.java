package org.artifactory.build.api.builder;

import com.google.common.collect.Lists;
import org.artifactory.build.api.Artifact;
import org.artifactory.build.api.Dependency;
import org.artifactory.build.api.Module;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Properties;

/**
 * Tests the behavior of the module builder class
 *
 * @author Noam Y. Tenne
 */
@Test
public class ModuleBuilderTest {

    /**
     * Validates the module values when using the defaults
     */
    public void testDefaultBuild() {
        Module module = new ModuleBuilder().build();

        assertEquals(module.getId(), "", "Unexpected default module ID.");
        assertTrue(module.getArtifacts().isEmpty(),
                "Default module artifacts list should not have been populated.");
        assertTrue(module.getDependencies().isEmpty(),
                "Default module dependencies list should not have been populated.");
        assertNotNull(module.getProperties(), "Default module properties should not be null.");
        assertTrue(module.getProperties().isEmpty(), "Default module properties list should not have been populated.");
    }

    /**
     * Validates the module values after using the builder setters
     */
    public void testBuilderSetters() {
        String id = "moo";
        List<Artifact> artifacts = Lists.newArrayList();
        List<Dependency> dependencies = Lists.newArrayList();
        Properties properties = new Properties();

        Module module = new ModuleBuilder().id(id).artifacts(artifacts).dependencies(dependencies).
                properties(properties).build();
        assertEquals(module.getId(), id, "Unexpected module ID.");
        assertEquals(module.getArtifacts(), artifacts, "Unexpected module artifacts.");
        assertTrue(module.getArtifacts().isEmpty(), "Module artifacts list should not have been populated.");
        assertEquals(module.getDependencies(), dependencies, "Unexpected module dependencies.");
        assertTrue(module.getDependencies().isEmpty(), "Module dependencies list should not have been populated.");
        assertEquals(module.getProperties(), properties, "Unexpected module properties.");
        assertTrue(module.getProperties().isEmpty(), "Module properties list should not have been populated.");
    }

    /**
     * Validates the module values after using the builder add methods
     */
    public void testBuilderAddMethods() {
        Artifact artifact = new Artifact();
        Dependency dependency = new Dependency();
        String propertyKey = "key";
        String propertyValue = "value";

        Module module = new ModuleBuilder().addArtifact(artifact).addDependency(dependency).
                addProperty(propertyKey, propertyValue).build();
        assertFalse(module.getArtifacts().isEmpty(), "A module artifact should have been added.");
        assertEquals(module.getArtifacts().get(0), artifact, "Unexpected module artifact.");
        assertFalse(module.getDependencies().isEmpty(), "A module dependency should have been added.");
        assertEquals(module.getDependencies().get(0), dependency, "Unexpected dependency artifact.");
        assertTrue(module.getProperties().containsKey(propertyKey), "A module property should have been added.");
        assertEquals(module.getProperties().get(propertyKey), propertyValue, "Unexpected module property value.");
    }
}