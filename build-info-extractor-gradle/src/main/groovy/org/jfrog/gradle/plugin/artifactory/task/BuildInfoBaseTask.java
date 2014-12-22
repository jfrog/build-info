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
import org.gradle.util.ConfigureUtil;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.build.client.ArtifactSpec;
import org.jfrog.build.client.ArtifactSpecs;
import org.jfrog.build.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.client.ArtifactoryClientConfiguration;
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.client.IncludeExcludePatterns;
import org.jfrog.build.client.PatternMatcher;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPluginUtil;
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention;
import org.jfrog.gradle.plugin.artifactory.dsl.PropertiesConfig;
import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig;
import org.jfrog.gradle.plugin.artifactory.extractor.GradleArtifactoryClientConfigUpdater;
import org.jfrog.gradle.plugin.artifactory.extractor.GradleBuildInfoExtractor;
import org.jfrog.gradle.plugin.artifactory.extractor.GradleClientLogger;
import org.jfrog.gradle.plugin.artifactory.extractor.GradleDeployDetails;
import org.jfrog.gradle.plugin.artifactory.extractor.PublishArtifactInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
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
    private static final Logger log = Logging.getLogger(BuildInfoBaseTask.class);

    public static final String BUILD_INFO_TASK_NAME = "artifactoryPublish";
    public static final String PUBLISH_ARTIFACTS = "publishArtifacts";
    public static final String PUBLISH_BUILD_INFO = "publishBuildInfo";
    public static final String ARCHIVES_BASE_NAME = "archivesBaseName";

    @Input
    protected final Multimap<String, CharSequence> properties = ArrayListMultimap.create();

    @Input
    protected final ArtifactSpecs artifactSpecs = new ArtifactSpecs();

    @Input
    private boolean skip = false;

    private final Map<String, Boolean> flags = Maps.newHashMap();

    protected Map<String, String> defaultProps;

    protected final Set<GradleDeployDetails> deployDetails = Sets.newHashSet();

    @Input
    @Optional
    @Nullable
    public Boolean getPublishBuildInfo() {
        return getFlag(PUBLISH_BUILD_INFO);
    }

    public void setPublishBuildInfo(Object publishBuildInfo) {
        setFlag(PUBLISH_BUILD_INFO, toBoolean(publishBuildInfo));
    }

    @Input
    @Optional
    @Nullable
    public Boolean getPublishArtifacts() {
        return getFlag(PUBLISH_ARTIFACTS);
    }

    public void setPublishArtifacts(Object publishArtifacts) {
        setFlag(PUBLISH_ARTIFACTS, toBoolean(publishArtifacts));
    }

    protected Boolean toBoolean(Object publishIvy) {
        return Boolean.valueOf(publishIvy.toString());
    }

    @Nullable
    protected Boolean getFlag(String flagName) {
        return flags.get(flagName);
    }

    protected void setFlag(String flagName, Boolean newValue) {
        flags.put(flagName, newValue);
    }

    public boolean isSkip() {
        return skip;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public Set<GradleDeployDetails> getDeployDetails() {
        return deployDetails;
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

    //For testing
    public ArtifactSpecs getArtifactSpecs() {
        return artifactSpecs;
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

        //Configure the task using the defaults closure (delegate to the task)
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

        checkDependsOnArtifactsToPublish(project, acc);
    }

    protected abstract void checkDependsOnArtifactsToPublish(Project project, ArtifactoryClientConfiguration acc);

    @Nonnull
    protected Boolean isPublishArtifacts(ArtifactoryClientConfiguration acc) {
        Boolean publishArtifacts = getPublishArtifacts();
        if (publishArtifacts == null) {
            return acc.publisher.isPublishArtifacts();
        }
        return publishArtifacts;
    }

    @Nonnull
    protected Boolean isPublishBuildInfo(ArtifactoryClientConfiguration acc) {
        Boolean publishBuildInfo = getPublishBuildInfo();
        if (publishBuildInfo == null) {
            return acc.publisher.isPublishBuildInfo();
        }
        return publishBuildInfo;
    }

    protected ArtifactoryClientConfiguration getArtifactoryClientConfiguration() {
        return ArtifactoryPluginUtil.getArtifactoryConvention(getProject()).getClientConfig();
    }

    protected Map<String, String> getPropsToAdd(PublishArtifactInfo artifact, String publicationName) {
        if (defaultProps == null) {
            defaultProps = Maps.newHashMap();
            addProps(defaultProps, properties);
            //Add the publisher properties
            ArtifactoryClientConfiguration clientConf = getArtifactoryClientConfiguration();
            defaultProps.putAll(clientConf.publisher.getMatrixParams());
        }

        Map<String, String> propsToAdd = Maps.newHashMap(defaultProps);
        //Apply artifact-specific props from the artifact specs
        Project project = getProject();
        ArtifactSpec spec =
                ArtifactSpec.builder().configuration(publicationName)
                        .group(project.getGroup().toString())
                        .name(project.getName()).version(project.getVersion().toString())
                        .classifier(artifact.getClassifier())
                        .type(artifact.getType()).build();
        Multimap<String, CharSequence> artifactSpecsProperties = artifactSpecs.getProperties(spec);
        addProps(propsToAdd, artifactSpecsProperties);
        return propsToAdd;
    }

    private void addProps(Map<String, String> target, Multimap<String, CharSequence> props) {
        for (Map.Entry<String, CharSequence> entry : props.entries()) {
            // Make sure all GString are now Java Strings
            String key = entry.getKey();
            String value = entry.getValue().toString();
            //Accumulate multi-value props
            if (!target.containsKey(key)) {
                target.put(key, value);
            } else {
                value = target.get(key) + ", " + value;
                target.put(key, value);
            }
        }
    }

    protected abstract void collectDescriptorsAndArtifactsForUpload() throws IOException;

    /**
     * This method will be activated only at the end of the build, when we reached the root project.
     *
     * @throws java.io.IOException In case the deployment fails.
     */
    protected void prepareAndDeploy() throws IOException {
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

        Set<GradleDeployDetails> allDeployDetails = Sets.newHashSet();

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
                 * if the {@link org.jfrog.build.client.ClientProperties#PROP_PUBLISH_ARTIFACT} is set the true,
                 * The uploadArchives task will be triggered ONLY at the end, ensuring that the artifacts will be
                 * published only after a successful build. This is done before the build-info is sent.
                 */

                IncludeExcludePatterns patterns = new IncludeExcludePatterns(
                        acc.publisher.getIncludePatterns(),
                        acc.publisher.getExcludePatterns());
                configureProxy(acc, client);
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
                log.debug("Publishing build info to artifactory at: '{}'", contextUrl);
                /**
                 * After all the artifacts were uploaded successfully the next task is to send the build-info
                 * object.
                 */
                // If export property set always save the file before sending it to artifactory
                exportBuildInfo(build, getExportFile(acc));
                client.sendBuildInfo(build);
            }
        } finally {
            client.shutdown();
        }
    }

    protected String getModuleName() {
        Project project = getProject();
        //Take into account the archivesBaseName if applied to the project by the Java plugin
        if (project.hasProperty(ARCHIVES_BASE_NAME)) {
            return project.property(ARCHIVES_BASE_NAME).toString();
        }
        return project.getName();
    }

    protected File getExportFile(ArtifactoryClientConfiguration clientConf) {
        String fileExportPath = clientConf.getExportFile();
        if (StringUtils.isNotBlank(fileExportPath)) {
            return new File(fileExportPath);
        }
        Project rootProject = getProject().getRootProject();
        return new File(rootProject.getBuildDir(), "build-info.json");
    }

    protected void configureProxy(ArtifactoryClientConfiguration clientConf, ArtifactoryBuildInfoClient client) {
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

    protected void exportBuildInfo(Build build, File toFile) throws IOException {
        log.debug("Exporting generated build info to '{}'", toFile.getAbsolutePath());
        BuildInfoExtractorUtils.saveBuildInfoToFile(build, toFile);
    }

    @TaskAction
    public void collectProjectBuildInfo() throws IOException {
        try {
            log.debug("Task '{}' activated", getPath());
            // Only the last buildInfo execution activate the deployment
            List<BuildInfoBaseTask> orderedTasks = getAllBuildInfoTasks();
            List<BuildInfoBaseTask> remainingTasks = new ArrayList<BuildInfoBaseTask>();
            for(BuildInfoBaseTask task : getAllBuildInfoTasks()) {
              if(!task.getState().getExecuted()) {
                remainingTasks.add(task);
              }
            }
            if (orderedTasks.indexOf(this) == -1) {
                log.error("Could not find my own task {} in the task graph!", getPath());
                return;
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
     * Analyze the task graph ordered and extract a list of build info tasks
     *
     * @return An ordered list of build info tasks
     */
    protected List<BuildInfoBaseTask> getAllBuildInfoTasks() {
        List<BuildInfoBaseTask> result = new ArrayList<BuildInfoBaseTask>();
        for (Task task : getProject().getGradle().getTaskGraph().getAllTasks()) {
            if (task instanceof BuildInfoBaseTask) {
                result.add((BuildInfoBaseTask) task);
            }
        }
        return result;
    }

    public void properties(Closure closure) {
        Project project = getProject();
        PropertiesConfig propertiesConfig = new PropertiesConfig(project);
        ConfigureUtil.configure(closure, propertiesConfig);
        artifactSpecs.addAll(propertiesConfig.getArtifactSpecs());
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

    public abstract boolean hasModules();
}
