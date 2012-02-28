package org.jfrog.build.api;

import org.jfrog.build.api.dependency.BuildDependency;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * Created by IntelliJ IDEA.
 *
 * @author jbaruch
 * @since 15/02/12
 */
@Test
public class BuildDependencyTest {
    /**
     * Validates the build dependency values after initializing the default constructor
     */
    public void testEmptyConstructor() {
        BuildDependency buildDependency = new BuildDependency();

        assertNull(buildDependency.getName(), "Name should have not been initialized.");
        assertNull(buildDependency.getNumber(), "Number should have not been initialized.");
        assertNull(buildDependency.getStarted(), "Timestamp should have not been initialized.");
        assertNull(buildDependency.getUri(), "URI should have not been initialized.");
    }

    /**
     * Validates the build dependency values after using the setters
     */
    public void testSetters() {

        String name = "foo";
        String number = "123";
        Date date = new Date();
        String started = new SimpleDateFormat(Build.STARTED_FORMAT).format(date);
        String uri = "http://myhostA.com/artifactory/builds/foo/123/";

        BuildDependency buildDependency = new BuildDependency();
        buildDependency.setName(name);
        buildDependency.setNumber(number);
        buildDependency.setStarted(started);
        buildDependency.setUri(uri);

        assertEquals(buildDependency.getName(), name, "Unexpected name.");
        assertEquals(buildDependency.getNumber(), number, "Unexpected number.");
        assertEquals(buildDependency.getStarted(), started, "Unexpected started.");
        assertEquals(buildDependency.getUri(), uri, "Unexpected uri.");
    }

}
