package org.jfrog.build.api;

import com.google.common.collect.Lists;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.jfrog.build.api.BuildType.GRADLE;
import static org.testng.Assert.*;

/**
 * Tests the behavior of the build class
 *
 * @author Noam Y. Tenne
 */
@Test
public class BuildTest {

    /**
     * Validates the build values after initializing the default constructor
     */
    public void testEmptyConstructor() {
        Build build = new Build();

        assertEquals(build.getVersion(), "1.0.0", "Unexpected default build version.");
        assertNull(build.getName(), "Build name should have not been initialized.");
        assertEquals(build.getNumber(), 0, "Build number should have not been initialized.");
        assertNull(build.getType(), "Build type should have not been initialized.");
        assertNull(build.getAgent(), "Build agent should have not been initialized.");
        assertNull(build.getStarted(), "Build started should have not been initialized.");
        assertEquals(build.getDurationMillis(), 0, "Build duration should have not been initialized.");
        assertNull(build.getPrincipal(), "Build principal should have not been initialized.");
        assertNull(build.getArtifactoryPrincipal(), "Build artifactory principal should have not been initialized.");
        assertNull(build.getUrl(), "Build URL should have not been initialized.");
        assertNull(build.getParentBuildId(), "Build parent build ID should have not been initialized.");
        assertNull(build.getModules(), "Build modules should have not been initialized.");
        assertNull(build.getProperties(), "Build properties should have not been initialized.");
    }

    /**
     * Validates the build values after using the build setters
     */
    public void testSetters() {
        String version = "1.2.0";
        String name = "moo";
        long number = 15L;
        BuildType buildType = GRADLE;
        Agent agent = new Agent("pop", "1.6");
        long durationMillis = 6L;
        String principal = "bob";
        String artifactoryPrincipal = "too";
        String url = "mitz";
        String parentBuildId = "pooh";
        List<Module> modules = Lists.newArrayList();
        Properties properties = new Properties();

        Build build = new Build();
        build.setVersion(version);
        build.setName(name);
        build.setNumber(number);
        build.setType(buildType);
        build.setAgent(agent);
        build.setDurationMillis(durationMillis);
        build.setPrincipal(principal);
        build.setArtifactoryPrincipal(artifactoryPrincipal);
        build.setUrl(url);
        build.setParentBuildId(parentBuildId);
        build.setModules(modules);
        build.setProperties(properties);

        assertEquals(build.getVersion(), version, "Unexpected build version.");
        assertEquals(build.getName(), name, "Unexpected build name.");
        assertEquals(build.getNumber(), number, "Unexpected build number.");
        assertEquals(build.getType(), buildType, "Unexpected build type.");
        assertEquals(build.getAgent(), agent, "Unexpected build agent.");
        assertEquals(build.getDurationMillis(), durationMillis, "Unexpected build duration millis.");
        assertEquals(build.getPrincipal(), principal, "Unexpected build principal.");
        assertEquals(build.getArtifactoryPrincipal(), artifactoryPrincipal, "Unexpected build artifactory principal.");
        assertEquals(build.getUrl(), url, "Unexpected build URL.");
        assertEquals(build.getParentBuildId(), parentBuildId, "Unexpected build parent build ID.");
        assertEquals(build.getModules(), modules, "Unexpected build modules.");
        assertTrue(build.getModules().isEmpty(), "Build modules list should not have been populated.");
        assertEquals(build.getProperties(), properties, "Unexpected build properties.");
        assertTrue(build.getProperties().isEmpty(), "Build properties list should not have been populated.");
    }

    /**
     * Validates the build start time values after using the build setters
     */
    public void testStartedSetters() throws ParseException {
        Build build = new Build();

        String started = "192-1212-1";
        build.setStarted(started);

        assertEquals(build.getStarted(), started, "Unexpected build started.");

        Date startedDate = new Date();
        build.setStartedDate(startedDate);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Build.STARTED_FORMAT);
        assertEquals(build.getStarted(), simpleDateFormat.format(startedDate), "Unexpected build started.");
    }
}