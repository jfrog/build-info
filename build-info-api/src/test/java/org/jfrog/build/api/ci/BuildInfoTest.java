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

package org.jfrog.build.api.ci;

import org.jfrog.build.api.builder.PromotionStatusBuilder;
import org.jfrog.build.api.builder.dependency.BuildDependencyBuilder;
import org.jfrog.build.api.dependency.BuildDependency;
import org.jfrog.build.api.release.Promotion;
import org.jfrog.build.api.release.PromotionStatus;
import org.jfrog.build.api.util.CommonUtils;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Tests the behavior of the build class
 *
 * @author Noam Y. Tenne
 */
@Test
public class BuildInfoTest {

    /**
     * Validates the build values after initializing the default constructor
     */
    public void testEmptyConstructor() {
        BuildInfo buildInfo = new BuildInfo();

        assertEquals(buildInfo.getVersion(), "1.0.1", "Unexpected default buildInfo version.");
        assertNull(buildInfo.getName(), "BuildInfo name should have not been initialized.");
        assertNull(buildInfo.getNumber(), "BuildInfo number should have not been initialized.");
        assertNull(buildInfo.getAgent(), "BuildInfo agent should have not been initialized.");
        assertNull(buildInfo.getStarted(), "BuildInfo started should have not been initialized.");
        assertEquals(buildInfo.getDurationMillis(), 0, "BuildInfo duration should have not been initialized.");
        assertNull(buildInfo.getPrincipal(), "BuildInfo principal should have not been initialized.");
        assertNull(buildInfo.getArtifactoryPrincipal(), "BuildInfo artifactory principal should have not been initialized.");
        assertNull(buildInfo.getArtifactoryPluginVersion(), "BuildInfo Artifactory Plugin Version should have not been initialized.");
        assertNull(buildInfo.getUrl(), "BuildInfo URL should have not been initialized.");
        assertNull(buildInfo.getParentBuildId(), "BuildInfo parent buildInfo ID should have not been initialized.");
        assertNull(buildInfo.getModules(), "BuildInfo modules should have not been initialized.");
        assertNull(buildInfo.getProperties(), "BuildInfo properties should have not been initialized.");
        assertNull(buildInfo.getBuildDependencies(), "BuildInfo dependencies should have not been initialized.");
    }

    /**
     * Validates the build values after using the build setters
     */
    public void testSetters() {
        String version = "1.2.0";
        String name = "moo";
        String number = "15";
        Agent agent = new Agent("pop", "1.6");
        long durationMillis = 6L;
        String principal = "bob";
        String artifactoryPrincipal = "too";
        String artifactoryPluginVersion = "2.3.1";
        String url = "mitz";
        String parentName = "pooh";
        String parentNumber = "5";
        List<Vcs> vcsList = Collections.singletonList(new Vcs(url, "2421", "main", "message"));

        List<Module> modules = new ArrayList<>();
        List<PromotionStatus> statuses = new ArrayList<>();
        List<BuildDependency> buildDependencies = Arrays.asList(
                new BuildDependencyBuilder().name("foo").number("123").startedDate(new Date()).build(),
                new BuildDependencyBuilder().name("bar").number("456").startedDate(new Date()).build()
        );
        Properties properties = new Properties();

        BuildInfo buildInfo = new BuildInfo();
        buildInfo.setVersion(version);
        buildInfo.setName(name);
        buildInfo.setNumber(number);
        buildInfo.setAgent(agent);
        buildInfo.setDurationMillis(durationMillis);
        buildInfo.setPrincipal(principal);
        buildInfo.setArtifactoryPrincipal(artifactoryPrincipal);
        buildInfo.setArtifactoryPluginVersion(artifactoryPluginVersion);
        buildInfo.setUrl(url);
        buildInfo.setParentName(parentName);
        buildInfo.setParentNumber(parentNumber);
        buildInfo.setModules(modules);
        buildInfo.setStatuses(statuses);
        buildInfo.setProperties(properties);
        buildInfo.setVcs(vcsList);
        buildInfo.setBuildDependencies(buildDependencies);

        assertEquals(buildInfo.getVersion(), version, "Unexpected buildInfo version.");
        assertEquals(buildInfo.getName(), name, "Unexpected buildInfo name.");
        assertEquals(buildInfo.getNumber(), number, "Unexpected buildInfo number.");
        assertEquals(buildInfo.getAgent(), agent, "Unexpected buildInfo agent.");
        assertEquals(buildInfo.getDurationMillis(), durationMillis, "Unexpected buildInfo duration millis.");
        assertEquals(buildInfo.getPrincipal(), principal, "Unexpected buildInfo principal.");
        assertEquals(buildInfo.getArtifactoryPrincipal(), artifactoryPrincipal, "Unexpected buildInfo artifactory principal.");
        assertEquals(buildInfo.getArtifactoryPluginVersion(), artifactoryPluginVersion, "Unexpected buildInfo artifactory principal.");
        assertEquals(buildInfo.getUrl(), url, "Unexpected buildInfo URL.");
        assertEquals(buildInfo.getParentName(), parentName, "Unexpected buildInfo parent buildInfo name.");
        assertEquals(buildInfo.getParentNumber(), parentNumber, "Unexpected buildInfo parent buildInfo number.");
        assertEquals(buildInfo.getVcs(), vcsList, "Unexpected buildInfo vcs revision.");
        assertEquals(buildInfo.getModules(), modules, "Unexpected buildInfo modules.");
        assertTrue(buildInfo.getModules().isEmpty(), "BuildInfo modules list should not have been populated.");
        assertEquals(buildInfo.getStatuses(), statuses, "Unexpected buildInfo statuses.");
        assertTrue(buildInfo.getStatuses().isEmpty(), "BuildInfo statuses list should not have been populated.");
        assertEquals(buildInfo.getProperties(), properties, "Unexpected buildInfo properties.");
        assertTrue(buildInfo.getProperties().isEmpty(), "BuildInfo properties list should not have been populated.");
        assertEquals(buildInfo.getBuildDependencies(), buildDependencies, "Unexpected buildInfo dependencies list.");
    }

    /**
     * Validates the build start time values after using the build setters
     */
    public void testStartedSetters() throws ParseException {
        BuildInfo buildInfo = new BuildInfo();

        String started = "192-1212-1";
        buildInfo.setStarted(started);

        assertEquals(buildInfo.getStarted(), started, "Unexpected buildInfo started.");

        Date startedDate = new Date();
        buildInfo.setStartedDate(startedDate);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(BuildInfo.STARTED_FORMAT);
        assertEquals(buildInfo.getStarted(), simpleDateFormat.format(startedDate), "Unexpected buildInfo started.");
    }

    public void testStatusAddMethod() {
        BuildInfo buildInfo = new BuildInfo();
        assertNull(buildInfo.getStatuses(), "Default status list should be null.");

        PromotionStatus promotionStatus = new PromotionStatusBuilder(Promotion.RELEASED).repository("bla").
                timestamp("bla").user("bla").build();
        buildInfo.addStatus(promotionStatus);

        assertFalse(buildInfo.getStatuses().isEmpty(), "Status object should have been added.");
        assertEquals(buildInfo.getStatuses().get(0), promotionStatus, "Unexpected status object.");

        PromotionStatus anotherPromotionStatus = new PromotionStatusBuilder(Promotion.RELEASED).repository("bla").
                timestamp("bla").user("bla").build();
        buildInfo.addStatus(anotherPromotionStatus);

        assertEquals(buildInfo.getStatuses().size(), 2, "Second status object should have been added.");
        assertEquals(buildInfo.getStatuses().get(1), anotherPromotionStatus, "Unexpected status object.");
    }

    public void testAddBuildDependencyMethod() {
        BuildInfo buildInfo = new BuildInfo();
        assertNull(buildInfo.getBuildDependencies(), "Default buildDependencies list should be null.");

        BuildDependency buildDependency = new BuildDependencyBuilder().name("foo").number("123").startedDate(new Date()).build();

        buildInfo.addBuildDependency(buildDependency);

        assertFalse(buildInfo.getBuildDependencies().isEmpty(), "BuildDependency object should have been added.");
        assertEquals(CommonUtils.getOnlyElement(buildInfo.getBuildDependencies()), buildDependency, "Unexpected buildInfo dependency object.");

        BuildDependency otherBuildDependency = new BuildDependencyBuilder().name("bar").number("456").startedDate(new Date()).build();
        buildInfo.addBuildDependency(otherBuildDependency);

        assertEquals(buildInfo.getBuildDependencies().size(), 2, "Second BuildDependency object should have been added.");
        assertEquals(CommonUtils.getLast(buildInfo.getBuildDependencies()), otherBuildDependency, "Unexpected buildInfo dependency object.");
    }
}