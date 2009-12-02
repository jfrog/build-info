package org.artifactory.build.api;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.io.Serializable;

/**
 * Contains the build agent information
 *
 * @author Noam Y. Tenne
 */
@XStreamAlias("agent")
public class Agent implements Serializable {

    private String name;
    private String version;

    /**
     * Default constructor
     */
    public Agent() {
    }

    /**
     * Main constructor
     *
     * @param name    Agent name
     * @param version Agent version
     */
    public Agent(String name, String version) {
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