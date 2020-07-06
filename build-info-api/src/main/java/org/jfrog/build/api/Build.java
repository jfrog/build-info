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

import com.google.common.collect.Lists;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.dependency.BuildDependency;
import org.jfrog.build.api.release.PromotionStatus;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.jfrog.build.api.BuildBean.ROOT;

/**
 * Contains the general build information
 *
 * @author Noam Y. Tenne
 */
@XStreamAlias(ROOT)
public class Build extends BaseBuildBean {

    public static final String STARTED_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private String version = "1.0.1";
    private String name;
    private String number;
    @Deprecated
    private BuildType type;
    private BuildAgent buildAgent;
    private Agent agent;
    private String started;
    private long durationMillis;
    private String principal;
    private String artifactoryPrincipal;
    private String artifactoryPluginVersion;
    private String url;
    private String parentName;
    private String parentNumber;
    private List<Vcs> vcs;

    @Deprecated
    private String parentBuildId;

    private LicenseControl licenseControl;

    private BuildRetention buildRetention;

    @XStreamAlias(RUN_PARAMETERS)
    private List<MatrixParameter> runParameters;

    @XStreamAlias(MODULES)
    private List<Module> modules;

    private List<PromotionStatus> statuses;

    private List<BuildDependency> buildDependencies;

    private Issues issues;

    private Governance governance;

    /**
     * Formats the timestamp to the ISO date time string format expected by the build info API.
     *
     * @param timestamp The build start time timestamp
     * @return ISO date time formatted string
     */
    public static String formatBuildStarted(long timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(STARTED_FORMAT);
        return dateFormat.format(timestamp);
    }

    /**
     * Returns the version of the build
     *
     * @return Build version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the version of the build
     *
     * @param version Build version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Returns the name of the build
     *
     * @return Build name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the build
     *
     * @param name Build name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the number of the build
     *
     * @return Build number
     */
    public String getNumber() {
        return number;
    }

    /**
     * Sets the number of the build
     *
     * @param number Build number
     */
    public void setNumber(String number) {
        this.number = number;
    }

    /**
     * Returns the name of the parent build
     *
     * @return Parent build name
     */
    public String getParentName() {
        return parentName;
    }

    /**
     * Sets the name of the parent build
     *
     * @param parentName Parent build number
     */
    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    /**
     * Returns the number of the parent build
     *
     * @return Parent build number
     */
    public String getParentNumber() {
        return parentNumber;
    }

    /**
     * Sets the number of the parent build
     *
     * @param parentNumber Parent build number
     */
    public void setParentNumber(String parentNumber) {
        this.parentNumber = parentNumber;
    }

    public List<Vcs> getVcs() {
        return vcs;
    }

    public void setVcs(List<Vcs> vcs) {
        this.vcs = vcs;
    }

    /**
     * Returns the type of the build
     *
     * @return Build type
     * @deprecated Use {@link Build#getBuildAgent()} instead.
     */
    @Deprecated
    public BuildType getType() {
        return type;
    }

    /**
     * Sets the type of the build
     *
     * @param type Build type
     * @deprecated Use {@link Build#setBuildAgent(BuildAgent)} instead.
     */
    @Deprecated
    public void setType(BuildType type) {
        this.type = type;
    }

    /**
     * Returns the agent that triggered the build (e.g. Hudson, TeamCity etc.). In case that the build was triggered by
     * the build agent itself, this value would be equal to the {@link #getBuildAgent()}
     *
     * @return Triggering agent
     */
    public Agent getAgent() {
        return agent;
    }

    /**
     * Sets the agent that triggered the build
     *
     * @param agent Triggering agent
     */
    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    /**
     * Returns the agent that executed the build (e.g. Maven, Ant/Ivy, Gradle etc.)
     *
     * @return Build agent
     */
    public BuildAgent getBuildAgent() {
        return buildAgent;
    }

    /**
     * Sets the agent that executed the build
     *
     * @param buildAgent Executing agent
     */
    public void setBuildAgent(BuildAgent buildAgent) {
        this.buildAgent = buildAgent;
    }

    /**
     * Returns the started time of the build
     *
     * @return Build started time
     */
    public String getStarted() {
        return started;
    }

    /**
     * Sets the started time of the build
     *
     * @param started Build started time
     */
    public void setStarted(String started) {
        this.started = started;
    }

    /**
     * Sets the build start time
     *
     * @param startedDate Build start date to set
     */
    public void setStartedDate(Date startedDate) {
        this.started = formatBuildStarted(startedDate.getTime());
    }

    /**
     * Returns the duration milliseconds of the build
     *
     * @return Build duration milliseconds
     */
    public long getDurationMillis() {
        return durationMillis;
    }

    /**
     * Sets the duration milliseconds of the build
     *
     * @param durationMillis Build duration milliseconds
     */
    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    /**
     * Returns the principal of the build
     *
     * @return Build principal
     */
    public String getPrincipal() {
        return principal;
    }

    /**
     * Sets the principal of the build
     *
     * @param principal Build principal
     */
    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    /**
     * Returns the Artifactory principal of the build
     *
     * @return Build Artifactory principal
     */
    public String getArtifactoryPrincipal() {
        return artifactoryPrincipal;
    }

    /**
     * Sets the Artifactory principal of the build
     *
     * @param artifactoryPrincipal Build Artifactory principal
     */
    public void setArtifactoryPrincipal(String artifactoryPrincipal) {
        this.artifactoryPrincipal = artifactoryPrincipal;
    }

    /**
     * Returns the Artifactory plugin version of the build
     *
     * @return Artifactory plugin version
     */
    public String getArtifactoryPluginVersion() {return artifactoryPluginVersion;}

    /**
     * Sets the Artifactory plugin version of the build
     *
     * @param artifactoryPluginVersion Artifactory plugin version
     */
    public void setArtifactoryPluginVersion (String artifactoryPluginVersion){
        this.artifactoryPluginVersion = artifactoryPluginVersion;
    }

    /**
     * Returns the URL of the build
     *
     * @return Build URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the URL of the build
     *
     * @param url Build URL
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Returns the parent build ID of the build
     *
     * @return Build parent build ID
     * @deprecated Use {@link org.jfrog.build.api.Build#getParentName()} and {@link Build#getParentNumber()} instead.
     */
    @Deprecated
    public String getParentBuildId() {
        return parentBuildId;
    }

    /**
     * Sets the parent build ID of the build
     *
     * @param parentBuildId Build parent build ID
     * @deprecated Use {@link org.jfrog.build.api.Build#setParentName(String)} and {@link Build#setParentNumber(String)}
     * instead.
     */
    @Deprecated
    public void setParentBuildId(String parentBuildId) {
        this.parentBuildId = parentBuildId;
    }

    /**
     * Returns the modules of the build
     *
     * @return Build modules
     */
    public List<Module> getModules() {
        return modules;
    }

    /**
     * Sets the modules of the build
     *
     * @param modules Build modules
     */
    public void setModules(List<Module> modules) {
        this.modules = modules;
    }

    /**
     * Returns the module object by the given ID
     *
     * @param moduleId ID of module to locate
     * @return Module object if found. Null if not
     */
    public Module getModule(String moduleId) {
        if (modules != null) {

            for (Module module : modules) {
                if (module.getId().equals(moduleId)) {
                    return module;
                }
            }
        }

        return null;
    }

    public LicenseControl getLicenseControl() {
        return licenseControl;
    }

    public void setLicenseControl(LicenseControl licenseControl) {
        this.licenseControl = licenseControl;
    }

    public BuildRetention getBuildRetention() {
        return buildRetention;
    }

    public void setBuildRetention(BuildRetention buildRetention) {
        this.buildRetention = buildRetention;
    }

    public List<MatrixParameter> getRunParameters() {
        return runParameters;
    }

    public void setRunParameters(List<MatrixParameter> runParameters) {
        this.runParameters = runParameters;
    }

    public List<PromotionStatus> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<PromotionStatus> statuses) {
        this.statuses = statuses;
    }

    public void addStatus(PromotionStatus promotionStatus) {
        if (statuses == null) {
            statuses = Lists.newArrayList();
        }

        statuses.add(promotionStatus);
    }

    /**
     * Returns the build dependencies of this build
     *
     * @return list of #BuildDependency objects
     */
    public List<BuildDependency> getBuildDependencies() {
        return buildDependencies;
    }

    /**
     * Sets build dependencies for this build
     *
     * @param buildDependencies List of #BuildDependency objects
     */
    public void setBuildDependencies(List<BuildDependency> buildDependencies) {
        this.buildDependencies = buildDependencies;
    }

    /**
     * Adds one #BuildDependency to build dependencies list
     *
     * @param buildDependency the #BuildDependency to add
     */
    public void addBuildDependency(BuildDependency buildDependency) {
        if (buildDependencies == null) {
            buildDependencies = Lists.newArrayList();
        }
        buildDependencies.add(buildDependency);
    }

    public Issues getIssues() {
        return issues;
    }

    public void setIssues(Issues issues) {
        this.issues = issues;
    }

    public Governance getGovernance() {
        return governance;
    }

    public void setGovernance(Governance governance) {
        this.governance = governance;
    }

    public void append(Build other) {
        if (buildAgent == null) {
            setBuildAgent(other.buildAgent);
        }

        appendProperties(other);
        appendModules(other);
        appendBuildDependencies(other);
        if (this.issues == null) {
            this.issues = other.issues;
            return;
        }
        this.issues.append(other.issues);
    }

    private void appendBuildDependencies(Build other) {
        List<BuildDependency> buildDependencies = other.getBuildDependencies();
        if (buildDependencies != null && buildDependencies.size() > 0) {
            if (this.buildDependencies == null) {
                this.setBuildDependencies(buildDependencies);
            } else {
                this.buildDependencies.addAll(buildDependencies);
            }
        }
    }

    private void appendModules(Build other) {
        List<Module> modules = other.getModules();
        if (modules != null && modules.size() > 0) {
            if (this.getModules() == null) {
                this.setModules(modules);
            } else {
                modules.forEach(this::addModule);
            }
        }
    }

    private void appendProperties(Build other) {
        Properties properties = other.getProperties();
        if (properties != null && properties.size() > 0) {
            if (this.getProperties() == null) {
                this.setProperties(properties);
            } else {
                this.getProperties().putAll(properties);
            }
        }
    }

    private void addModule(Module other) {
        List<Module> modules = getModules();
        Module currentModule = modules.stream()
                // Check if there's already a module with the same name.
                .filter(module -> StringUtils.equals(module.getId(), other.getId()))
                .findAny()
                .orElse(null);
        if (currentModule == null) {
            // Append new module.
            modules.add(other);
        } else {
            // Append the other module into the existing module with the same name.
            currentModule.append(other);
        }
    }

    @Override
    public String toString() {
        return "Build{" +
                "version='" + version + '\'' +
                ", name='" + name + '\'' +
                ", number='" + number + '\'' +
                ", type=" + type +
                ", buildAgent=" + buildAgent +
                ", agent=" + agent +
                ", started='" + started + '\'' +
                ", durationMillis=" + durationMillis +
                ", principal='" + principal + '\'' +
                ", artifactoryPrincipal='" + artifactoryPrincipal + '\'' +
                ", artifactoryPluginVersion='" + artifactoryPluginVersion + '\''+
                ", url='" + url + '\'' +
                ", parentName='" + parentName + '\'' +
                ", parentNumber='" + parentNumber + '\'' +
                ", vcs='" + vcs + '\'' +
                ", parentBuildId='" + parentBuildId + '\'' +
                ", licenseControl=" + licenseControl +
                ", buildRetention=" + buildRetention +
                ", runParameters=" + runParameters +
                ", modules=" + modules +
                ", statuses=" + statuses +
                ", buildDependencies=" + buildDependencies +
                ", issues=" + issues +
                ", governance=" + governance +
                '}';
    }
}