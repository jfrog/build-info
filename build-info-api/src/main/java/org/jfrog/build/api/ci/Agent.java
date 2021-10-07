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

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

/**
 * Information about the agent that triggered the build (e.g. Jenkins, TeamCity etc.).
 *
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
     * Build the build agent from a full agent name in the following format: AGENT_NAME/AGENT_VERSION
     *
     * @param agent The agent name
     */
    public Agent(String agent) {
        int slash = agent.indexOf('/');
        if (slash != -1 && slash < agent.length() - 1) {
            this.name = agent.substring(0, slash);
            this.version = agent.substring(slash + 1);
        } else {
            this.name = agent;
        }
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
        StringBuilder toStringBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(name)) {
            toStringBuilder.append(name);
        }
        if (StringUtils.isNotBlank(version)) {
            if (toStringBuilder.length() > 0) {
                toStringBuilder.append("/");
            }
            toStringBuilder.append(version);
        }
        return toStringBuilder.toString();
    }
}