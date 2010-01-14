package org.artifactory.build.api;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import static org.artifactory.build.api.BuildBean.ROOT;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Contains the general build information
 *
 * @author Noam Y. Tenne
 */
@XStreamAlias(ROOT)
public class Build extends BaseBuildBean {

    public static final String STARTED_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private String version = "1.0.0";
    private String name;
    private long number;
    private BuildType type;
    private Agent agent;
    private String started;
    private long durationMillis;
    private String principal;
    private String artifactoryPrincipal;
    private String url;
    private String parentBuildId;

    @XStreamAlias(MODULES)
    private List<Module> modules;

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
    public long getNumber() {
        return number;
    }

    /**
     * Sets the number of the build
     *
     * @param number Build number
     */
    public void setNumber(long number) {
        this.number = number;
    }

    /**
     * Returns the type of the build
     *
     * @return Build type
     */
    public BuildType getType() {
        return type;
    }

    /**
     * Sets the type of the build
     *
     * @param type Build type
     */
    public void setType(BuildType type) {
        this.type = type;
    }

    /**
     * Returns the agent of the build
     *
     * @return Build agent
     */
    public Agent getAgent() {
        return agent;
    }

    /**
     * Sets the agent of the build
     *
     * @param agent Build agent
     */
    public void setAgent(Agent agent) {
        this.agent = agent;
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
        SimpleDateFormat dateFormat = new SimpleDateFormat(STARTED_FORMAT);
        this.started = dateFormat.format(startedDate);
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
     */
    public String getParentBuildId() {
        return parentBuildId;
    }

    /**
     * Sets the parent build ID of the build
     *
     * @param parentBuildId Build parent build ID
     */
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
}