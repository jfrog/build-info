package org.artifactory.build.api.builder;

import com.google.common.collect.Lists;
import org.artifactory.build.api.Agent;
import org.artifactory.build.api.Build;
import org.artifactory.build.api.BuildType;
import org.artifactory.build.api.Module;

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

    private String version = "1.0.0";
    private String name = "";
    private long number = 0L;
    private BuildType type = BuildType.GENERIC;
    private Agent agent = new Agent("", "");
    private String started = "";
    private long durationMillis = 0L;
    private String principal = "";
    private String artifactoryPrincipal = "";
    private String url = "";
    private String parentBuildId = "";
    private List<Module> modules;
    private Properties properties;

    /**
     * Assembles the build class
     *
     * @return Assembled build
     */
    public Build build() {
        Build build = new Build();
        build.setVersion(version);
        build.setName(name);
        build.setNumber(number);
        build.setType(type);
        build.setAgent(agent);
        build.setStarted(started);
        build.setDurationMillis(durationMillis);
        build.setPrincipal(principal);
        build.setArtifactoryPrincipal(artifactoryPrincipal);
        build.setUrl(url);
        build.setParentBuildId(parentBuildId);
        build.setModules(modules);
        build.setProperties(properties);
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
    public BuildInfoBuilder number(long number) {
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
     * Sets the parent build ID of the build
     *
     * @param parentBuildId Build parent build ID
     * @return Builder instance
     */
    public BuildInfoBuilder parentBuildId(String parentBuildId) {
        this.parentBuildId = parentBuildId;
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