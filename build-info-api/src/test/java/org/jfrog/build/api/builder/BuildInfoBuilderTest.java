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

import org.jfrog.build.api.*;
import org.jfrog.build.api.release.PromotionStatus;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

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
        Build build = new BuildInfoBuilder("test").number("4").started("test").build();
        assertEquals(build.getVersion(), "1.0.1", "Unexpected default build version.");
        assertEquals(build.getNumber(), "4", "Unexpected default build number.");

        assertNull(build.getAgent(), "Default build agent should be null.");

        assertEquals(build.getDurationMillis(), 0, "Default build duration millis should be zero.");
        assertNull(build.getPrincipal(), "Default build principal should be null.");
        assertNull(build.getArtifactoryPrincipal(), "Default build artifactory principal should be null.");
        assertNull(build.getArtifactoryPluginVersion(), "Default build ArtifactoryPluginVersion should be null.");
        assertNull(build.getUrl(), "Default build URL should be null.");
        assertNull(build.getParentName(), "Default build parent build name should be null.");
        assertNull(build.getParentNumber(), "Default build parent build number should be null.");
        assertNull(build.getModules(), "Default build modules should be null.");
        assertNull(build.getProperties(), "Default properties should be null.");
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

        Build build = new BuildInfoBuilder(name).started("test").version(version).number(number)
                .agent(agent).durationMillis(durationMillis).principal(principal)
                .artifactoryPrincipal(artifactoryPrincipal).url(url).parentName(parentName).parentNumber(parentNumber)
                .modules(modules).properties(properties).buildAgent(buildAgent).buildRunParameters(runParameters)
                .artifactoryPluginVersion(artifactoryPluginVersion).build();

        assertEquals(build.getVersion(), version, "Unexpected build version.");
        assertEquals(build.getName(), name, "Unexpected build name.");
        assertEquals(build.getNumber(), number, "Unexpected build number.");
        assertEquals(build.getAgent(), agent, "Unexpected build agent.");
        assertEquals(build.getBuildAgent(), buildAgent, "Unexpected build agent.");
        assertEquals(build.getDurationMillis(), durationMillis, "Unexpected build duration millis.");
        assertEquals(build.getPrincipal(), principal, "Unexpected build principal.");
        assertEquals(build.getArtifactoryPrincipal(), artifactoryPrincipal, "Unexpected build artifactory principal.");
        assertEquals(build.getArtifactoryPluginVersion(), artifactoryPluginVersion, "Unexpected build artifactoryPluginVersion.");
        assertEquals(build.getUrl(), url, "Unexpected build URL.");
        assertEquals(build.getParentName(), parentName, "Unexpected build parent name.");
        assertEquals(build.getParentNumber(), parentNumber, "Unexpected build parent build number.");
        assertEquals(build.getModules(), modules, "Unexpected build modules.");
        assertTrue(build.getModules().isEmpty(), "Build modules list should not have been populated.");
        assertEquals(build.getProperties(), properties, "Unexpected build properties.");
        assertTrue(build.getProperties().isEmpty(), "Build properties list should not have been populated.");
        assertTrue(build.getRunParameters().isEmpty(), "Run Parameters list should not have been populated.");
    }

    /**
     * Validates the build start time values after using the builder setters
     */
    public void testStartedSetters() throws ParseException {
        String started = "192-1212-1";
        Build build = new BuildInfoBuilder("test").number("4").started(started).build();
        assertEquals(build.getStarted(), started, "Unexpected build started.");

        Date startedDate = new Date();
        build = new BuildInfoBuilder("test").number("4").startedDate(startedDate).build();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Build.STARTED_FORMAT);
        assertEquals(build.getStarted(), simpleDateFormat.format(startedDate), "Unexpected build started.");
    }

    /**
     * Validates the build values after using the builder add methods
     */
    public void testBuilderAddMethod() {
        Module module = new Module();
        module.setId("module-id");
        String propertyKey = "key";
        String propertyValue = "value";
        PromotionStatus promotionStatus = new PromotionStatusBuilder("momo").timestampDate(new Date()).build();

        Build build = new BuildInfoBuilder("test").number("4").started("test").addModule(module)
                .addProperty(propertyKey, propertyValue).addStatus(promotionStatus).build();
        List<Module> modules = build.getModules();
        assertFalse(modules.isEmpty(), "A build module should have been added.");
        assertEquals(modules.get(0), module, "Unexpected build module.");

        assertTrue(build.getProperties().containsKey(propertyKey), "A build property should have been added.");
        assertEquals(build.getProperties().get(propertyKey), propertyValue, "Unexpected build property value.");

        List<PromotionStatus> statuses = build.getStatuses();
        assertFalse(statuses.isEmpty(), "Expected a status to be added.");
        assertEquals(statuses.get(0), promotionStatus, "Unexpected added status.");
    }
}