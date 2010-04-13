package org.jfrog.build.api.builder;

import com.google.common.collect.Lists;
import org.jfrog.build.api.Agent;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildAgent;
import org.jfrog.build.api.BuildType;
import org.jfrog.build.api.Module;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.jfrog.build.api.BuildType.GENERIC;
import static org.jfrog.build.api.BuildType.GRADLE;
import static org.testng.Assert.*;

/**
 * Tests the behavior of the build info builder class
 *
 * @author Noam Y. Tenne
 */
@Test
public class BuildInfoBuilderTest {

    /**
     * Validates the build values when using the defaults
     */
    public void testDefaultBuild() {
        Build build = new BuildInfoBuilder("test").started("test").build();
        assertEquals(build.getVersion(), "1.0.0", "Unexpected default build version.");
        assertEquals(build.getNumber(), 0L, "Unexpected default build number.");
        assertEquals(build.getType(), GENERIC, "Unexpected default build type.");

        Agent agent = build.getAgent();
        assertNotNull(agent, "Default build agent should not be null.");
        assertEquals(agent.getName(), "", "Unexpected default build agent name.");
        assertEquals(agent.getVersion(), "", "Unexpected default build agent version.");

        assertEquals(build.getDurationMillis(), 0L, "Unexpected default build duration millis.");
        assertEquals(build.getPrincipal(), "", "Unexpected default build principal.");
        assertEquals(build.getArtifactoryPrincipal(), "", "Unexpected default build artifactory principal.");
        assertEquals(build.getUrl(), "", "Unexpected default build URL.");
        assertEquals(build.getParentBuildId(), "", "Unexpected default build parent build ID.");
        assertEquals(build.getModules().size(), 0, "Default build modules size should be 0.");
        assertEquals(build.getProperties().size(), 0, "Default properties size should be 0.");
    }

    /**
     * Validates the build values after using the builder setters
     */
    public void testBuilderSetters() {
        String version = "1.2.0";
        String name = "moo";
        long number = 15L;
        BuildType buildType = GRADLE;
        Agent agent = new Agent("pop", "1.6");
        BuildAgent buildAgent = new BuildAgent("rock", "2.6");
        long durationMillis = 6L;
        String principal = "bob";
        String artifactoryPrincipal = "too";
        String url = "mitz";
        String parentBuildId = "pooh";
        List<Module> modules = Lists.newArrayList();
        Properties properties = new Properties();

        Build build = new BuildInfoBuilder(name).started("test").version(version).number(number).type(buildType)
                .agent(agent).durationMillis(durationMillis).principal(principal)
                .artifactoryPrincipal(artifactoryPrincipal).url(url)
                .parentBuildId(parentBuildId).modules(modules).properties(properties).buildAgent(buildAgent).build();

        assertEquals(build.getVersion(), version, "Unexpected build version.");
        assertEquals(build.getName(), name, "Unexpected build name.");
        assertEquals(build.getNumber(), number, "Unexpected build number.");
        assertEquals(build.getType(), buildType, "Unexpected build type.");
        assertEquals(build.getAgent(), agent, "Unexpected build agent.");
        assertEquals(build.getBuildAgent(), buildAgent, "Unexpected build agent.");
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
     * Validates the build start time values after using the builder setters
     */
    public void testStartedSetters() throws ParseException {
        String started = "192-1212-1";
        Build build = new BuildInfoBuilder("test").started(started).build();
        assertEquals(build.getStarted(), started, "Unexpected build started.");

        Date startedDate = new Date();
        build = new BuildInfoBuilder("test").startedDate(startedDate).build();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Build.STARTED_FORMAT);
        assertEquals(build.getStarted(), simpleDateFormat.format(startedDate), "Unexpected build started.");
    }

    /**
     * Validates the build values after using the builder add methods
     */
    public void testBuilderAddMethod() {
        Module module = new Module();
        String propertyKey = "key";
        String propertyValue = "value";

        Build build = new BuildInfoBuilder("test").started("test").addModule(module)
                .addProperty(propertyKey, propertyValue).build();
        List<Module> modules = build.getModules();
        assertFalse(modules.isEmpty(), "A build module should have been added.");
        assertEquals(modules.get(0), module, "Unexpected build module.");
        assertTrue(build.getProperties().containsKey(propertyKey), "A build property should have been added.");
        assertEquals(build.getProperties().get(propertyKey), propertyValue, "Unexpected build property value.");
    }
}