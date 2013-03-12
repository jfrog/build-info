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

package org.jfrog.gradle.plugin.artifactory.extractor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import groovy.lang.Closure;
import org.apache.commons.lang.StringUtils;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.tools.ant.util.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.artifacts.PublishArtifactSet;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.plugins.MavenPluginConvention;
import org.gradle.api.publish.ivy.tasks.GenerateIvyDescriptor;
import org.gradle.api.tasks.*;
import org.gradle.util.ConfigureUtil;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.client.*;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPluginUtil;
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention;
import org.jfrog.gradle.plugin.artifactory.dsl.PropertiesConfig;
import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Tomer Cohen
 */
public class BuildInfoTask extends DefaultTask {

    private static final Logger log = Logging.getLogger(BuildInfoTask.class);

    public static final String BUILD_INFO_TASK_NAME = "artifactoryPublish";

    public static final String PUBLISH_IVY = "publishIvy";
    public static final String PUBLISH_POM = "publishPom";
    public static final String PUBLISH_ARTIFACTS = "publishArtifacts";
    public static final String PUBLISH_BUILD_INFO = "publishBuildInfo";
    public static final String ARCHIVES_BASE_NAME = "archivesBaseName";

    @InputFile
    @Optional
    private File ivyDescriptor;

    @InputFile
    @Optional
    private File mavenDescriptor;

    @InputFiles
    @Optional
    private Set<Configuration> publishConfigurations = Sets.newHashSet();

    @Input
    private final Multimap<String, CharSequence> properties = ArrayListMultimap.create();

    @Input
    private final ArtifactSpecs artifactSpecs = new ArtifactSpecs();

    @Input
    private boolean skip = false;

    private final Map<String, Boolean> flags = Maps.newHashMap();

    private Map<String, String> defaultProps;
    private boolean publishConfigsSpecified;

    //TODO: [by yl] Task flags are not merged into ArtifactoryClientConfiguration, leading to double checking -
    // against the class and against acc (e.g. isPublishIvy() here)
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

    @Input
    @Optional
    @Nullable
    public Boolean getPublishIvy() {
        // TODO: Need to take the default from ACC
        return getFlag(PUBLISH_IVY);
    }

    public void setPublishIvy(Object publishIvy) {
        setFlag(PUBLISH_IVY, toBoolean(publishIvy));
    }

    private Boolean toBoolean(Object publishIvy) {
        return Boolean.valueOf(publishIvy.toString());
    }

    @Input
    @Optional
    @Nullable
    public Boolean getPublishPom() {
        return getFlag(PUBLISH_POM);
    }

    public void setPublishPom(Object publishPom) {
        setFlag(PUBLISH_POM, toBoolean(publishPom));
    }

    @Nullable
    private Boolean getFlag(String flagName) {
        return flags.get(flagName);
    }

    private void setFlag(String flagName, Boolean newValue) {
        flags.put(flagName, newValue);
    }

    public boolean isSkip() {
        return skip;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
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

    public void publishConfigs(Object... confs) {
        if (confs == null) {
            return;
        }
        for (Object conf : confs) {
            if (conf instanceof CharSequence) {
                Configuration projectConfig = getProject().getConfigurations().findByName(conf.toString());
                if (projectConfig != null) {
                    publishConfigurations.add(projectConfig);
                } else {
                    log.error("Configuration named '{}' does not exist for project '{}' in task '{}'.",
                            new Object[]{conf, getProject().getPath(), getPath()});
                }
            } else if (conf instanceof Configuration) {
                publishConfigurations.add((Configuration) conf);
            } else {
                log.error("Configuration type '{}' not supported in task '{}'.",
                        new Object[]{conf.getClass().getName(), getPath()});
            }
        }
        publishConfigsSpecified = true;
    }

    public Set<Configuration> getPublishConfigurations() {
        return publishConfigurations;
    }

    public boolean hasConfigurations() {
        return !publishConfigurations.isEmpty();
    }

    public File getIvyDescriptor() {
        return ivyDescriptor;
    }

    public void setIvyDescriptor(Object ivyDescriptor) {
        if (ivyDescriptor != null) {
            if (ivyDescriptor instanceof File) {
                this.ivyDescriptor = (File) ivyDescriptor;
            } else if (ivyDescriptor instanceof CharSequence) {
                if (FileUtils.isAbsolutePath(ivyDescriptor.toString())) {
                    this.ivyDescriptor = new File(ivyDescriptor.toString());
                } else {
                    this.ivyDescriptor = new File(getProject().getProjectDir(), ivyDescriptor.toString());
                }
            } else {
                log.warn("Unknown type '{}' for ivy descriptor in task '{}'",
                        new Object[]{ivyDescriptor.getClass().getName(), getPath()});
            }
        } else {
            this.ivyDescriptor = null;
        }
    }

    public File getMavenDescriptor() {
        return mavenDescriptor;
    }

    public void setMavenDescriptor(Object mavenDescriptor) {
        if (mavenDescriptor != null) {
            if (mavenDescriptor instanceof File) {
                this.mavenDescriptor = (File) mavenDescriptor;
            } else if (mavenDescriptor instanceof CharSequence) {
                if (FileUtils.isAbsolutePath(mavenDescriptor.toString())) {
                    this.mavenDescriptor = new File(mavenDescriptor.toString());
                } else {
                    this.mavenDescriptor = new File(getProject().getProjectDir(), mavenDescriptor.toString());
                }
            } else {
                log.warn("Unknown type '{}' for maven descriptor in task '{}'",
                        new Object[]{mavenDescriptor.getClass().getName(), getPath()});
            }
        } else {
            this.mavenDescriptor = null;
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

        // If no configuration no descriptors
        if (!hasConfigurations()) {
            if (publishConfigsSpecified) {
                log.warn("None of the specified publish configurations matched for project '{}' - nothing to publish.",
                        project.getPath());
                return;
            } else {
                Configuration archiveConfig = project.getConfigurations().findByName(Dependency.ARCHIVES_CONFIGURATION);
                if (archiveConfig != null) {
                    log.info("No publish configurations specified for project '{}' - using the default '{}' " +
                            "configuration.", project.getPath(), Dependency.ARCHIVES_CONFIGURATION);
                    publishConfigurations.add(archiveConfig);
                } else {
                    log.warn("No publish configurations specified for project '{}' and the default '{}' " +
                            "configuration does not exist.", project.getPath(), Dependency.ARCHIVES_CONFIGURATION);
                    return;
                }
            }
        }
        // The task depends on the produced artifacts of all configurations "to publish"
        for (Configuration publishConfiguration : publishConfigurations) {
            dependsOn(publishConfiguration.getArtifacts());
        }

        // Set ivy descriptor parameters
        if (isPublishIvy(acc)) {
            if (ivyDescriptor == null) {
                setDefaultIvyDescriptor();
            }
        } else {
            ivyDescriptor = null;
        }

        // Set maven pom parameters
        if (isPublishMaven(acc)) {
            if (mavenDescriptor == null) {
                setDefaultMavenDescriptor();
            }
        } else {
            mavenDescriptor = null;
        }
    }

    private void setDefaultIvyDescriptor() {
        Project project = getProject();
        TaskContainer tasks = project.getTasks();
        TaskCollection<GenerateIvyDescriptor> generateIvyDescriptors = tasks.withType(GenerateIvyDescriptor.class);
        if (!generateIvyDescriptors.isEmpty()) {
            findIvyDescriptorFromGenertorTask(project, generateIvyDescriptors);
        } else {
            findIvyDescriptorFromUploadTask(project, tasks);
        }
    }

    private void findIvyDescriptorFromGenertorTask(Project project, TaskCollection<GenerateIvyDescriptor> generateIvyDescriptors) {
        if (generateIvyDescriptors.size() > 1) {
            log.warn("Project {} has multiple tasks of type '{}' cannot select one automatically!\n" +
                    "Please add artifactoryPublish.ivyDescriptor = generate«NAME OF PUBLICATION»IvyModuleDescriptor.destination\n" +
                    "in the project configuration code.", project.getPath(), GenerateIvyDescriptor.class);
        } else {
            GenerateIvyDescriptor generateIvyDescriptor = generateIvyDescriptors.iterator().next();
            ivyDescriptor = generateIvyDescriptor.getDestination();
            dependsOn(generateIvyDescriptor);
        }
    }

    private void findIvyDescriptorFromUploadTask(Project project, TaskContainer tasks) {
        Configuration archiveConfig = project.getConfigurations().findByName(Dependency.ARCHIVES_CONFIGURATION);
        if (archiveConfig == null) {
            log.warn("Cannot publish Ivy descriptor if ivyDescriptor not set in task '{}' " +
                    "and no '{}' configuration exists in project '{}'.", Dependency.ARCHIVES_CONFIGURATION,
                    project.getPath());
        } else {
            // Flag to publish the Ivy XML file, but no ivy descriptor file inputted, activate default upload${configuration}.
            // ATTENTION: Tasks not part of the execution graph have withType(Upload.class) false ?!? Need to check for type our self.
            Task candidateUploadTask = tasks.findByName(archiveConfig.getUploadTaskName());
            if (candidateUploadTask == null) {
                log.warn("Cannot publish Ivy descriptor if ivyDescriptor not set in task '{}' " +
                        "and task '{}' does not exist." +
                        "\nAdding \"apply plugin: 'java'\" or any other plugin extending the 'base' plugin" +
                        "will solve this issue.",
                        new Object[]{getPath(), archiveConfig.getUploadTaskName()});
            } else {
                if (!(candidateUploadTask instanceof Upload)) {
                    log.warn("Cannot publish Ivy descriptor if ivyDescriptor not set in task '{}' " +
                            "and task '{}' is not an Upload task." +
                            "\nYou'll need to set publishIvy=false or provide a path to the ivy file to " +
                            "publish to solve this issue.",
                            new Object[]{getPath(), archiveConfig.getUploadTaskName()});
                } else {
                    Upload uploadTask = (Upload) candidateUploadTask;
                    if (!uploadTask.isUploadDescriptor()) {
                        log.info("Forcing task '{}' to upload its Ivy descriptor (uploadDescriptor was false).",
                                uploadTask.getPath());
                        uploadTask.setUploadDescriptor(true);
                    }
                    ivyDescriptor = uploadTask.getDescriptorDestination();
                    dependsOn(candidateUploadTask);
                }
            }
        }
    }

    private void setDefaultMavenDescriptor() {
        // Flag to publish the Maven POM, but no pom file inputted, activate default Maven install.
        // if the project doesn't have the maven install task, warn
        Project project = getProject();
        TaskContainer tasks = project.getTasks();
        Upload installTask = tasks.withType(Upload.class).findByName("install");
        if (installTask == null) {
            log.warn("Cannot publish pom for project '{}' since it does not contain the Maven " +
                    "plugin install task and task '{}' does not specify a custom pom path.",
                    new Object[]{project.getPath(), getPath()});
            mavenDescriptor = null;
        } else {
            mavenDescriptor = new File(
                    project.getConvention().getPlugin(MavenPluginConvention.class).getMavenPomDir(),
                    "pom-default.xml");
            dependsOn(installTask);
        }
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

    @Nonnull
    private Boolean isPublishMaven(ArtifactoryClientConfiguration acc) {
        Boolean publishPom = getPublishPom();
        if (publishPom == null) {
            return acc.publisher.isMaven();
        }
        return publishPom;
    }

    @Nonnull
    private Boolean isPublishIvy(ArtifactoryClientConfiguration acc) {
        Boolean publishIvy = getPublishIvy();
        if (publishIvy == null) {
            return acc.publisher.isIvy();
        }
        return publishIvy;
    }

    private ArtifactoryClientConfiguration getArtifactoryClientConfiguration() {
        return ArtifactoryPluginUtil.getArtifactoryConvention(getProject()).getClientConfig();
    }

    @TaskAction
    public void collectProjectBuildInfo() throws IOException {
        try {
            log.debug("Task '{}' activated", getPath());
            // Only the last buildInfo execution activate the deployment
            List<BuildInfoTask> orderedTasks = getAllBuildInfoTasks();
            int myIndex = orderedTasks.indexOf(this);
            if (myIndex == -1) {
                log.error("Could not find my own task {} in the task graph!", getPath());
                return;
            }
            if (myIndex == orderedTasks.size() - 1) {
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
    private List<BuildInfoTask> getAllBuildInfoTasks() {
        List<BuildInfoTask> result = new ArrayList<BuildInfoTask>();
        for (Task task : getProject().getGradle().getTaskGraph().getAllTasks()) {
            if (task instanceof BuildInfoTask) {
                result.add((BuildInfoTask) task);
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

    private void collectDescriptorsAndArtifactsForUpload(Set<GradleDeployDetails> allDeployableDetails)
            throws IOException {
        Set<GradleDeployDetails> deployDetailsFromProject = getArtifactDeployDetailsFromClientConf();
        allDeployableDetails.addAll(deployDetailsFromProject);

        //Add the ivy and maven descriptors if they exist
        ArtifactoryClientConfiguration acc = getArtifactoryClientConfiguration();
        if (isPublishIvy(acc)) {
            if (ivyDescriptor != null && ivyDescriptor.exists()) {
                allDeployableDetails.add(getIvyDescriptorDeployDetails());
            }
        }
        if (isPublishMaven(acc)) {
            if (mavenDescriptor != null && mavenDescriptor.exists()) {
                allDeployableDetails.add(getMavenDeployDetails());
            }
        }
    }

    private GradleDeployDetails getIvyDescriptorDeployDetails() {
        ArtifactoryClientConfiguration clientConf = getArtifactoryClientConfiguration();
        DeployDetails.Builder artifactBuilder = new DeployDetails.Builder().file(ivyDescriptor);
        try {
            Map<String, String> checksums =
                    FileChecksumCalculator.calculateChecksums(ivyDescriptor, "MD5", "SHA1");
            artifactBuilder.md5(checksums.get("MD5")).sha1(checksums.get("SHA1"));
        } catch (Exception e) {
            throw new GradleException(
                    "Failed to calculate checksums for artifact: " + ivyDescriptor.getAbsolutePath(), e);
        }
        String gid = getProject().getGroup().toString();
        if (clientConf.publisher.isM2Compatible()) {
            gid = gid.replace(".", "/");
        }
        artifactBuilder.artifactPath(IvyPatternHelper
                .substitute(clientConf.publisher.getIvyPattern(), gid, getModuleName(),
                        getProject().getVersion().toString(), null, "ivy", "xml"));
        artifactBuilder.targetRepository(clientConf.publisher.getRepoKey());
        PublishArtifactInfo artifactInfo =
                new PublishArtifactInfo(ivyDescriptor.getName(), "xml", "ivy", null, ivyDescriptor);
        Map<String, String> propsToAdd = getPropsToAdd(artifactInfo, null);
        artifactBuilder.addProperties(propsToAdd);
        return new GradleDeployDetails(artifactInfo, artifactBuilder.build(), getProject());
    }

    private GradleDeployDetails getMavenDeployDetails() {
        ArtifactoryClientConfiguration clientConf = getArtifactoryClientConfiguration();
        DeployDetails.Builder artifactBuilder = new DeployDetails.Builder().file(mavenDescriptor);
        try {
            Map<String, String> checksums =
                    FileChecksumCalculator.calculateChecksums(mavenDescriptor, "MD5", "SHA1");
            artifactBuilder.md5(checksums.get("MD5")).sha1(checksums.get("SHA1"));
        } catch (Exception e) {
            throw new GradleException(
                    "Failed to calculate checksums for artifact: " + mavenDescriptor.getAbsolutePath(), e);
        }
        // for pom files always enforce the M2 pattern
        artifactBuilder.artifactPath(IvyPatternHelper.substitute(LayoutPatterns.M2_PATTERN,
                getProject().getGroup().toString().replace(".", "/"), getModuleName(),
                getProject().getVersion().toString(), null, "pom", "pom"));
        artifactBuilder.targetRepository(clientConf.publisher.getRepoKey());
        PublishArtifactInfo artifactInfo =
                new PublishArtifactInfo(mavenDescriptor.getName(), "pom", "pom", null, mavenDescriptor);
        Map<String, String> propsToAdd = getPropsToAdd(artifactInfo, null);
        artifactBuilder.addProperties(propsToAdd);
        return new GradleDeployDetails(artifactInfo, artifactBuilder.build(), getProject());
    }

    /**
     * This method will be activated only at the end of the build, when we reached the root project.
     *
     * @throws java.io.IOException In case the deployment fails.
     */
    private void prepareAndDeploy() throws IOException {
        ArtifactoryClientConfiguration acc = getArtifactoryClientConfiguration();
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
        ArtifactoryBuildInfoClient client =
                new ArtifactoryBuildInfoClient(contextUrl, username, password, new GradleClientLogger(log));
        Set<GradleDeployDetails> allDeployableDetails = Sets.newHashSet();

        // Update the artifacts for all project build info task
        List<BuildInfoTask> orderedTasks = getAllBuildInfoTasks();
        for (BuildInfoTask bit : orderedTasks) {
            if (bit.getDidWork()) {
                bit.collectDescriptorsAndArtifactsForUpload(allDeployableDetails);
            }
        }
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
                deployArtifacts(client, allDeployableDetails, patterns);
            }

            //Extract build info and update the clientConf info accordingly (build name, num, etc.)
            GradleBuildInfoExtractor gbie = new GradleBuildInfoExtractor(acc, allDeployableDetails);
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

    private String getModuleName() {
        Project project = getProject();
        //Take into account the archivesBaseName if applied to the project by the Java plugin
        if (project.hasProperty(ARCHIVES_BASE_NAME)) {
            return project.property(ARCHIVES_BASE_NAME).toString();
        }
        return project.getName();
    }

    private File getExportFile(ArtifactoryClientConfiguration clientConf) {
        String fileExportPath = clientConf.getExportFile();
        if (StringUtils.isNotBlank(fileExportPath)) {
            return new File(fileExportPath);
        }
        Project rootProject = getProject().getRootProject();
        return new File(rootProject.getBuildDir(), "build-info.json");
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

    private void deployArtifacts(ArtifactoryBuildInfoClient client, Set<GradleDeployDetails> details,
                                 IncludeExcludePatterns patterns) throws IOException {
        for (GradleDeployDetails detail : details) {
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

    private void exportBuildInfo(Build build, File toFile) throws IOException {
        log.debug("Exporting generated build info to '{}'", toFile.getAbsolutePath());
        BuildInfoExtractorUtils.saveBuildInfoToFile(build, toFile);
    }

    private Set<GradleDeployDetails> getArtifactDeployDetailsFromClientConf() {
        ArtifactoryClientConfiguration clientConf = getArtifactoryClientConfiguration();

        Set<GradleDeployDetails> deployDetails = Sets.newLinkedHashSet();
        if (!hasConfigurations()) {
            log.info("No configurations to publish for project '{}'.", getProject().getPath());
            return deployDetails;
        }
        ArtifactoryClientConfiguration.PublisherHandler publisherConf = clientConf.publisher;
        String pattern = publisherConf.getIvyArtifactPattern();
        String gid = getProject().getGroup().toString();
        if (publisherConf.isM2Compatible()) {
            gid = gid.replace(".", "/");
        }

        Set<String> processedFiles = Sets.newHashSet();
        for (Configuration configuration : publishConfigurations) {
            PublishArtifactSet artifacts = configuration.getAllArtifacts();
            for (PublishArtifact artifact : artifacts) {
                File file = artifact.getFile();
                if (processedFiles.contains(file.getAbsolutePath())) {
                    continue;
                }
                if (!file.exists()) {
                    log.warn("Skipping non-existent file '{}'.", file.getAbsolutePath());
                    continue;
                }
                processedFiles.add(file.getAbsolutePath());
                DeployDetails.Builder artifactBuilder = new DeployDetails.Builder().file(file);
                try {
                    Map<String, String> checksums =
                            FileChecksumCalculator.calculateChecksums(file, "MD5", "SHA1");
                    artifactBuilder.md5(checksums.get("MD5")).sha1(checksums.get("SHA1"));
                } catch (Exception e) {
                    throw new GradleException(
                            "Failed to calculate checksums for artifact: " + file.getAbsolutePath(), e);
                }
                String revision = getProject().getVersion().toString();
                Map<String, String> extraTokens = Maps.newHashMap();
                if (StringUtils.isNotBlank(artifact.getClassifier())) {
                    extraTokens.put("classifier", artifact.getClassifier());
                }
                artifactBuilder.artifactPath(IvyPatternHelper.substitute(pattern, gid, getModuleName(),
                        revision, artifact.getName(), artifact.getType(),
                        artifact.getExtension(), configuration.getName(),
                        extraTokens, null));
                artifactBuilder.targetRepository(publisherConf.getRepoKey());
                PublishArtifactInfo artifactInfo = new PublishArtifactInfo(artifact);
                Map<String, String> propsToAdd = getPropsToAdd(artifactInfo, configuration);
                artifactBuilder.addProperties(propsToAdd);
                DeployDetails details = artifactBuilder.build();
                deployDetails.add(new GradleDeployDetails(artifactInfo, details, getProject()));
            }
        }
        return deployDetails;
    }

    private Map<String, String> getPropsToAdd(PublishArtifactInfo artifact, Configuration configuration) {
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
                ArtifactSpec.builder().configuration(configuration != null ? configuration.getName() : null)
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
}