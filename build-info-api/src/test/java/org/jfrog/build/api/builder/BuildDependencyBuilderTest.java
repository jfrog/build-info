package org.jfrog.build.api.builder;

import org.jfrog.build.api.Build;
import org.jfrog.build.api.builder.dependency.BuildDependencyBuilder;
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

    private final String timestamp = new SimpleDateFormat(Build.STARTED_FORMAT).format(new Date());

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