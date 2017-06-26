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

import org.jfrog.build.api.Artifact;
import org.testng.annotations.Test;

import java.util.Properties;

import static org.testng.Assert.*;

/**
 * Tests the behavior of the artifact builder class
 *
 * @author Noam Y. Tenne
 */
@Test
public class ArtifactBuilderTest {

    /**
     * Validates the artifact values when using the defaults
     */
    public void testDefaultBuild() {
        Artifact artifact = new ArtifactBuilder("name").build();

        assertEquals(artifact.getName(), "name", "Unexpected artifact name.");
        assertNull(artifact.getType(), "Default artifact type.");
        assertNull(artifact.getSha1(), "Default artifact SHA1 checksum should be null.");
        assertNull(artifact.getSha256(), "Default artifact SHA256 checksum should be null.");
        assertNull(artifact.getMd5(), "Default artifact MD5 checksum should be null.");
        assertNull(artifact.getProperties(), "Default artifact properties should be null.");
    }

    /**
     * Validates the artifact values after using the builder setters
     */
    public void testBuilderSetters() {
        String name = "moo";
        String type = "bob";
        String sha1 = "pop";
        String sha256 = "lol";
        String md5 = "shmop";
        Properties properties = new Properties();

        Artifact artifact = new ArtifactBuilder(name).type(type).sha1(sha1).sha256(sha256).md5(md5).properties(properties)
                .build();

        assertEquals(artifact.getName(), name, "Unexpected artifact ID.");
        assertEquals(artifact.getType(), type, "Unexpected artifact type.");
        assertEquals(artifact.getSha1(), sha1, "Unexpected artifact SHA1 checksum.");
        assertEquals(artifact.getSha256(), sha256, "Unexpected artifact SHA256 checksum.");
        assertEquals(artifact.getMd5(), md5, "Unexpected artifact SHA1 checksum.");
        assertEquals(artifact.getProperties(), properties, "Unexpected artifact properties.");
        assertTrue(artifact.getProperties().isEmpty(), "Artifact properties list should not have been populated.");
    }

    /**
     * Validates the artifact values after using the builder add methods
     */
    public void testBuilderAddMethods() {
        String propertyKey = "key";
        String propertyValue = "value";

        Artifact artifact = new ArtifactBuilder("name").addProperty(propertyKey, propertyValue).build();
        assertTrue(artifact.getProperties().containsKey(propertyKey), "An artifact property should have been added.");
        assertEquals(artifact.getProperties().get(propertyKey), propertyValue, "Unexpected artifact property value.");
    }
}