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
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.internal.artifacts.publish.DefaultPublishArtifact;
import org.gradle.api.internal.resource.ResourceException;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.*;
import org.gradle.util.ConfigureUtil;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.client.*;
import org.jfrog.build.extractor.BuildInfoExtractorSpec;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Tomer Cohen
 */
public class BuildInfoRecorderTask extends DefaultTask {
    private static final Logger log = Logging.getLogger(BuildInfoRecorderTask.class);
    public static final String PUBLISH_IVY = "publishIvy";
    public static final String PUBLISH_POM = "publishPom";
    public static final String PUBLISH_ARTIFACTS = "publishArtifacts";
    public static final String PUBLISH_BUILD_INFO = "publishBuildInfo";

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

    private final Map<String, Boolean> flags = Maps.newHashMap();

    private boolean lastInGraph = false;

    private Map<String, String> propsToAdd;

    @Input
    @Optional
    public Boolean getPublishBuildInfo() {
        return getFlag(PUBLISH_BUILD_INFO);
    }

    public void setPublishBuildInfo(Boolean publishBuildInfo) {
        setFlag(PUBLISH_BUILD_INFO, publishBuildInfo);
    }

    @Input
    @Optional
    public Boolean getPublishArtifacts() {
        return getFlag(PUBLISH_ARTIFACTS);
    }

    public void setPublishArtifacts(Boolean publishArtifacts) {
        setFlag(PUBLISH_ARTIFACTS, publishArtifacts);
    }

    @Input
    @Optional
    public Boolean getPublishIvy() {
        return getFlag(PUBLISH_IVY);
    }

    public void setPublishIvy(Boolean publishIvy) {
        setFlag(PUBLISH_IVY, publishIvy);
    }

    @Input
    @Optional
    public Boolean getPublishPom() {
        return getFlag(PUBLISH_POM);
    }

    public void setPublishPom(Boolean publishPom) {
        setFlag(PUBLISH_POM, publishPom);
    }

    private Boolean getFlag(String flagName) {
        return flags.get(flagName);
    }

    private void setFlag(String flagName, Boolean newValue) {
        flags.put(flagName, newValue);
    }

    public void setLastInGraph(boolean lastInGraph) {
        this.lastInGraph = lastInGraph;
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
                    this.properties.put(key.toString(), value);
                }
            }
        }
    }

    public void publishConfigs(Object... confs) {
        if (confs == null) {
            return;
        }
        for (Object conf : confs) {
            if (conf instanceof CharSequence) {
                Configuration projectConf = getProject().getConfigurations().findByName(conf.toString());
                if (projectConf != null) {
                    publishConfigurations.add(projectConf);
                } else {
                    log.info("Configuration named '{}' does not exists for project '{}' in task '{}'",
                            new Object[]{conf, getProject().getPath(), getPath()});
                }
            } else if (conf instanceof Configuration) {
                publishConfigurations.add((Configuration) conf);
            } else {
                log.info("Configuration type '{}' not supported in task '{}'",
                        new Object[]{conf.getClass().getName(), getPath()});
            }
        }
    }

    public Set<Configuration> getPublishConfigurations() {
        return publishConfigurations;
    }

    public Configuration getConfiguration() {
        if (!hasConfigurations()) {
            return null;
        }
        return publishConfigurations.iterator().next();
    }

    public boolean hasConfigurations() {
        return publishConfigurations != null && !publishConfigurations.isEmpty();
    }

    public void setConfiguration(Configuration configuration) {
        if (configuration == null) {
            return;
        }
        if (publishConfigurations == null) {
            publishConfigurations = Sets.newHashSet();
        }
        publishConfigurations.add(configuration);
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
        ArtifactoryPluginConvention convention = GradlePluginUtils.getArtifactoryConvention(project);
        List<Closure> configurationClosures = convention.getTaskDefaultClosures();
        for (Closure closure : configurationClosures) {
            ConfigureUtil.configure(closure, this);
        }
        if (!hasConfigurations()) {
            // If no configurations set and the archives conf exists, adding it by default
            Configuration archiveConf = project.getConfigurations().findByName(Dependency.ARCHIVES_CONFIGURATION);
            setConfiguration(archiveConf);
        }
        for (Project sub : project.getSubprojects()) {
            Task subBiTask = sub.getTasks().findByName(GradlePluginUtils.BUILD_INFO_TASK_NAME);
            if (subBiTask != null) {
                dependsOn(subBiTask);
            }
        }

        // If no configuration no descriptor
        if (!hasConfigurations()) {
            return;
        }
        ArtifactoryClientConfiguration acc = convention.getConfiguration();

        // Set ivy descriptor parameters
        TaskContainer tasks = project.getTasks();
        if (this.isPublishIvy(acc.publisher.isIvy())) {
            Configuration archiveConf = project.getConfigurations().findByName(Dependency.ARCHIVES_CONFIGURATION);
            if (ivyDescriptor == null && archiveConf != null) {
                // Flag to publish the Ivy XML file, but no ivy descriptor file inputted, activate default upload${configuration}.
                // ATTENTION: Task not part of the execution graph have withType(Upload.class) false ?!? Need to check for type our self.
                Task mayBeUploadTask = tasks.findByName(archiveConf.getUploadTaskName());
                if (mayBeUploadTask == null) {
                    log.warn("Cannot publish Ivy descriptor if ivyDescriptor not set in task '{}' " +
                            "\nAnd task '{}' does not exists." +
                            "\nAdding \"apply plugin: 'java'\" or any other plugin extending the 'base' plugin will solve this issue.",
                            new Object[]{getPath(), archiveConf.getUploadTaskName()});
                    ivyDescriptor = null;
                } else {
                    if (!(mayBeUploadTask instanceof Upload)) {
                        log.warn("Cannot publish Ivy descriptor if ivyDescriptor not set in task '{}' " +
                                "\nAnd task '{}' is not an Upload task." +
                                "\nYou'll need to set publishIvy=false or provide a path to the ivy file to publish to solve this issue.",
                                new Object[]{getPath(), archiveConf.getUploadTaskName()});
                        ivyDescriptor = null;
                    } else {
                        Upload uploadTask = (Upload) mayBeUploadTask;
                        if (!uploadTask.isUploadDescriptor()) {
                            log.info("Task '{}' does not upload its Ivy descriptor. Forcing true to export it.", uploadTask.getPath());
                            uploadTask.setUploadDescriptor(true);
                        }
                        ivyDescriptor = uploadTask.getDescriptorDestination();
                        dependsOn(mayBeUploadTask);
                    }
                }
            }
        } else {
            ivyDescriptor = null;
        }

        // Set maven pom parameters
        if (isPublishMaven(acc)) {
            if (mavenDescriptor == null) {
                // Flag to publish the Maven POM, but no pom file inputted, activate default Maven install.
                // if the project doesn't have the maven install task, throw an exception
                Upload installTask = tasks.withType(Upload.class).findByName("install");
                if (installTask == null) {
                    log.warn("Cannot publish Maven descriptor if mavenDescriptor not set in task '{}' " +
                            "\nAnd default install task for project '{}' is not an Upload task",
                            new Object[]{getPath(), project.getPath()});
                    mavenDescriptor = null;
                } else {
                    mavenDescriptor = new File(project.getRepositories().getMavenPomDir(), "pom-default.xml");
                    dependsOn(installTask);
                }
            }
        } else {
            mavenDescriptor = null;
        }
    }

    private Boolean isPublishMaven(ArtifactoryClientConfiguration acc) {
        if (getPublishPom() == null)
            return acc.publisher.isMaven();
        return getPublishPom();
    }

    private boolean isPublishIvy(Boolean defaultValue) {
        if (getPublishIvy() == null)
            return defaultValue;
        return getPublishIvy();
    }

    private ArtifactoryClientConfiguration getArtifactoryClientConfiguration(Project project) {
        return GradlePluginUtils.getArtifactoryConvention(project).getConfiguration();
    }

    @TaskAction
    public void collectProjectBuildInfo() throws IOException {
        log.debug("Task '{}' activated", getPath());
        // Only the last buildInfo execution activate the deployment
        if (lastInGraph) {
            log.debug("Starting build info extraction for project '{}' using last task in graph '{}'",
                    new Object[]{getProject().getPath(), getPath()});
            closeAndDeploy();
        }
    }

    private void uploadDescriptorsAndArtifacts(Set<GradleDeployDetails> allDeployableDetails) throws IOException {
        ArtifactoryClientConfiguration clientConf = getArtifactoryClientConfiguration(getProject());
        Map<String, String> params = clientConf.publisher.getMatrixParams();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (StringUtils.isNotBlank(entry.getKey())) {
                properties.put(entry.getKey(), entry.getValue());
            }
        }
        Set<GradleDeployDetails> deployDetailsFromProject = getDeployArtifactsProject();
        allDeployableDetails.addAll(deployDetailsFromProject);
        if (clientConf.publisher.isPublishArtifacts()) {
            if (ivyDescriptor != null && ivyDescriptor.exists()) {
                allDeployableDetails.add(getIvyDescriptorDeployDetails());
            }

            if (mavenDescriptor != null && mavenDescriptor.exists()) {
                allDeployableDetails.add(getMavenDeployDetails());
            }
        }
    }

    private GradleDeployDetails getIvyDescriptorDeployDetails() {
        ArtifactoryClientConfiguration clientConf = getArtifactoryClientConfiguration(getProject());
        DeployDetails.Builder artifactBuilder = new DeployDetails.Builder().file(ivyDescriptor);
        try {
            Map<String, String> checksums =
                    FileChecksumCalculator.calculateChecksums(ivyDescriptor, "MD5", "SHA1");
            artifactBuilder.md5(checksums.get("MD5")).sha1(checksums.get("SHA1"));
        } catch (Exception e) {
            throw new ResourceException(
                    "Failed to calculated checksums for artifact: " + ivyDescriptor.getAbsolutePath(), e);
        }
        String gid = getProject().getGroup().toString();
        if (clientConf.publisher.isM2Compatible()) {
            gid = gid.replace(".", "/");
        }
        artifactBuilder.artifactPath(IvyPatternHelper
                .substitute(clientConf.publisher.getIvyPattern(), gid, getProject().getName(),
                        getProject().getVersion().toString(), null, "ivy", "xml"));
        artifactBuilder.targetRepository(clientConf.publisher.getRepoKey());
        Map<String, String> propsToAdd = getPropsToAdd();
        artifactBuilder.addProperties(propsToAdd);
        DefaultPublishArtifact artifact =
                new DefaultPublishArtifact(ivyDescriptor.getName(), "xml", "ivy", null, null, ivyDescriptor);
        return new GradleDeployDetails(artifact, artifactBuilder.build(), getProject());
    }

    private GradleDeployDetails getMavenDeployDetails() {
        ArtifactoryClientConfiguration clientConf = getArtifactoryClientConfiguration(getProject());
        DeployDetails.Builder artifactBuilder = new DeployDetails.Builder().file(mavenDescriptor);
        try {
            Map<String, String> checksums =
                    FileChecksumCalculator.calculateChecksums(mavenDescriptor, "MD5", "SHA1");
            artifactBuilder.md5(checksums.get("MD5")).sha1(checksums.get("SHA1"));
        } catch (Exception e) {
            throw new ResourceException(
                    "Failed to calculated checksums for artifact: " + mavenDescriptor.getAbsolutePath(), e);
        }
        // for pom files always enforce the M2 pattern
        artifactBuilder.artifactPath(IvyPatternHelper.substitute(LayoutPatterns.M2_PATTERN,
                getProject().getGroup().toString().replace(".", "/"), getProject().getName(),
                getProject().getVersion().toString(), null, "pom", "pom"));
        artifactBuilder.targetRepository(clientConf.publisher.getRepoKey());
        Map<String, String> propsToAdd = getPropsToAdd();
        artifactBuilder.addProperties(propsToAdd);
        DefaultPublishArtifact artifact =
                new DefaultPublishArtifact(mavenDescriptor.getName(), "pom", "pom", null, null, mavenDescriptor);
        return new GradleDeployDetails(artifact, artifactBuilder.build(), getProject());
    }

    /**
     * This method will be activated only at the end of the build, when we reached the root project.
     *
     * @throws java.io.IOException In case the deployment fails.
     */
    private void closeAndDeploy() throws IOException {
        ArtifactoryClientConfiguration clientConf = getArtifactoryClientConfiguration(getProject());
        String contextUrl = clientConf.getContextUrl();
        log.debug("Context URL for deployment '{}", contextUrl);
        String username = clientConf.publisher.getUsername();
        String password = clientConf.publisher.getPassword();
        if (StringUtils.isBlank(username)) {
            username = "";
        }
        if (StringUtils.isBlank(password)) {
            password = "";
        }
        ArtifactoryBuildInfoClient client =
                new ArtifactoryBuildInfoClient(contextUrl, username, password, new GradleClientLogger(log));
        Set<GradleDeployDetails> allDeployableDetails = Sets.newHashSet();
        for (Task birt : getProject().getTasksByName(GradlePluginUtils.BUILD_INFO_TASK_NAME, true)) {
            ((BuildInfoRecorderTask) birt).uploadDescriptorsAndArtifacts(allDeployableDetails);
        }
        try {
            if (clientConf.publisher.isPublishArtifacts()) {
                log.debug("Uploading artifacts to Artifactory at '{}'", contextUrl);
                /**
                 * if the {@link org.jfrog.build.client.ClientProperties#PROP_PUBLISH_ARTIFACT} is set the true,
                 * The uploadArchives task will be triggered ONLY at the end, ensuring that the artifacts will be published
                 * only after a successful build. This is done before the build-info is sent.
                 */

                IncludeExcludePatterns patterns = new IncludeExcludePatterns(
                        clientConf.publisher.getIncludePatterns(),
                        clientConf.publisher.getExcludePatterns());
                configureProxy(clientConf, client);
                deployArtifacts(client, allDeployableDetails, patterns);
            }
            GradleBuildInfoExtractor gbie = new GradleBuildInfoExtractor(clientConf, allDeployableDetails);
            Build build = gbie.extract(getProject().getRootProject(), new BuildInfoExtractorSpec());
            /**
             * The build-info will be always written to a file in its JSON form.
             */
            exportBuildInfo(build, getExportFile(clientConf));
            if (clientConf.publisher.isPublishBuildInfo()) {
                log.debug("Publishing build info to artifactory at: '{}'", contextUrl);
                /**
                 * After all the artifacts were uploaded successfully the next task is to send the build-info
                 * object.
                 */
                // If export property set always save the file before sending it to artifactory
                exportBuildInfo(build, getExportFile(clientConf));
                client.sendBuildInfo(build);
            }
        } finally {
            client.shutdown();
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

    private Set<GradleDeployDetails> getDeployArtifactsProject() {
        ArtifactoryClientConfiguration clientConf = getArtifactoryClientConfiguration(getProject());
        Set<GradleDeployDetails> deployDetails = Sets.newLinkedHashSet();
        if (!clientConf.publisher.isPublishArtifacts()) {
            return deployDetails;
        }
        if (!hasConfigurations()) {
            return deployDetails;
        }
        ArtifactoryClientConfiguration.PublisherHandler publisherConf = clientConf.publisher;
        String pattern = publisherConf.getIvyArtifactPattern();
        String gid = getProject().getGroup().toString();
        if (publisherConf.isM2Compatible()) {
            gid = gid.replace(".", "/");
        }
        for (Configuration configuration : publishConfigurations) {
            Set<PublishArtifact> artifacts = configuration.getArtifacts();
            for (PublishArtifact publishArtifact : artifacts) {
                File file = publishArtifact.getFile();
                DeployDetails.Builder artifactBuilder = new DeployDetails.Builder().file(file);
                try {
                    Map<String, String> checksums =
                            FileChecksumCalculator.calculateChecksums(file, "MD5", "SHA1");
                    artifactBuilder.md5(checksums.get("MD5")).sha1(checksums.get("SHA1"));
                } catch (Exception e) {
                    throw new ResourceException(
                            "Failed to calculated checksums for artifact: " + file.getAbsolutePath(), e);
                }
                String revision = getProject().getVersion().toString();
                Map<String, String> extraTokens = Maps.newHashMap();
                if (StringUtils.isNotBlank(publishArtifact.getClassifier())) {
                    extraTokens.put("classifier", publishArtifact.getClassifier());
                }
                artifactBuilder.artifactPath(IvyPatternHelper.substitute(pattern, gid, getProject().getName(),
                        revision, publishArtifact.getName(), publishArtifact.getType(),
                        publishArtifact.getExtension(), configuration.getName(),
                        extraTokens, null));
                artifactBuilder.targetRepository(publisherConf.getRepoKey());
                Map<String, String> propsToAdd = getPropsToAdd();
                artifactBuilder.addProperties(propsToAdd);
                DeployDetails details = artifactBuilder.build();
                deployDetails.add(new GradleDeployDetails(publishArtifact, details, getProject()));
            }
        }
        return deployDetails;
    }

    private Map<String, String> getPropsToAdd() {
        if (this.propsToAdd == null) {
            this.propsToAdd = Maps.newHashMap();
            for (Map.Entry<String, CharSequence> entry : properties.entries()) {
                // Make sure all GString are now Java Strings
                String key = entry.getKey().toString();
                String value = entry.getValue().toString();
                if (!this.propsToAdd.containsKey(key)) {
                    this.propsToAdd.put(key, value);
                } else {
                    value = this.propsToAdd.get(key) + ", " + value;
                    this.propsToAdd.put(key, value);
                }
            }
            ArtifactoryClientConfiguration configuration = getArtifactoryClientConfiguration(getProject());
            propsToAdd.putAll(configuration.publisher.getMatrixParams());
        }
        return propsToAdd;
    }
}