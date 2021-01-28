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

import org.apache.commons.lang.ArrayUtils;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.util.CommonUtils;
import org.testng.annotations.Test;

import java.util.Properties;
import java.util.Set;

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
        assertNull(dependency.getSha1(), "Default dependency SHA1 checksum should be null.");
        assertNull(dependency.getSha256(), "Default dependency SHA256 checksum should be null.");
        assertNull(dependency.getMd5(), "Default dependency MD5 checksum should be null.");
        assertNull(dependency.getRequestedBy(), "Default dependency requested by should not have been initialized.");
        assertNull(dependency.getProperties(), "Default dependency properties should be null.");
    }

    /**
     * Validates the dependency values after using the builder setters
     */
    public void testBuilderSetters() {
        String id = "moo";
        String type = "bob";
        Set<String> scopes = CommonUtils.newHashSet("mitzi");
        String sha1 = "pop";
        String sha256 = "lol";
        String md5 = "shmop";
        String[][] requestedBy = new String[][]{{"pitzi"}};
        Properties properties = new Properties();

        Dependency dependency = new DependencyBuilder().id(id).type(type).scopes(scopes).sha1(sha1).md5(md5).sha256(sha256)
                .requestedBy(requestedBy).properties(properties).build();

        assertEquals(dependency.getId(), id, "Unexpected dependency ID.");
        assertEquals(dependency.getType(), type, "Unexpected dependency type.");
        assertEquals(dependency.getScopes(), scopes, "Unexpected dependency scopes.");
        assertEquals(dependency.getSha1(), sha1, "Unexpected dependency SHA1 checksum.");
        assertEquals(dependency.getSha256(), sha256, "Unexpected dependency SHA256 checksum.");
        assertEquals(dependency.getMd5(), md5, "Unexpected dependency SHA1 checksum.");
        assertEquals(dependency.getRequestedBy(), requestedBy, "Unexpected dependency requested by.");
        assertEquals(dependency.getProperties(), properties, "Unexpected dependency properties.");
        assertTrue(dependency.getProperties().isEmpty(), "Dependency properties list should not have been populated.");
    }

    /**
     * Validates the dependency values after using the builder add methods
     */
    public void testBuilderAddMethods() {
        String propertyKey = "keyA";
        String propertyValue = "valueA";

        Dependency dependency = new DependencyBuilder().addProperty(propertyKey, propertyValue).build();
        assertTrue(dependency.getProperties().containsKey(propertyKey), "A dependency property should have been added.");
        assertEquals(dependency.getProperties().get(propertyKey), propertyValue, "Unexpected dependency property value.");
    }

    public void testBuilderAddRequestedBy() {
        String[] requestedByA = {"A", "to", "module", "root", "moduleID"};
        String[] requestedByB = {"B", "C", "to", "module", "root", "moduleID"};

        Dependency dependency = new DependencyBuilder().addRequestedBy(requestedByA).addRequestedBy(requestedByB).build();
        String[][] requestedByList = dependency.getRequestedBy();
        assertEquals(ArrayUtils.getLength(requestedByList), 2);
        assertEquals(requestedByList[0], requestedByA, "Unexpected dependency requirement.");
        assertEquals(requestedByList[1], requestedByB, "Unexpected dependency requirement.");
    }
}