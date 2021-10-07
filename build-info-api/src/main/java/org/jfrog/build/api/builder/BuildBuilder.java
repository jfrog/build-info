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

import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Agent;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildAgent;
import org.jfrog.build.api.BuildRetention;
import org.jfrog.build.api.Issues;
import org.jfrog.build.api.LicenseControl;
import org.jfrog.build.api.MatrixParameter;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.Vcs;
import org.jfrog.build.api.release.PromotionStatus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A builder for the build class
 */
public class BuildBuilder {
    protected String version;
    protected String name;
    protected String started;
    protected long startedMillis;
    protected String number;
    protected String project;
    protected String artifactoryPluginVersion;
    protected Agent agent;
    protected BuildAgent buildAgent;
    protected long durationMillis;
    protected String principal;
    protected String artifactoryPrincipal;
    protected String url;
    protected String parentName;
    protected String parentNumber;
    protected List<Vcs> vcs = new ArrayList<Vcs>();
    protected String vcsRevision;
    protected String vcsUrl;
    protected List<MatrixParameter> runParameters;
    protected ConcurrentHashMap<String, Module> modules;
    protected List<PromotionStatus> statuses;
    protected Properties properties;
    protected LicenseControl licenseControl;
    protected BuildRetention buildRetention;
    protected Issues issues;

    public BuildBuilder(String name) {
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
        build.setProject(project);
        build.setAgent(agent);
        build.setBuildAgent(buildAgent);
        build.setStarted(started);
        build.setStartedMillis(startedMillis);
        build.setDurationMillis(durationMillis);
        build.setPrincipal(principal);
        build.setArtifactoryPrincipal(artifactoryPrincipal);
        build.setArtifactoryPluginVersion(artifactoryPluginVersion);
        build.setUrl(url);
        build.setParentName(parentName);
        build.setParentNumber(parentNumber);
        build.setRunParameters(runParameters);
        build.setModules(modules != null ? new ArrayList<>(modules.values()) : null);
        build.setStatuses(statuses);
        build.setProperties(properties);
        build.setVcs(vcs);
        build.setLicenseControl(licenseControl);
        build.setBuildRetention(buildRetention);
        build.setIssues(issues);
        return build;
    }

    /**
     * Sets the version of the build
     *
     * @param version Build version
     * @return Builder instance
     */
    public BuildBuilder version(String version) {
        this.version = version;
        return this;
    }

    /**
     * Sets the name of the build
     *
     * @param name Build name
     * @return Builder instance
     */
    public BuildBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the number of the build
     *
     * @param number Build number
     * @return Builder instance
     */
    public BuildBuilder number(String number) {
        this.number = number;
        return this;
    }

    /**
     * Sets the agent of the build
     *
     * @param agent Build agent
     * @return Builder instance
     */
    public BuildBuilder agent(Agent agent) {
        this.agent = agent;
        return this;
    }

    /**
     * Sets the build agent of the build
     *
     * @param buildAgent The build agent
     * @return Builder instance
     */
    public BuildBuilder buildAgent(BuildAgent buildAgent) {
        this.buildAgent = buildAgent;
        return this;
    }

    /**
     * Sets the started time of the build
     *
     * @param started Build started time
     * @return Builder instance
     */
    public BuildBuilder started(String started) {
        this.started = started;
        return this;
    }

    /**
     * Sets the started time in millis of the build
     *
     * @return Builder instance
     */
    public BuildBuilder startedMillis(long startedMillis) {
        this.startedMillis = startedMillis;
        return this;
    }

    /**
     * Sets the started time of the build
     *
     * @param startedDate BuildInfo started date
     * @return Builder instance
     */
    public BuildBuilder startedDate(Date startedDate) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Build.STARTED_FORMAT);
        this.started = simpleDateFormat.format(startedDate);
        this.startedMillis = startedDate.getTime();
        return this;
    }
    /**
     * Sets the duration milliseconds of the build
     *
     * @param durationMillis Build duration milliseconds
     * @return Builder instance
     */
    public BuildBuilder durationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
        return this;
    }

    /**
     * Sets the principal of the build
     *
     * @param principal Build principal
     * @return Builder instance
     */
    public BuildBuilder principal(String principal) {
        this.principal = principal;
        return this;
    }

    /**
     * Sets the Artifactory principal of the build
     *
     * @param artifactoryPrincipal Build Artifactory principal
     * @return Builder instance
     */
    public BuildBuilder artifactoryPrincipal(String artifactoryPrincipal) {
        this.artifactoryPrincipal = artifactoryPrincipal;
        return this;
    }

    public BuildBuilder artifactoryPluginVersion(String artifactoryPluginVersion) {
        this.artifactoryPluginVersion = artifactoryPluginVersion;
        return this;
    }

    /**
     * Sets the URL of the build
     *
     * @param url Build URL
     * @return Builder instance
     */
    public BuildBuilder url(String url) {
        this.url = url;
        return this;
    }


    /**
     * Sets the parent build name of the build
     *
     * @param parentName Build parent build name
     * @return Builder instance
     */
    public BuildBuilder parentName(String parentName) {
        this.parentName = parentName;
        return this;
    }

    /**
     * Sets the parent build number of the build
     *
     * @param parentNumber Build parent build number
     * @return Builder instance
     */
    public BuildBuilder parentNumber(String parentNumber) {
        this.parentNumber = parentNumber;
        return this;
    }

    /**
     * Sets the vcs revision (format is vcs specific)
     *
     * @param vcs The vcs data
     * @return Builder instance
     */
    public BuildBuilder vcs(List<Vcs> vcs) {
        this.vcs = vcs;
        return this;
    }

    /**
     * Sets the vcs revision (format is vcs specific)
     *
     * @param vcsRevision The vcs revision
     * @return Builder instance
     */
    public BuildBuilder vcsRevision(String vcsRevision) {
        this.vcsRevision = vcsRevision;
        return this;
    }

    /**
     * Sets the vcs revision (format is vcs specific)
     *
     * @param vcsUrl The vcs revision
     * @return Builder instance
     */
    public BuildBuilder vcsUrl(String vcsUrl) {
        this.vcsUrl = vcsUrl;
        return this;
    }

    /**
     * Sets the modules of the build
     *
     * @param modules Build modules
     * @return Builder instance
     */
    public BuildBuilder modules(ConcurrentHashMap<String, Module> modules) {
        this.modules = modules;
        return this;
    }

    /**
     * Sets the modules of the build
     *
     * @param modules Build modules
     * @return Builder instance
     */
    public BuildBuilder modules(List<Module> modules) {
        ConcurrentHashMap<String, Module> modulesMap = new ConcurrentHashMap<String, Module>();
        for (Module module : modules) {
            modulesMap.put(module.getId(), module);
        }

        this.modules = modulesMap;
        return this;
    }

    public BuildBuilder statuses(List<PromotionStatus> statuses) {
        this.statuses = statuses;
        return this;
    }

    public BuildBuilder addStatus(PromotionStatus promotionStatus) {
        if (statuses == null) {
            statuses = new ArrayList<>();
        }
        statuses.add(promotionStatus);
        return this;
    }

    /**
     * Sets the violation notifications of the build
     *
     * @param licenseControl Build violation  recipients.
     * @return Builder instance
     */
    public BuildBuilder licenseControl(LicenseControl licenseControl) {
        this.licenseControl = licenseControl;
        return this;
    }

    /**
     * Sets the post build retention period
     *
     * @param buildRetention Build violation  recipients.
     * @return Builder instance
     */
    public BuildBuilder buildRetention(BuildRetention buildRetention) {
        this.buildRetention = buildRetention;
        return this;
    }

    /**
     * Sets the post build retention period
     *
     * @param runParameters matrix parameters.
     * @return Builder instance
     */
    public BuildBuilder buildRunParameters(List<MatrixParameter> runParameters) {
        this.runParameters = runParameters;
        return this;
    }

    /**
     * Sets the properties of the build
     *
     * @param properties Build properties
     * @return Builder instance
     */
    public BuildBuilder properties(Properties properties) {
        this.properties = properties;
        return this;
    }

    public BuildBuilder issues(Issues issues) {
        this.issues = issues;
        return this;
    }

    public BuildBuilder setProject(String project) {
        this.project = project;
        return this;
    }
}