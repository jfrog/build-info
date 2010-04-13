package org.jfrog.build.api;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.io.Serializable;

/**
 * Information about the agent that executed the build (e.g. Maven, Ant/Ivy, Gradle etc.)
 *
 * @author Tomer Cohen
 */
@XStreamAlias("buildagent")
public class BuildAgent implements Serializable {

    private String name;
    private String version;

    /**
     * Default constructor
     */
    public BuildAgent() {
    }

    /**
     * Main constructor
     *
     * @param name    Agent name
     * @param version Agent version
     */
    public BuildAgent(String name, String version) {
        this.name = name;
        this.version = version;
    }

    /**
     * Returns the name of the agent
     *
     * @return Agent name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the agent
     *
     * @param name Agent name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the version of the agent
     *
     * @return Agent version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the version of the agent
     *
     * @param version Agent version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Returns the display representation of the agent
     *
     * @return AGENT_NAME/AGENT_VERISON
     */
    @Override
    public String toString() {
        return name + "/" + version;
    }
}