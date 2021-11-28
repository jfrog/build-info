package org.jfrog.build.api.builder;

import org.jfrog.build.api.Agent;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildAgent;
import org.jfrog.build.api.MatrixParameter;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.release.PromotionStatus;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Tests the behavior of the build info builder class
 *
 * @author Noam Y. Tenne
 */
@Test
public class BuildBuilderTest {

    /**
     * Validates the build values when using the defaults
     */
    public void testDefaultBuild() {
        Build buildInfo = new BuildInfoBuilder("test").number("4").started("test").build();
        assertEquals(buildInfo.getVersion(), "1.0.1", "Unexpected default buildInfo version.");
        assertEquals(buildInfo.getNumber(), "4", "Unexpected default buildInfo number.");

        assertNull(buildInfo.getAgent(), "Default buildInfo agent should be null.");

        assertEquals(buildInfo.getDurationMillis(), 0, "Default buildInfo duration millis should be zero.");
        assertNull(buildInfo.getPrincipal(), "Default buildInfo principal should be null.");
        assertNull(buildInfo.getArtifactoryPrincipal(), "Default buildInfo artifactory principal should be null.");
        assertNull(buildInfo.getArtifactoryPluginVersion(), "Default buildInfo ArtifactoryPluginVersion should be null.");
        assertNull(buildInfo.getUrl(), "Default buildInfo URL should be null.");
        assertNull(buildInfo.getParentName(), "Default buildInfo parent buildInfo name should be null.");
        assertNull(buildInfo.getParentNumber(), "Default buildInfo parent buildInfo number should be null.");
        assertNull(buildInfo.getModules(), "Default buildInfo modules should be null.");
        assertNull(buildInfo.getProperties(), "Default properties should be null.");
    }

    /**
     * Validates the build values after using the builder setters
     */
    public void testBuilderSetters() {
        String version = "1.2.0";
        String name = "moo";
        String number = "15";
        Agent agent = new Agent("pop", "1.6");
        BuildAgent buildAgent = new BuildAgent("rock", "2.6");
        long durationMillis = 6L;
        String principal = "bob";
        String artifactoryPrincipal = "too";
        String artifactoryPluginVersion = "BestCI Artifactory Plugin: 2.3.1";
        String url = "mitz";
        String parentName = "pooh";
        String parentNumber = "5";
        List<Module> modules = new ArrayList<>();
        Properties properties = new Properties();
        List<MatrixParameter> runParameters = new ArrayList<>();

        Build buildInfo = new BuildInfoBuilder(name).started("test").version(version).number(number)
                .agent(agent).durationMillis(durationMillis).principal(principal)
                .artifactoryPrincipal(artifactoryPrincipal).url(url).parentName(parentName).parentNumber(parentNumber)
                .modules(modules).properties(properties).buildAgent(buildAgent).buildRunParameters(runParameters)
                .artifactoryPluginVersion(artifactoryPluginVersion).build();

        assertEquals(buildInfo.getVersion(), version, "Unexpected buildInfo version.");
        assertEquals(buildInfo.getName(), name, "Unexpected buildInfo name.");
        assertEquals(buildInfo.getNumber(), number, "Unexpected buildInfo number.");
        assertEquals(buildInfo.getAgent(), agent, "Unexpected buildInfo agent.");
        assertEquals(buildInfo.getBuildAgent(), buildAgent, "Unexpected buildInfo agent.");
        assertEquals(buildInfo.getDurationMillis(), durationMillis, "Unexpected buildInfo duration millis.");
        assertEquals(buildInfo.getPrincipal(), principal, "Unexpected buildInfo principal.");
        assertEquals(buildInfo.getArtifactoryPrincipal(), artifactoryPrincipal, "Unexpected buildInfo artifactory principal.");
        assertEquals(buildInfo.getArtifactoryPluginVersion(), artifactoryPluginVersion, "Unexpected buildInfo artifactoryPluginVersion.");
        assertEquals(buildInfo.getUrl(), url, "Unexpected buildInfo URL.");
        assertEquals(buildInfo.getParentName(), parentName, "Unexpected buildInfo parent name.");
        assertEquals(buildInfo.getParentNumber(), parentNumber, "Unexpected buildInfo parent buildInfo number.");
        assertEquals(buildInfo.getModules(), modules, "Unexpected buildInfo modules.");
        assertTrue(buildInfo.getModules().isEmpty(), "BuildInfo modules list should not have been populated.");
        assertEquals(buildInfo.getProperties(), properties, "Unexpected buildInfo properties.");
        assertTrue(buildInfo.getProperties().isEmpty(), "BuildInfo properties list should not have been populated.");
        assertTrue(buildInfo.getRunParameters().isEmpty(), "Run Parameters list should not have been populated.");
    }

    /**
     * Validates the build start time values after using the builder setters
     */
    public void testStartedSetters() throws ParseException {
        String started = "192-1212-1";
        Build buildInfo = new BuildInfoBuilder("test").number("4").started(started).build();
        assertEquals(buildInfo.getStarted(), started, "Unexpected buildInfo started.");

        Date startedDate = new Date();
        buildInfo = new BuildInfoBuilder("test").number("4").startedDate(startedDate).build();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Build.STARTED_FORMAT);
        assertEquals(buildInfo.getStarted(), simpleDateFormat.format(startedDate), "Unexpected buildInfo started.");
    }

    /**
     * Validates the build values after using the builder add methods
     */
    public void testBuilderAddMethod() {
        Module module = new Module();
        module.setId("module-id");
        module.setRepository("libs-cow");
        module.setMd5("moo22");
        module.setSha1("moo33");
        module.setType("mammals");
        String propertyKey = "key";
        String propertyValue = "value";
        PromotionStatus promotionStatus = new PromotionStatusBuilder("momo").timestampDate(new Date()).build();

        Build buildInfo = new BuildInfoBuilder("test").number("4").started("test").addModule(module)
                .addProperty(propertyKey, propertyValue).addStatus(promotionStatus).build();
        List<Module> modules = buildInfo.getModules();
        assertFalse(modules.isEmpty(), "A buildInfo module should have been added.");
        assertEquals(modules.get(0), module, "Unexpected buildInfo module.");

        assertTrue(buildInfo.getProperties().containsKey(propertyKey), "A buildInfo property should have been added.");
        assertEquals(buildInfo.getProperties().get(propertyKey), propertyValue, "Unexpected buildInfo property value.");

        List<PromotionStatus> statuses = buildInfo.getStatuses();
        assertFalse(statuses.isEmpty(), "Expected a status to be added.");
        assertEquals(statuses.get(0), promotionStatus, "Unexpected added status.");
    }
}