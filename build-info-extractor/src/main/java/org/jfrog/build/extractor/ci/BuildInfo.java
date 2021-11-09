
package org.jfrog.build.extractor.ci;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.dependency.BuildDependency;
import org.jfrog.build.api.release.PromotionStatus;
import org.jfrog.build.extractor.builder.BuildInfoBuilder;
import org.jfrog.build.extractor.builder.ModuleBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.jfrog.build.extractor.ci.BuildBean.ROOT;

/**
 * A temporary build-info for CI use (e.g. Artifactory jenkins plugin, Maven plugin, etc.).
 *  BuildInfo class should be converted to {@link org.jfrog.build.api.Build} before publishing / getting build-info from Artifactory.
 */
@XStreamAlias(ROOT)
@JsonIgnoreProperties(ignoreUnknown = true, value = {"project", "startedMillis"})
public class BuildInfo extends BaseBuildBean {

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
     * @return BuildInfo version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the version of the build
     *
     * @param version BuildInfo version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Returns the name of the build
     *
     * @return BuildInfo name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the build
     *
     * @param name BuildInfo name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the number of the build
     *
     * @return BuildInfo number
     */
    public String getNumber() {
        return number;
    }

    /**
     * Sets the number of the build
     *
     * @param number BuildInfo number
     */
    public void setNumber(String number) {
        this.number = number;
    }

    /**
     * Returns the project of the build
     *
     * @return BuildInfo project
     */
    public String getProject() {
        return project;
    }

    /**
     * Sets the project of the build
     *
     * @param project BuildInfo project
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
     * @return BuildInfo agent
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
     * @return BuildInfo started time
     */
    public String getStarted() {
        return started;
    }

    /**
     * Sets the started time of the build
     *
     * @param started BuildInfo started time
     */
    public void setStarted(String started) {
        this.started = started;
    }

    /**
     * Returns the started time of the build in a unit of milliseconds
     *
     * @return BuildInfo started time in a unit of milliseconds
     */
    public long getStartedMillis() {
        return startedMillis;
    }

    /**
     * Sets the started time of the build in a unit of milliseconds
     */
    public void setStartedMillis(long startedMillis) {
        this.startedMillis = startedMillis;
    }

    /**
     * Sets the build start time
     *
     * @param startedDate BuildInfo start date to set
     */
    public void setStartedDate(Date startedDate) {
        startedMillis = startedDate.getTime();
        started = formatBuildStarted(startedMillis);
    }

    /**
     * Returns the duration milliseconds of the build
     *
     * @return BuildInfo duration milliseconds
     */
    public long getDurationMillis() {
        return durationMillis;
    }

    /**
     * Sets the duration milliseconds of the build
     *
     * @param durationMillis BuildInfo duration milliseconds
     */
    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    /**
     * Returns the principal of the build
     *
     * @return BuildInfo principal
     */
    public String getPrincipal() {
        return principal;
    }

    /**
     * Sets the principal of the build
     *
     * @param principal BuildInfo principal
     */
    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    /**
     * Returns the Artifactory principal of the build
     *
     * @return BuildInfo Artifactory principal
     */
    public String getArtifactoryPrincipal() {
        return artifactoryPrincipal;
    }

    /**
     * Sets the Artifactory principal of the build
     *
     * @param artifactoryPrincipal BuildInfo Artifactory principal
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
     * @return BuildInfo URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the URL of the build
     *
     * @param url BuildInfo URL
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Returns the parent build ID of the build
     *
     * @return BuildInfo parent build ID
     * @deprecated Use {@link BuildInfo#getParentName()} and {@link BuildInfo#getParentNumber()} instead.
     */
    @Deprecated
    public String getParentBuildId() {
        return parentBuildId;
    }

    /**
     * Sets the parent build ID of the build
     *
     * @param parentBuildId BuildInfo parent build ID
     * @deprecated Use {@link BuildInfo#setParentName(String)} and {@link BuildInfo#setParentNumber(String)}
     * instead.
     */
    @Deprecated
    public void setParentBuildId(String parentBuildId) {
        this.parentBuildId = parentBuildId;
    }

    /**
     * Returns the modules of the build
     *
     * @return BuildInfo modules
     */
    public List<Module> getModules() {
        return modules;
    }

    /**
     * Sets the modules of the build
     *
     * @param modules BuildInfo modules
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

    /**
     * Adds one #BuildDependency to build dependencies list
     *
     * @param buildDependency the #BuildDependency to add
     */
    public void addBuildDependency(BuildDependency buildDependency) {
        if (buildDependencies == null) {
            buildDependencies = new ArrayList<>();
        }
        buildDependencies.add(buildDependency);
    }

    public Issues getIssues() {
        return issues;
    }

    public void setIssues(Issues issues) {
        this.issues = issues;
    }

    public void append(BuildInfo other) {
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

    private void appendBuildDependencies(BuildInfo other) {
        List<BuildDependency> buildDependencies = other.getBuildDependencies();
        if (buildDependencies != null && buildDependencies.size() > 0) {
            if (this.buildDependencies == null) {
                this.setBuildDependencies(buildDependencies);
            } else {
                this.buildDependencies.addAll(buildDependencies);
            }
        }
    }

    private void appendModules(BuildInfo other) {
        List<Module> modules = other.getModules();
        if (modules != null && modules.size() > 0) {
            if (this.getModules() == null) {
                this.setModules(modules);
            } else {
                modules.forEach(this::addModule);
            }
        }
    }

    private void appendProperties(BuildInfo other) {
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
        return "BuildInfo{" +
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
                ", buildRetention=" + buildRetention +
                ", runParameters=" + runParameters +
                ", modules=" + modules +
                ", statuses=" + statuses +
                ", buildDependencies=" + buildDependencies +
                ", issues=" + issues +
                '}';
    }

    public Build ToBuild() {
        org.jfrog.build.api.builder.BuildInfoBuilder builder = new org.jfrog.build.api.builder.BuildInfoBuilder(name)
                .number(number)
                .setProject(project)
                .agent(agent == null ? null : new org.jfrog.build.api.Agent(agent.getName(), agent.getVersion()))
                .buildAgent(buildAgent == null ? null : new org.jfrog.build.api.BuildAgent(buildAgent.getName(), buildAgent.getVersion()))
                .started(started)
                .startedMillis(startedMillis)
                .durationMillis(durationMillis)
                .principal(principal)
                .artifactoryPrincipal(artifactoryPrincipal)
                .artifactoryPluginVersion(artifactoryPluginVersion)
                .url(url)
                .parentName(parentName)
                .parentNumber(parentNumber)
                .buildRunParameters(runParameters == null ? null : runParameters.stream().map(rp -> new org.jfrog.build.api.MatrixParameter(rp.getKey(), rp.getValue())).collect(Collectors.toList()))
                .statuses(statuses)
                .properties(getProperties())
                .vcs(vcs == null ? null : vcs.stream().map(Vcs::ToBuildVcs).collect(Collectors.toList()))
                .buildRetention(buildRetention == null ? null : buildRetention.ToBuildRetention())
                .issues(issues == null ? null : issues.ToBuildIssues());
        if (modules != null) {
            builder.modules(modules.stream().map(m -> {
                return new org.jfrog.build.api.builder.ModuleBuilder().
                        type(m.getType())
                        .id(m.getId())
                        .repository(m.getRepository())
                        .sha1(m.getSha1())
                        .md5(m.getType())
                        .artifacts(m.getArtifacts() == null ? null : m.getArtifacts().stream().map(Artifact::ToBuildArtifact).collect(Collectors.toList()))
                        .dependencies(m.getDependencies() == null ? null : m.getDependencies().stream().map(Dependency::ToBuildDependency).collect(Collectors.toList()))
                        .properties(m.getProperties())
                        .excludedArtifacts(m.getExcludedArtifacts() == null ? null : m.getExcludedArtifacts().stream().map(Artifact::ToBuildArtifact).collect(Collectors.toList()))
                        .build();
            }).collect(Collectors.toList()));
        }
        return builder.build();
    }

    public static BuildInfo ToBuildInfo( org.jfrog.build.api.Build build) {
        BuildInfoBuilder builder = new BuildInfoBuilder(build.getName());
                 builder
                .number(build.getNumber())
                .setProject(build.getProject())
                .agent(build.getAgent() == null ? null : new Agent(build.getAgent().getName(), build.getAgent().getVersion()))
                .buildAgent(build.getBuildAgent() == null ? null : new BuildAgent(build.getBuildAgent().getName(), build.getBuildAgent().getVersion()))
                .started(build.getStarted())
                .startedMillis(build.getStartedMillis())
                .durationMillis(build.getDurationMillis())
                .principal(build.getPrincipal())
                .artifactoryPrincipal(build.getArtifactoryPrincipal())
                .artifactoryPluginVersion(build.getArtifactoryPluginVersion())
                .url(build.getUrl())
                .parentName(build.getParentName())
                .parentNumber(build.getParentNumber())
                .buildRunParameters(build.getRunParameters() == null ? null : build.getRunParameters().stream().map(rp -> new MatrixParameter(rp.getKey(), rp.getValue())).collect(Collectors.toList()))
                .statuses(build.getStatuses())
                .properties(build.getProperties())
                .vcs(build.getVcs() == null ? null : build.getVcs().stream().map(Vcs::ToBuildInfoVcs).collect(Collectors.toList()))
                .buildRetention(build.getBuildRetention() == null ? null : BuildRetention.ToBuildInfoRetention(build.getBuildRetention()))
                .issues(build.getIssues() == null ? null : Issues.ToBuildInfoIssues(build.getIssues()));
        if (build.getModules() != null) {
            builder.modules(build.getModules().stream().map(m -> new ModuleBuilder().
                    type(m.getType())
                    .id(m.getId())
                    .repository(m.getRepository())
                    .sha1(m.getSha1())
                    .md5(m.getMd5())
                    .artifacts(m.getArtifacts() == null ? null : m.getArtifacts().stream().map(Artifact::ToBuildInfoArtifact).collect(Collectors.toList()))
                    .dependencies(m.getDependencies() == null ? null : m.getDependencies().stream().map(Dependency::ToBuildDependency).collect(Collectors.toList()))
                    .properties(m.getProperties())
                    .excludedArtifacts(m.getExcludedArtifacts() == null ? null : m.getExcludedArtifacts().stream().map(Artifact::ToBuildInfoArtifact).collect(Collectors.toList()))
                    .build()).collect(Collectors.toList()));
        }
        return builder.build();
    }
}