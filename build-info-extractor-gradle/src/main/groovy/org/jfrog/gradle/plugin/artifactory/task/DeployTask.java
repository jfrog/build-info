package org.jfrog.gradle.plugin.artifactory.task;

import org.apache.commons.lang.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.execution.TaskExecutionGraph;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.IncludeExcludePatterns;
import org.jfrog.build.extractor.clientConfiguration.PatternMatcher;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployableArtifactsUtils;
import org.jfrog.build.extractor.retention.Utils;
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPluginUtil;
import org.jfrog.gradle.plugin.artifactory.extractor.*;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author Ruben Perez
 */
public class DeployTask extends DefaultTask {

    private static final Logger log = Logging.getLogger(DeployTask.class);

    private List<ModuleInfoFileProducer> moduleInfoFileProducers = new ArrayList<>();

    @TaskAction
    public void taskAction() throws IOException {
        log.debug("Task '{}' activated", getPath());
        collectProjectBuildInfo();
    }

    private void collectProjectBuildInfo() throws IOException {
        log.debug("Starting build info extraction for project '{}' using last task in graph '{}'",
                new Object[]{getProject().getPath(), getPath()});
        prepareAndDeploy();
        String propertyFilePath = System.getenv(BuildInfoConfigProperties.PROP_PROPS_FILE);
        if (StringUtils.isBlank(propertyFilePath)) {
            propertyFilePath = System.getenv(BuildInfoConfigProperties.ENV_BUILDINFO_PROPFILE);
        }
        if (StringUtils.isNotBlank(propertyFilePath)) {
            File file = new File(propertyFilePath);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    /**
     * This method will be activated only at the "end" of the build, when we reached the root project.
     *
     * @throws java.io.IOException In case the deployment fails.
     */
    private void prepareAndDeploy() throws IOException {
        ArtifactoryClientConfiguration accRoot =
                ArtifactoryPluginUtil.getArtifactoryConvention(getProject()).getClientConfig();

        Map<String, String> propsRoot = accRoot.publisher.getProps();

        // Reset the default properties, they may have changed
        GradleArtifactoryClientConfigUpdater.setMissingBuildAttributes(
                accRoot, getProject().getRootProject());

        Map<String, Set<DeployDetails>> allDeployDetails = new ConcurrentHashMap<>();
        List<ArtifactoryTask> orderedTasks = findArtifactoryPublishTasks(getProject().getGradle().getTaskGraph());

        int publishForkCount = getPublishForkCount(accRoot);
        if (publishForkCount <= 1) {
            orderedTasks.forEach(t -> deployArtifacts(accRoot, propsRoot, allDeployDetails, t, null));
        } else {
            try {
                ExecutorService executor = Executors.newFixedThreadPool(publishForkCount);
                CompletableFuture<Void> allUploads = CompletableFuture.allOf(orderedTasks.stream()
                    .map(t -> CompletableFuture.runAsync(() -> deployArtifacts(accRoot, propsRoot, allDeployDetails, t, "[" + Thread.currentThread().getName() + "]"), executor))
                    .toArray(CompletableFuture[]::new));
                allUploads.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        ArtifactoryBuildInfoClient client = null;
        String contextUrl = accRoot.publisher.getContextUrl();
        if (contextUrl != null) {
            try {
                client = new ArtifactoryBuildInfoClient(
                        accRoot.publisher.getContextUrl(),
                        accRoot.publisher.getUsername(),
                        accRoot.publisher.getPassword(),
                        new GradleClientLogger(log));
                configureProxy(accRoot, client);
                configConnectionTimeout(accRoot, client);
                configRetriesParams(accRoot, client);
                GradleBuildInfoExtractor gbie = new GradleBuildInfoExtractor(accRoot, moduleInfoFileProducers);
                Build build = gbie.extract(getProject().getRootProject());
                exportBuildInfo(build, getExportFile(accRoot));
                if (isPublishBuildInfo(accRoot)) {
                    // If export property set always save the file before sending it to artifactory
                    exportBuildInfo(build, getExportFile(accRoot));
                    if (accRoot.info.isIncremental()) {
                        log.debug("Publishing build info modules to artifactory at: '{}'", contextUrl);
                        client.sendModuleInfo(build);
                    } else {
                        log.debug("Publishing build info to artifactory at: '{}'", contextUrl);
                        Utils.sendBuildAndBuildRetention(client, build, accRoot);
                    }
                }
                if (isGenerateBuildInfoToFile(accRoot)) {
                    try {
                        exportBuildInfo(build, new File(accRoot.info.getGeneratedBuildInfoFilePath()));
                    } catch (Exception e) {
                        log.error("Failed writing build info to file: ", e);
                        throw new IOException("Failed writing build info to file", e);
                    }
                }
                if (isGenerateDeployableArtifactsToFile(accRoot)) {
                    try {
                        exportDeployableArtifacts(allDeployDetails, new File(accRoot.info.getDeployableArtifactsFilePath()), accRoot.info.isBackwardCompatibleDeployableArtifacts());
                    } catch (Exception e) {
                        log.error("Failed writing deployable artifacts to file: ", e);
                        throw new RuntimeException("Failed writing deployable artifacts to file", e);
                    }
                }
            } finally {
                if (client != null) {
                    client.close();
                }
            }
        }
    }

    private void deployArtifacts(ArtifactoryClientConfiguration accRoot, Map<String, String> propsRoot, Map<String,
            Set<DeployDetails>> allDeployDetails, ArtifactoryTask artifactoryTask, String logPrefix) {
        try {
            if (artifactoryTask.getDidWork()) {
                ArtifactoryClientConfiguration.PublisherHandler publisher =
                    ArtifactoryPluginUtil.getPublisherHandler(artifactoryTask.getProject());

                if (publisher != null && publisher.getContextUrl() != null) {
                    Map<String, String> moduleProps = new HashMap<String, String>(propsRoot);
                    moduleProps.putAll(publisher.getProps());
                    publisher.getProps().putAll(moduleProps);
                    String contextUrl = publisher.getContextUrl();
                    String username = publisher.getUsername();
                    String password = publisher.getPassword();
                    if (StringUtils.isBlank(username)) {
                        username = "";
                    }
                    if (StringUtils.isBlank(password)) {
                        password = "";
                    }

                    if (publisher.isPublishArtifacts()) {
                        ArtifactoryBuildInfoClient client = null;
                        try {
                            client = new ArtifactoryBuildInfoClient(contextUrl, username, password,
                                new GradleClientLogger(log));

                            log.debug("Uploading artifacts to Artifactory at '{}'", contextUrl);
                            IncludeExcludePatterns patterns = new IncludeExcludePatterns(
                                publisher.getIncludePatterns(),
                                publisher.getExcludePatterns());
                            configureProxy(accRoot, client);
                            configConnectionTimeout(accRoot, client);
                            configRetriesParams(accRoot, client);
                            deployArtifacts(artifactoryTask.deployDetails, client, patterns, logPrefix);
                        } finally {
                            if (client != null) {
                                client.close();
                            }
                        }
                    }

                    if (!artifactoryTask.deployDetails.isEmpty()) {
                        Set<DeployDetails> deployDetailsSet = new LinkedHashSet<>();
                        for (GradleDeployDetails details : artifactoryTask.deployDetails) {
                            deployDetailsSet.add(details.getDeployDetails());
                        }
                        allDeployDetails.put(artifactoryTask.getProject().getName(), deployDetailsSet);
                    }
                }
            } else {
                log.debug("Task '{}' did no work", artifactoryTask.getPath());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    private void configConnectionTimeout(ArtifactoryClientConfiguration clientConf, ArtifactoryBuildInfoClient client) {
        if (clientConf.getTimeout() != null) {
            client.setConnectionTimeout(clientConf.getTimeout());
        }
    }

    private void configRetriesParams(ArtifactoryClientConfiguration clientConf, ArtifactoryBuildInfoClient client) {
        if (clientConf.getConnectionRetries() != null) {
            client.setConnectionRetries(clientConf.getConnectionRetries());
        }
    }

    private void exportBuildInfo(Build build, File toFile) throws IOException {
        log.debug("Exporting generated build info to '{}'", toFile.getAbsolutePath());
        BuildInfoExtractorUtils.saveBuildInfoToFile(build, toFile);
    }

    private void exportDeployableArtifacts(Map<String, Set<DeployDetails>> allDeployDetails, File toFile, boolean exportBackwardCompatibleDeployableArtifacts) throws IOException {
        log.debug("Exporting deployable artifacts to '{}'", toFile.getAbsolutePath());
        DeployableArtifactsUtils.saveDeployableArtifactsToFile(allDeployDetails, toFile, exportBackwardCompatibleDeployableArtifacts);
    }

    private File getExportFile(ArtifactoryClientConfiguration clientConf) {
        String fileExportPath = clientConf.getExportFile();
        if (StringUtils.isNotBlank(fileExportPath)) {
            return new File(fileExportPath);
        }
        Project rootProject = getProject().getRootProject();
        return new File(rootProject.getBuildDir(), "build-info.json");
    }

    @Nonnull
    private Boolean isPublishBuildInfo(ArtifactoryClientConfiguration acc) {
        return acc.publisher.isPublishBuildInfo();
    }

    private int getPublishForkCount(ArtifactoryClientConfiguration acc) {
        return acc.publisher.getPublishForkCount();
    }

    @Nonnull
    private Boolean isGenerateBuildInfoToFile(ArtifactoryClientConfiguration acc) {
        return !StringUtils.isEmpty(acc.info.getGeneratedBuildInfoFilePath());
    }

    @Nonnull
    private Boolean isGenerateDeployableArtifactsToFile(ArtifactoryClientConfiguration acc) {
        return !StringUtils.isEmpty(acc.info.getDeployableArtifactsFilePath());
    }

    private void deployArtifacts(Set<GradleDeployDetails> allDeployDetails, ArtifactoryBuildInfoClient client,
                                 IncludeExcludePatterns patterns, String logPrefix)
            throws IOException {
        for (GradleDeployDetails detail : allDeployDetails) {
            DeployDetails deployDetails = detail.getDeployDetails();
            String artifactPath = deployDetails.getArtifactPath();
            if (PatternMatcher.pathConflicts(artifactPath, patterns)) {
                log.log(LogLevel.LIFECYCLE, "Skipping the deployment of '" + artifactPath +
                        "' due to the defined include-exclude patterns.");
                continue;
            }
            client.deployArtifact(deployDetails, logPrefix);
        }
    }

    private List<ArtifactoryTask> findArtifactoryPublishTasks(TaskExecutionGraph graph) {
        List<ArtifactoryTask> tasks = new ArrayList<ArtifactoryTask>();
        for (Task task : graph.getAllTasks()) {
            if (task instanceof ArtifactoryTask) {
                tasks.add(((ArtifactoryTask)task));
            }
        }
        return tasks;
    }

    /**
     * Returns a file collection containing all of the module info files that this task aggregates.
     */
    @InputFiles
    public FileCollection getModuleInfoFiles() {
        ConfigurableFileCollection moduleInfoFiles = getProject().files();
        moduleInfoFileProducers.forEach(moduleInfoFileProducer -> {
            moduleInfoFiles.from(moduleInfoFileProducer.getModuleInfoFiles());
            moduleInfoFiles.builtBy(moduleInfoFileProducer.getModuleInfoFiles().getBuildDependencies());
        });
        return moduleInfoFiles;
    }

    /**
     * Registers a producer of module info files for this task to aggregate.
     *
     * @param moduleInfoFileProducer
     */
    public void registerModuleInfoProducer(ModuleInfoFileProducer moduleInfoFileProducer) {
        this.moduleInfoFileProducers.add(moduleInfoFileProducer);
    }
}
