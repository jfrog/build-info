

package org.jfrog.build.extractor.ci;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * Tests the behavior of the agent class
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