/*
 * Copyright (C) 2012 JFrog Ltd.
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

import org.jfrog.build.api.builder.dependency.BuildDependencyBuilder;
import org.jfrog.build.api.ci.BuildInfo;
import org.jfrog.build.api.dependency.BuildDependency;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * Tests the behavior of the module builder class
 *
 * @author Noam Y. Tenne
 */
@Test
public class BuildDependencyBuilderTest {

    private final String timestamp = new SimpleDateFormat(BuildInfo.STARTED_FORMAT).format(new Date());

    /**
     * Validates the module values when using the defaults
     */
    public void testDefaultBuild() {
        BuildDependency buildDependency = new BuildDependencyBuilder().name("foo").number("123").started(timestamp).build();
        assertNull(buildDependency.getUrl(), "URI should have not been initialized.");
    }

    /**
     * Validates the build dependency values after using the builder setters
     */
    public void testBuilderSetters() {
        String name = "foo";
        String number = "123";
        String url = "http://myhostA.com/artifactory/builds/foo/123/";

        BuildDependency buildDependency = new BuildDependencyBuilder().name(name).number(number).started(timestamp).url(url).build();

        assertEquals(buildDependency.getName(), name, "Unexpected name.");
        assertEquals(buildDependency.getNumber(), number, "Unexpected number.");
        assertEquals(buildDependency.getStarted(), timestamp, "Unexpected started.");
        assertEquals(buildDependency.getUrl(), url, "Unexpected url.");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullDateForTimestamp() {
        new BuildDependencyBuilder().startedDate(null);
    }
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBuildWithNullForTimestamp() {
        new BuildDependencyBuilder().started(null).build();
    }
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBuildWithNullForName() {
        new BuildDependencyBuilder().name(null).build();
    }
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBuildWithNullForNumber() {
        new BuildDependencyBuilder().number(null).build();
    }
}