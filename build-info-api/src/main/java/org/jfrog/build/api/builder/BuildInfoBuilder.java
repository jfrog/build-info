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

package org.jfrog.build.api.builder;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * A builder for the build class
 *
 * @author Noam Y. Tenne
 */
public class BuildInfoBuilder {

    private String version;
    private String name;
    private String started;
    private String number;
    private BuildType type;
    private Agent agent;
    private BuildAgent buildAgent;
    private long durationMillis;
    private String principal;
    private String artifactoryPrincipal;
    private String url;
    private String parentName;
    private String parentNumber;
    private String vcsRevision;
    private List<Module> modules;
    private Properties properties;
    private LicenseControl licenseControl;

    public BuildInfoBuilder(String name) {
        this.name = name;
    }

    /**
     * Assembles the build class
     *
     * @return Assembled build
     */
    public Build build() {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Build must have a name");
        }
        if (StringUtils.isBlank(number)) {
            throw new IllegalArgumentException("Build number must be set");
        }
        if (StringUtils.isBlank(started)) {
            throw new IllegalArgumentException("Build start time must be set");
        }

        Build build = new Build();
        if (StringUtils.isNotBlank(version)) {
            build.setVersion(version);
        }
        build.setName(name);
        build.setNumber(number);
        build.setType(type);
        build.setAgent(agent);
        build.setBuildAgent(buildAgent);
        build.setStarted(started);
        build.setDurationMillis(durationMillis);
        build.setPrincipal(principal);
        build.setArtifactoryPrincipal(artifactoryPrincipal);
        build.setUrl(url);
        build.setParentName(parentName);
        build.setParentNumber(parentNumber);
        build.setModules(modules);
        build.setProperties(properties);
        build.setVcsRevision(vcsRevision);
        build.setLicenseControl(licenseControl);
        return build;
    }

    /**
     * Sets the version of the build
     *
     * @param version Build version
     * @return Builder instance
     */
    public BuildInfoBuilder version(String version) {
        this.version = version;
        return this;
    }

    /**
     * Sets the name of the build
     *
     * @param name Build name
     * @return Builder instance
     */
    public BuildInfoBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the number of the build
     *
     * @param number Build number
     * @return Builder instance
     */
    public BuildInfoBuilder number(String number) {
        this.number = number;
        return this;
    }

    /**
     * Sets the type of the build
     *
     * @param type Build type
     * @return Builder instance
     */
    public BuildInfoBuilder type(BuildType type) {
        this.type = type;
        return this;
    }

    /**
     * Sets the agent of the build
     *
     * @param agent Build agent
     * @return Builder instance
     */
    public BuildInfoBuilder agent(Agent agent) {
        this.agent = agent;
        return this;
    }

    /**
     * Sets the build agent of the build
     *
     * @param buildAgent The build agent
     * @return Builder instance
     */
    public BuildInfoBuilder buildAgent(BuildAgent buildAgent) {
        this.buildAgent = buildAgent;
        return this;
    }

    /**
     * Sets the started time of the build
     *
     * @param started Build started time
     * @return Builder instance
     */
    public BuildInfoBuilder started(String started) {
        this.started = started;
        return this;
    }

    /**
     * Sets the started time of the build
     *
     * @param startedDate Build started date
     * @return Builder instance
     */
    public BuildInfoBuilder startedDate(Date startedDate) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Build.STARTED_FORMAT);
        this.started = simpleDateFormat.format(startedDate);
        return this;
    }

    /**
     * Sets the duration milliseconds of the build
     *
     * @param durationMillis Build duration milliseconds
     * @return Builder instance
     */
    public BuildInfoBuilder durationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
        return this;
    }

    /**
     * Sets the principal of the build
     *
     * @param principal Build principal
     * @return Builder instance
     */
    public BuildInfoBuilder principal(String principal) {
        this.principal = principal;
        return this;
    }

    /**
     * Sets the Artifactory principal of the build
     *
     * @param artifactoryPrincipal Build Artifactory principal
     * @return Builder instance
     */
    public BuildInfoBuilder artifactoryPrincipal(String artifactoryPrincipal) {
        this.artifactoryPrincipal = artifactoryPrincipal;
        return this;
    }

    /**
     * Sets the URL of the build
     *
     * @param url Build URL
     * @return Builder instance
     */
    public BuildInfoBuilder url(String url) {
        this.url = url;
        return this;
    }


    /**
     * Sets the parent build name of the build
     *
     * @param parentName Build parent build name
     * @return Builder instance
     */
    public BuildInfoBuilder parentName(String parentName) {
        this.parentName = parentName;
        return this;
    }

    /**
     * Sets the parent build number of the build
     *
     * @param parentNumber Build parent build number
     * @return Builder instance
     */
    public BuildInfoBuilder parentNumber(String parentNumber) {
        this.parentNumber = parentNumber;
        return this;
    }

    /**
     * Sets the vcs revision (format is vcs specific)
     *
     * @param vcsRevision The vcs revision
     * @return Builder instance
     */
    public BuildInfoBuilder vcsRevision(String vcsRevision) {
        this.vcsRevision = vcsRevision;
        return this;
    }

    /**
     * Sets the modules of the build
     *
     * @param modules Build modules
     * @return Builder instance
     */
    public BuildInfoBuilder modules(List<Module> modules) {
        this.modules = modules;
        return this;
    }

    /**
     * Sets the violation notifications of the build
     *
     * @param licenseControl Build violation  recipients.
     * @return Builder instance
     */
    public BuildInfoBuilder licenseControl(LicenseControl licenseControl) {
        this.licenseControl = licenseControl;
        return this;
    }

    /**
     * Adds the given module to the modules list
     *
     * @param module Module to add
     * @return Builder instance
     */
    public BuildInfoBuilder addModule(Module module) {
        if (modules == null) {
            modules = Lists.newArrayList();
        }
        modules.add(module);
        return this;
    }

    /**
     * Sets the properties of the build
     *
     * @param properties Build properties
     * @return Builder instance
     */
    public BuildInfoBuilder properties(Properties properties) {
        this.properties = properties;
        return this;
    }

    /**
     * Adds the given property to the properties object
     *
     * @param key   Key of property to add
     * @param value Value of property to add
     * @return Builder instance
     */
    public BuildInfoBuilder addProperty(Object key, Object value) {
        if (properties == null) {
            properties = new Properties();
        }
        properties.put(key, value);
        return this;
    }
}