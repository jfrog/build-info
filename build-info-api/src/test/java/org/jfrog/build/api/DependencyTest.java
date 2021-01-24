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

package org.jfrog.build.api;

import org.jfrog.build.api.util.CommonUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

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
        assertNull(dependency.getSha256(), "Dependency SHA256 checksum should have not been initialized.");
        assertNull(dependency.getMd5(), "Dependency MD5 checksum should have not been initialized.");
        assertNull(dependency.getRequiredBy(), "Dependency required by should have not been initialized.");
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
        String[][] requiredBy = new String[][]{{"pitzi"}};

        Dependency dependency = new Dependency();
        dependency.setId(id);
        dependency.setType(type);
        dependency.setScopes(scopes);
        dependency.setSha1(sha1);
        dependency.setSha256(sha256);
        dependency.setMd5(md5);
        dependency.setRequiredBy(requiredBy);

        assertEquals(dependency.getId(), id, "Unexpected dependency ID.");
        assertEquals(dependency.getType(), type, "Unexpected dependency type.");
        assertEquals(dependency.getSha1(), sha1, "Unexpected dependency SHA1 checksum.");
        assertEquals(dependency.getSha256(), sha256, "Unexpected dependency SHA256 checksum.");
        assertEquals(dependency.getMd5(), md5, "Unexpected dependency MD5 checksum.");
        assertEquals(dependency.getScopes(), scopes, "Unexpected dependency scopes.");
        assertEquals(dependency.getRequiredBy(), requiredBy, "Unexpected dependency required by.");
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
        dependency1.setRequiredBy(new String[][]{{"11111"}});
        dependency1.setScopes(CommonUtils.newHashSet("1", "11"));
        dependency1.setProperties(properties);

        Dependency dependency2 = new Dependency();
        dependency2.setId("1");
        dependency2.setType("11");
        dependency2.setSha1("111");
        dependency2.setSha256("11111");
        dependency2.setMd5("1111");
        dependency2.setRequiredBy(new String[][]{{"11111"}});
        dependency2.setScopes(CommonUtils.newHashSet("1", "11"));
        dependency2.setProperties(properties);

        Dependency dependency3 = new Dependency();
        dependency3.setId("3");
        dependency3.setType("33");
        dependency3.setSha1("333");
        dependency3.setSha256("33333");
        dependency3.setMd5("3333");
        dependency3.setRequiredBy(new String[][]{{"33333"}});
        dependency3.setScopes(CommonUtils.newHashSet("333333", "3333333"));
        dependency3.setProperties(properties);

        Assert.assertEquals(dependency1, dependency2, "Expected equals == true for equivalent artifacts");
        Assert.assertNotEquals(dependency3, dependency1, "Expected equals == false for non-equivalent artifacts");

        HashMap<Dependency, String> testMap = new HashMap<>();
        testMap.put(dependency1, "1");
        testMap.put(dependency2, "2");
        Assert.assertEquals(testMap.size(), 1, "Expected same hashcode for equal artifacts");
        Assert.assertEquals(testMap.values().iterator().next(), "2", "Expected insertion of equivalent artifact to" +
                " map to replace old one");

        testMap.put(dependency3, "3");
        Assert.assertEquals(testMap.size(), 2, "Expected artifact to not overwrite existing non-equivalent artifact");
    }
}