/*
 * Copyright (C) 2010 JFrog Ltd.
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

        assertEquals(build.getVersion(), "1.0.1", "Unexpected default build version.");
        assertNull(build.getName(), "Build name should have not been initialized.");
        assertNull(build.getNumber(), "Build number should have not been initialized.");
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
        String number = "15";
        BuildType buildType = GRADLE;
        Agent agent = new Agent("pop", "1.6");
        long durationMillis = 6L;
        String principal = "bob";
        String artifactoryPrincipal = "too";
        String url = "mitz";
        String parentName = "pooh";
        String parentNumber = "5";
        String vcsRevision = "2421";
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
        build.setParentName(parentName);
        build.setParentNumber(parentNumber);
        build.setModules(modules);
        build.setProperties(properties);
        build.setVcsRevision(vcsRevision);

        assertEquals(build.getVersion(), version, "Unexpected build version.");
        assertEquals(build.getName(), name, "Unexpected build name.");
        assertEquals(build.getNumber(), number, "Unexpected build number.");
        assertEquals(build.getType(), buildType, "Unexpected build type.");
        assertEquals(build.getAgent(), agent, "Unexpected build agent.");
        assertEquals(build.getDurationMillis(), durationMillis, "Unexpected build duration millis.");
        assertEquals(build.getPrincipal(), principal, "Unexpected build principal.");
        assertEquals(build.getArtifactoryPrincipal(), artifactoryPrincipal, "Unexpected build artifactory principal.");
        assertEquals(build.getUrl(), url, "Unexpected build URL.");
        assertEquals(build.getParentName(), parentName, "Unexpected build parent build name.");
        assertEquals(build.getParentNumber(), parentNumber, "Unexpected build parent build number.");
        assertEquals(build.getVcsRevision(), vcsRevision, "Unexpected build vcs revision.");
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