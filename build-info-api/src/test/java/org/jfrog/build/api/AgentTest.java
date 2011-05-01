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

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * Tests the behavior of the agent class
 *
 * @author Noam Y. Tenne
 */
@Test
public class AgentTest {

    /**
     * Validates the agent values after initializing the default constructor
     */
    public void testEmptyConstructor() {
        Agent agent = new Agent();
        assertNull(agent.getName(), "Agent name should have not been initialized.");
        assertNull(agent.getVersion(), "Agent version should have not been initialized.");
        assertEquals(agent.toString(), "", "Agent display representation should not be valid.");
    }

    public void testAgentNameConstructor() {
        Agent agent = new Agent("MOO/1.0");
        assertEquals(agent.getVersion(), "1.0", "Agent version does not match");
        assertEquals(agent.getName(), "MOO", "Agent name does not match");
    }

    /**
     * Validates the agent values after initializing the main constructor
     */
    public void testConstructor() {
        String name = "moo";
        String version = "1.5";

        Agent agent = new Agent(name, version);

        assertEquals(agent.getName(), name, "Unexpected agent name.");
        assertEquals(agent.getVersion(), version, "Unexpected agent version.");
        assertEquals(agent.toString(), name + "/" + version, "Unexpected agent display representation.");
    }

    /**
     * Validates the agent values after using the agent setters
     */
    public void testSetters() {
        String name = "pop";
        String version = "2.3";

        Agent agent = new Agent("moo", "1.5");

        agent.setName(name);
        agent.setVersion(version);
        assertEquals(agent.getName(), name, "Unexpected agent name.");
        assertEquals(agent.getVersion(), version, "Unexpected agent version.");
        assertEquals(agent.toString(), name + "/" + version, "Unexpected agent display representation.");
    }
}