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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.api.builder.BuildInfoModuleBuilder;
import org.jfrog.build.api.ci.BuildInfo;
import org.jfrog.build.api.dependency.BuildDependency;
import org.jfrog.build.api.release.PromotionStatus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.jfrog.build.api.BuildBean.ROOT;

/**
 * Represents pure (without logic) schema of build info which contains the build info properties of a typical build.
 * Convert {@link org.jfrog.build.api.ci.BuildInfo} to this class before sending the build info.
 */
@XStreamAlias(ROOT)
@JsonIgnoreProperties(ignoreUnknown = true, value = {"project", "startedMillis"})
public class Build extends BaseBuildBean {

    public static final String STARTED_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private String version = "1.0.1";
    private String name;
    private String number;
    private String project;
    private BuildAgent buildAgent;
    private Agent agent;
    private String started;
    private long startedMillis;
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
     * Returns the project of the build
     *
     * @return Build project
     */
    public String getProject() {
        return project;
    }

    /**
     * Sets the project of the build
     *
     * @param project Build project
     */
    public void setProject(String project) {
        this.project = project;
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
     * Returns the started time of the build in a unit of milliseconds
     *
     * @return Build started time in a unit of milliseconds
     */
    public long getStartedMillis() {
        return startedMillis;
    }

    /**
     * Sets the started time of the build in a unit of milliseconds
     *
     */
    public void setStartedMillis(long startedMillis) {
        this.startedMillis = startedMillis;
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
        startedMillis = startedDate.getTime();
        started = formatBuildStarted(startedMillis);
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
    public String getArtifactoryPluginVersion() {
        return artifactoryPluginVersion;
    }

    /**
     * Sets the Artifactory plugin version of the build
     *
     * @param artifactoryPluginVersion Artifactory plugin version
     */
    public void setArtifactoryPluginVersion(String artifactoryPluginVersion) {
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
     * @deprecated Use {@link Build#getParentName()} and {@link Build#getParentNumber()} instead.
     */
    @Deprecated
    public String getParentBuildId() {
        return parentBuildId;
    }

    /**
     * Sets the parent build ID of the build
     *
     * @param parentBuildId Build parent build ID
     * @deprecated Use {@link Build#setParentName(String)} and {@link Build#setParentNumber(String)}
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
            statuses = new ArrayList<>();
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

    public Issues getIssues() {
        return issues;
    }
    public void setIssues(Issues issues) {
        this.issues = issues;
    }


    @Override
    public String toString() {
        return "Build{" +
                "version='" + version + '\'' +
                ", name='" + name + '\'' +
                ", number='" + number + '\'' +
                ", buildAgent=" + buildAgent +
                ", agent=" + agent +
                ", started='" + started + '\'' +
                ", durationMillis=" + durationMillis +
                ", principal='" + principal + '\'' +
                ", artifactoryPrincipal='" + artifactoryPrincipal + '\'' +
                ", artifactoryPluginVersion='" + artifactoryPluginVersion + '\'' +
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
                '}';
    }

    public BuildInfo ToBuildInfo() {
        BuildInfoBuilder builder = new BuildInfoBuilder(name)
                .number(number)
                .setProject(project)
                .agent(agent == null ? null : new org.jfrog.build.api.ci.Agent(agent.getName(), agent.getVersion()))
                .buildAgent(buildAgent == null ? null : new org.jfrog.build.api.ci.BuildAgent(buildAgent.getName(), buildAgent.getVersion()))
                .started(started)
                .startedMillis(startedMillis)
                .durationMillis(durationMillis)
                .principal(principal)
                .artifactoryPrincipal(artifactoryPrincipal)
                .artifactoryPluginVersion(artifactoryPluginVersion)
                .url(url)
                .parentName(parentName)
                .parentNumber(parentNumber)
                .buildRunParameters(runParameters == null ? null : runParameters.stream().map(rp -> new org.jfrog.build.api.ci.MatrixParameter(rp.getKey(), rp.getValue())).collect(Collectors.toList()))
                .statuses(statuses)
                .properties(getProperties())
                .vcs(vcs == null ? null : vcs.stream().map(Vcs::ToBuildInfoVcs).collect(Collectors.toList()))
                .licenseControl(licenseControl == null ? null : licenseControl.ToBuildInfoLicenseControl())
                .buildRetention(buildRetention == null ? null : buildRetention.ToBuildInfoRetention())
                .issues(issues == null ? null : issues.ToBuildInfoIssues());
        if (modules != null) {
            builder.modules(modules.stream().map(m -> new BuildInfoModuleBuilder().
                    type(m.getType())
                    .id(m.getId())
                    .repository(m.getRepository())
                    .sha1(m.getSha1())
                    .md5(m.getType())
                    .artifacts(m.getArtifacts() == null ? null : m.getArtifacts().stream().map(Artifact::ToBuildInfoArtifact).collect(Collectors.toList()))
                    .dependencies(m.getDependencies() == null ? null : m.getDependencies().stream().map(Dependency::ToBuildDependency).collect(Collectors.toList()))
                    .properties(m.getProperties())
                    .excludedArtifacts(m.getExcludedArtifacts() == null ? null : m.getExcludedArtifacts().stream().map(Artifact::ToBuildInfoArtifact).collect(Collectors.toList()))
                    .build()).collect(Collectors.toList()));
        }
        return builder.build();
    }
}