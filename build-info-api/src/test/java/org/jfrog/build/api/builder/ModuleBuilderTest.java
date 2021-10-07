/*
 * Copyright (C) 2011 JFrog Ltd.
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

import org.jfrog.build.api.ci.Artifact;
import org.jfrog.build.api.ci.Dependency;
import org.jfrog.build.api.ci.Module;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.jfrog.build.api.builder.ModuleType.BUILD;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

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
        Module module = new ModuleBuilder().id("test").build();

        assertEquals(module.getId(), "test", "Default module ID cannot be null.");
        assertNull(module.getType(), "Default module type should be null.");
        assertNull(module.getArtifacts(), "Default module artifacts list should be null.");
        assertNull(module.getDependencies(), "Default module dependencies list should be null.");
        assertNull(module.getSha1(), "Default module sha1 should be null.");
        assertNull(module.getMd5(), "Default module md5 should be null.");
        assertNull(module.getProperties(), "Default module properties should be null.");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullModuleIdBuild() {
        new ModuleBuilder().build();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testEmptyModuleIdBuild() {
        new ModuleBuilder().id(" ").build();
    }

    /**
     * Validates the module values after using the builder setters
     */
    public void testBuilderSetters() {
        String id = "moo";
        String repo = "test-repo";
        String sha1 = "abcd";
        String md5 = "efgh";
        List<Artifact> artifacts = new ArrayList<>();
        List<Dependency> dependencies = new ArrayList<>();
        Properties properties = new Properties();

        Module module = new ModuleBuilder()
                .type(BUILD).id(id).repository(repo).artifacts(artifacts).dependencies(dependencies)
                .sha1(sha1).md5(md5).properties(properties).build();
        assertEquals(module.getType(), "build", "Unexpected module type.");
        assertEquals(module.getId(), id, "Unexpected module ID.");
        assertEquals(module.getRepository(), repo, "Unexpected module repository.");
        assertEquals(module.getArtifacts(), artifacts, "Unexpected module artifacts.");
        assertTrue(module.getArtifacts().isEmpty(), "Module artifacts list should not have been populated.");
        assertEquals(module.getDependencies(), dependencies, "Unexpected module dependencies.");
        assertTrue(module.getDependencies().isEmpty(), "Module dependencies list should not have been populated.");
        assertEquals(module.getSha1(), sha1, "Unexpected module sha1.");
        assertEquals(module.getMd5(), md5, "Unexpected module md5.");
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

        Module module = new ModuleBuilder().id("test").addArtifact(artifact).addDependency(dependency).
                addProperty(propertyKey, propertyValue).build();
        assertEquals(module.getId(), "test", "Unexpected module id");
        assertFalse(module.getArtifacts().isEmpty(), "A module artifact should have been added.");
        assertEquals(module.getArtifacts().get(0), artifact, "Unexpected module artifact.");
        assertFalse(module.getDependencies().isEmpty(), "A module dependency should have been added.");
        assertEquals(module.getDependencies().get(0), dependency, "Unexpected dependency artifact.");
        assertTrue(module.getProperties().containsKey(propertyKey), "A module property should have been added.");
        assertEquals(module.getProperties().get(propertyKey), propertyValue, "Unexpected module property value.");
    }
}