package org.jfrog.gradle.plugin.artifactory.task;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import groovy.lang.Closure;
import org.apache.commons.lang.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskState;
import org.gradle.util.ConfigureUtil;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.clientConfiguration.ArtifactSpecs;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.IncludeExcludePatterns;
import org.jfrog.build.extractor.clientConfiguration.PatternMatcher;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPluginUtil;
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention;
import org.jfrog.gradle.plugin.artifactory.dsl.PropertiesConfig;
import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig;
import org.jfrog.gradle.plugin.artifactory.extractor.GradleArtifactoryClientConfigUpdater;
import org.jfrog.gradle.plugin.artifactory.extractor.GradleBuildInfoExtractor;
import org.jfrog.gradle.plugin.artifactory.extractor.GradleClientLogger;
import org.jfrog.gradle.plugin.artifactory.extractor.GradleDeployDetails;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Date: 3/20/13 Time: 10:32 AM
 *
 * @author freds
 */
public abstract class BuildInfoBaseTask extends DefaultTask {
    public static final String BUILD_INFO_TASK_NAME = "artifactoryPublish";
    public static final String PUBLISH_ARTIFACTS = "publishArtifacts";
    public static final String PUBLISH_BUILD_INFO = "publishBuildInfo";
    public static final String PUBLISH_IVY = "publishIvy";
    public static final String PUBLISH_POM = "publishPom";

    private static final Logger log = Logging.getLogger(BuildInfoBaseTask.class);
    private final Map<String, Boolean> flags = Maps.newHashMap();
    public final Set<GradleDeployDetails> deployDetails = Sets.newHashSet();

    public abstract void checkDependsOnArtifactsToPublish();

    public abstract void collectDescriptorsAndArtifactsForUpload() throws IOException;

    public abstract boolean hasModules();

    private final Multimap<String, CharSequence> properties = ArrayListMultimap.create();

    @Input
    public Multimap<String, CharSequence> getProperties() {
        return properties;
    }

    @Input
    public final ArtifactSpecs artifactSpecs = new ArtifactSpecs();

    @Input
    public boolean skip = false;

    @Input
    @Optional
    @Nullable
    public Boolean getPublishBuildInfo() {
        return getFlag(PUBLISH_BUILD_INFO);
    }

    @Input
    @Optional
    @Nullable
    public Boolean getPublishArtifacts() {
        return getFlag(PUBLISH_ARTIFACTS);
    }

    @Input
    @Optional
    @Nullable
    public Boolean getPublishIvy() {
        return getFlag(PUBLISH_IVY);
    }

    @Input
    @Optional
    @Nullable
    public Boolean getPublishPom() {
        return getFlag(PUBLISH_POM);
    }

    @TaskAction
    public void collectProjectBuildInfo() throws IOException {
        try {
            log.debug("Task '{}' activated", getPath());
            // Only the last buildInfo execution activate the deployment
            List<BuildInfoBaseTask> orderedTasks = getAllBuildInfoTasks();
            if (orderedTasks.indexOf(this) == -1) {
                log.error("Could not find my own task {} in the task graph!", getPath());
                return;
            }

            List<BuildInfoBaseTask> remainingTasks = new ArrayList<BuildInfoBaseTask>();
            for (BuildInfoBaseTask task : getAllBuildInfoTasks()) {
                if (!isTaskExecuted(task)) {
                    remainingTasks.add(task);
                }
            }

            if (remainingTasks.size() <= 1) {
                log.debug("Starting build info extraction for project '{}' using last task in graph '{}'",
                        new Object[]{getProject().getPath(), getPath()});
                prepareAndDeploy();
            }
        } finally {
            String propertyFilePath = System.getenv(BuildInfoConfigProperties.PROP_PROPS_FILE);
            if (StringUtils.isNotBlank(propertyFilePath)) {
                File file = new File(propertyFilePath);
                if (file.exists()) {
                    file.delete();
                }
            }
        }
    }

    /**
     * Determines if the BuildInfoBaseTask has already been executed.
     * This methods wraps Gradle's task.getState().getExecuted().
     * @param task  The BuildInfoBaseTask
     * @return      true if the task has already been executed in this build.
     */
    private boolean isTaskExecuted(BuildInfoBaseTask task) {
        try {
            return task.getState().getExecuted();
        } catch (NoSuchMethodError error) {
            // Compatibility with older versions of Gradle:
            try {
                Method m = task.getClass().getMethod("getState");
                TaskState state = (TaskState)m.invoke(task);
                return state.getExecuted();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void projectsEvaluated() {
        Project project = getProject();
        if (isSkip()) {
            log.debug("Artifactory plugin artifactoryPublish task '{}' skipped for project '{}'.",
                    this.getPath(), project.getName());
            return;
        }
        ArtifactoryPluginConvention convention = ArtifactoryPluginUtil.getArtifactoryConvention(project);
        ArtifactoryClientConfiguration acc = convention.getClientConfig();
        artifactSpecs.addAll(acc.publisher.getArtifactSpecs());

        //Configure the task using the "defaults" closure (delegate to the task)
        PublisherConfig config = convention.getPublisherConfig();
        if (config != null) {
            Closure defaultsClosure = config.getDefaultsClosure();
            ConfigureUtil.configure(defaultsClosure, this);
        }

        //Depend on buildInfo task in sub-projects
        for (Project sub : project.getSubprojects()) {
            Task subBiTask = sub.getTasks().findByName(BUILD_INFO_TASK_NAME);
            if (subBiTask != null) {
                dependsOn(subBiTask);
            }
        }

        checkDependsOnArtifactsToPublish();
    }

    public boolean isSkip() {
        return skip;
    }

    public void setProperties(Map<String, CharSequence> props) {
        if (props == null || props.isEmpty()) {
            return;
        }
        for (Map.Entry<String, CharSequence> entry : props.entrySet()) {
            // The key cannot be lazy eval, but we keep the value as GString as long as possible
            String key = entry.getKey();
            if (StringUtils.isNotBlank(key)) {
                CharSequence value = entry.getValue();
                if (value != null) {
                    // Make sure all GString are now Java Strings for key,
                    // and don't call toString for value (keep lazy eval as long as possible)
                    // So, don't use HashMultimap this will call equals on the GString
                    this.properties.put(key, value);
                }
            }
        }
    }

    public Set<GradleDeployDetails> getDeployDetails() {
        return deployDetails;
    }

    //For testing
    public ArtifactSpecs getArtifactSpecs() {
        return artifactSpecs;
    }

    /**
     * Setters (Object and DSL)
     **/

    public void properties(Closure closure) {
        Project project = getProject();
        PropertiesConfig propertiesConfig = new PropertiesConfig(project);
        ConfigureUtil.configure(closure, propertiesConfig);
        artifactSpecs.addAll(propertiesConfig.getArtifactSpecs());
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public void setPublishIvy(Object publishIvy) {
        setFlag(PUBLISH_IVY, toBoolean(publishIvy));
    }

    public void setPublishPom(Object publishPom) {
        setFlag(PUBLISH_POM, toBoolean(publishPom));
    }

    //Publish build-info to Artifactory (true by default)
    public void setPublishBuildInfo(Object publishBuildInfo) {
        setFlag(PUBLISH_BUILD_INFO, toBoolean(publishBuildInfo));
    }

    //Publish artifacts to Artifactory (true by default)
    public void setPublishArtifacts(Object publishArtifacts) {
        setFlag(PUBLISH_ARTIFACTS, toBoolean(publishArtifacts));
    }

    /**
     * Analyze the task graph ordered and extract a list of build info tasks
     *
     * @return An ordered list of build info tasks
     */
    private List<BuildInfoBaseTask> getAllBuildInfoTasks() {
        List<BuildInfoBaseTask> result = new ArrayList<BuildInfoBaseTask>();
        for (Task task : getProject().getGradle().getTaskGraph().getAllTasks()) {
            if (task instanceof BuildInfoBaseTask) {
                result.add((BuildInfoBaseTask) task);
            }
        }
        return result;
    }

    private ArtifactoryClientConfiguration getArtifactoryClientConfiguration() {
        return ArtifactoryPluginUtil.getArtifactoryConvention(getProject()).getClientConfig();
    }

    private void configureProxy(ArtifactoryClientConfiguration clientConf, ArtifactoryBuildInfoClient client) {
        ArtifactoryClientConfiguration.ProxyHandler proxy = clientConf.proxy;
        String proxyHost = proxy.getHost();
        if (StringUtils.isNotBlank(proxyHost) && proxy.getPort() != null) {
            log.debug("Found proxy host '{}'", proxyHost);
            String proxyUserName = proxy.getUsername();
            if (StringUtils.isNotBlank(proxyUserName)) {
                log.debug("Found proxy user name '{}'", proxyUserName);
                client.setProxyConfiguration(proxyHost, proxy.getPort(), proxyUserName, proxy.getPassword());
            } else {
                log.debug("No proxy user name and password found, using anonymous proxy");
                client.setProxyConfiguration(proxyHost, proxy.getPort());
            }
        }
    }

    protected void configConnectionTimeout(ArtifactoryClientConfiguration clientConf, ArtifactoryBuildInfoClient client){
        if(clientConf.getTimeout() != null) {
            client.setConnectionTimeout(clientConf.getTimeout());
        }
    }

    private File getExportFile(ArtifactoryClientConfiguration clientConf) {
        String fileExportPath = clientConf.getExportFile();
        if (StringUtils.isNotBlank(fileExportPath)) {
            return new File(fileExportPath);
        }
        Project rootProject = getProject().getRootProject();
        return new File(rootProject.getBuildDir(), "build-info.json");
    }

    private void exportBuildInfo(Build build, File toFile) throws IOException {
        log.debug("Exporting generated build info to '{}'", toFile.getAbsolutePath());
        BuildInfoExtractorUtils.saveBuildInfoToFile(build, toFile);
    }

    @Nonnull
    private Boolean isPublishArtifacts(ArtifactoryClientConfiguration acc) {
        Boolean publishArtifacts = getPublishArtifacts();
        if (publishArtifacts == null) {
            return acc.publisher.isPublishArtifacts();
        }
        return publishArtifacts;
    }

    @Nonnull
    private Boolean isPublishBuildInfo(ArtifactoryClientConfiguration acc) {
        Boolean publishBuildInfo = getPublishBuildInfo();
        if (publishBuildInfo == null) {
            return acc.publisher.isPublishBuildInfo();
        }
        return publishBuildInfo;
    }

    @Nullable
    private Boolean getFlag(String flagName) {
        return flags.get(flagName);
    }

    private Boolean toBoolean(Object o) {
        return Boolean.valueOf(o.toString());
    }

    /**
     * This method will be activated only at the "end" of the build, when we reached the root project.
     *
     * @throws java.io.IOException In case the deployment fails.
     */
    private void prepareAndDeploy() throws IOException {
        ArtifactoryClientConfiguration acc = getArtifactoryClientConfiguration();
        // Reset the default properties, they may have changed
        GradleArtifactoryClientConfigUpdater.setMissingBuildAttributes(acc, getProject().getRootProject());

        String contextUrl = acc.publisher.getContextUrl();
        log.debug("Context URL for deployment '{}", contextUrl);
        String username = acc.publisher.getUsername();
        String password = acc.publisher.getPassword();
        if (StringUtils.isBlank(username)) {
            username = "";
        }
        if (StringUtils.isBlank(password)) {
            password = "";
        }

        //Sort all the deploy artifacts by the natural ordering of GradleDeployDetails
        Set<GradleDeployDetails> allDeployDetails = Sets.newTreeSet();

        // Update the artifacts for all project build info task
        List<BuildInfoBaseTask> orderedTasks = getAllBuildInfoTasks();
        for (BuildInfoBaseTask bit : orderedTasks) {
            if (bit.getDidWork()) {
                bit.collectDescriptorsAndArtifactsForUpload();
                allDeployDetails.addAll(bit.deployDetails);
            }
        }

        ArtifactoryBuildInfoClient client =
                new ArtifactoryBuildInfoClient(contextUrl, username, password, new GradleClientLogger(log));
        try {
            if (isPublishArtifacts(acc)) {
                log.debug("Uploading artifacts to Artifactory at '{}'", contextUrl);

                /**
                 * if the {@link org.jfrog.build.extractor.clientConfiguration.ClientProperties#PROP_PUBLISH_ARTIFACT} is set to true,
                 * The uploadArchives task will be triggered ONLY at the end, ensuring that the artifacts will be
                 * published only after a successful build. This is done before the build-info is sent.
                 */

                IncludeExcludePatterns patterns = new IncludeExcludePatterns(
                        acc.publisher.getIncludePatterns(),
                        acc.publisher.getExcludePatterns());
                configureProxy(acc, client);
                configConnectionTimeout(acc, client);
                deployArtifacts(allDeployDetails, client, patterns);
            }

            //Extract build info and update the clientConf info accordingly (build name, num, etc.)
            GradleBuildInfoExtractor gbie = new GradleBuildInfoExtractor(acc, allDeployDetails);
            Build build = gbie.extract(getProject().getRootProject());
            /**
             * The build-info will be always written to a file in its JSON form.
             */
            exportBuildInfo(build, getExportFile(acc));
            if (isPublishBuildInfo(acc)) {
                // If export property set always save the file before sending it to artifactory
                exportBuildInfo(build, getExportFile(acc));
                if (acc.info.isIncremental()) {
                    log.debug("Publishing build info modules to artifactory at: '{}'", contextUrl);
                    client.sendModuleInfo(build);
                } else {
                    log.debug("Publishing build info to artifactory at: '{}'", contextUrl);
                    client.sendBuildInfo(build);
                }
            }
        } finally {
            client.shutdown();
        }
    }

    private void deployArtifacts(Set<GradleDeployDetails> allDeployDetails, ArtifactoryBuildInfoClient client,
                                 IncludeExcludePatterns patterns)
            throws IOException {
        for (GradleDeployDetails detail : allDeployDetails) {
            DeployDetails deployDetails = detail.getDeployDetails();
            String artifactPath = deployDetails.getArtifactPath();
            if (PatternMatcher.pathConflicts(artifactPath, patterns)) {
                log.log(LogLevel.LIFECYCLE, "Skipping the deployment of '" + artifactPath +
                        "' due to the defined include-exclude patterns.");
                continue;
            }
            client.deployArtifact(deployDetails);
        }
    }

    private void setFlag(String flagName, Boolean newValue) {
        flags.put(flagName, newValue);
    }
}
