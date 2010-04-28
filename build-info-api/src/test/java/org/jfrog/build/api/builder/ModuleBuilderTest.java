/*
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.build.api.builder;

import com.google.common.collect.Lists;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Properties;

import static org.testng.Assert.*;

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

        assertNull(module.getId(), "Default module ID should be null.");
        assertNull(module.getArtifacts(), "Default module artifacts list should be null.");
        assertNull(module.getDependencies(), "Default module dependencies list should be null.");
        assertNull(module.getProperties(), "Default module properties should be null.");
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