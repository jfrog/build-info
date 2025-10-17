package org.jfrog.gradle.plugin.artifactory.task;

import org.apache.commons.lang3.StringUtils;
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
import org.jfrog.build.client.ArtifactoryUploadResponse;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.extractor.ci.BuildInfoConfigProperties;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.IncludeExcludePatterns;
import org.jfrog.build.extractor.clientConfiguration.PatternMatcher;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
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

import static org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration.addDefaultPublisherAttributes;

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
        String propertyFilePath = System.getProperty(BuildInfoConfigProperties.PROP_PROPS_FILE);
        if (StringUtils.isBlank(propertyFilePath)) {
            propertyFilePath = System.getProperty(BuildInfoConfigProperties.ENV_BUILDINFO_PROPFILE);
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
        addDefaultPublisherAttributes(
                accRoot, getProject().getRootProject().getName(), "Gradle", getProject().getGradle().getGradleVersion());

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

        // Extract Build Info.
        GradleBuildInfoExtractor gbie = new GradleBuildInfoExtractor(accRoot, moduleInfoFileProducers);
        BuildInfo buildInfo = gbie.extract(getProject().getRootProject());
        exportBuildInfo(buildInfo, getExportFile(accRoot));

        // Export generated.
        generateBuildInfoJson(accRoot, buildInfo);

        // Handle deployment.
        handleBuildInfoDeployment(accRoot, buildInfo, allDeployDetails);
    }

    private void generateBuildInfoJson(ArtifactoryClientConfiguration accRoot, BuildInfo buildInfo) throws IOException {
        if (isGenerateBuildInfoToFile(accRoot)) {
            try {
                exportBuildInfo(buildInfo, new File(accRoot.info.getGeneratedBuildInfoFilePath()));
            } catch (Exception e) {
                log.error("Failed writing build info to file: ", e);
                throw new IOException("Failed writing build info to file", e);
            }
        }
    }

    private void handleBuildInfoDeployment(ArtifactoryClientConfiguration accRoot, BuildInfo buildInfo, Map<String, Set<DeployDetails>> allDeployDetails) throws IOException {
        String contextUrl = accRoot.publisher.getContextUrl();
        if (contextUrl != null) {
            try (ArtifactoryManager artifactoryManager = new ArtifactoryManager(
                    accRoot.publisher.getContextUrl(),
                    accRoot.publisher.getUsername(),
                    accRoot.publisher.getPassword(),
                    new GradleClientLogger(log))) {

                configureProxy(accRoot, artifactoryManager);
                if (isPublishBuildInfo(accRoot)) {
                    // If export property set always save the file before sending it to artifactory
                    exportBuildInfo(buildInfo, getExportFile(accRoot));
                    if (accRoot.info.isIncremental()) {
                        log.debug("Publishing build info modules to artifactory at: '{}'", contextUrl);
                        artifactoryManager.sendModuleInfo(buildInfo);
                    } else {
                        log.debug("Publishing build info to artifactory at: '{}'", contextUrl);
                        Utils.sendBuildAndBuildRetention(artifactoryManager, buildInfo, accRoot);
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
                        try (ArtifactoryManager artifactoryManager = new ArtifactoryManager(contextUrl, username, password,
                                new GradleClientLogger(log))) {
                            log.debug("Uploading artifacts to Artifactory at '{}'", contextUrl);
                            IncludeExcludePatterns patterns = new IncludeExcludePatterns(
                                    publisher.getIncludePatterns(),
                                    publisher.getExcludePatterns());
                            configureProxy(accRoot, artifactoryManager);
                            configConnectionTimeout(accRoot, artifactoryManager);
                            configRetriesParams(accRoot, artifactoryManager);
                            configInsecureTls(accRoot, artifactoryManager);
                            deployArtifacts(artifactoryTask.deployDetails, artifactoryManager, patterns, logPrefix, publisher.getMinChecksumDeploySizeKb());
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

    private void configureProxy(ArtifactoryClientConfiguration clientConf, ArtifactoryManager artifactoryManager) {
        ArtifactoryClientConfiguration.ProxyHandler proxy = clientConf.proxy;
        String proxyHost = proxy.getHost();
        if (StringUtils.isNotBlank(proxyHost) && proxy.getPort() != null) {
            log.debug("Found proxy host '{}'", proxyHost);
            String proxyUserName = proxy.getUsername();
            if (StringUtils.isNotBlank(proxyUserName)) {
                log.debug("Found proxy user name '{}'", proxyUserName);
                artifactoryManager.setProxyConfiguration(proxyHost, proxy.getPort(), proxyUserName, proxy.getPassword());
            } else {
                log.debug("No proxy user name and password found, using anonymous proxy");
                artifactoryManager.setProxyConfiguration(proxyHost, proxy.getPort());
            }
        }
    }

    private void configConnectionTimeout(ArtifactoryClientConfiguration clientConf, ArtifactoryManager artifactoryManager) {
        if (clientConf.getTimeout() != null) {
            artifactoryManager.setConnectionTimeout(clientConf.getTimeout());
        }
    }

    private void configRetriesParams(ArtifactoryClientConfiguration clientConf, ArtifactoryManager artifactoryManager) {
        if (clientConf.getConnectionRetries() != null) {
            artifactoryManager.setConnectionRetries(clientConf.getConnectionRetries());
        }
    }

    private void configInsecureTls(ArtifactoryClientConfiguration clientConf, ArtifactoryManager artifactoryManager) {
        log.debug("Deploying artifacts using InsecureTls = " + clientConf.getInsecureTls());
        artifactoryManager.setInsecureTls(clientConf.getInsecureTls());
    }

    private void exportBuildInfo(BuildInfo buildInfo, File toFile) throws IOException {
        log.debug("Exporting generated build info to '{}'", toFile.getAbsolutePath());
        BuildInfoExtractorUtils.saveBuildInfoToFile(buildInfo, toFile);
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

    private void deployArtifacts(Set<GradleDeployDetails> allDeployDetails, ArtifactoryManager artifactoryManager,
                                 IncludeExcludePatterns patterns, String logPrefix, int minChecksumDeploySizeKb)
            throws IOException {
        for (GradleDeployDetails detail : allDeployDetails) {
            DeployDetails deployDetails = detail.getDeployDetails();
            String artifactPath = deployDetails.getArtifactPath();
            if (PatternMatcher.pathConflicts(artifactPath, patterns)) {
                log.log(LogLevel.LIFECYCLE, "Skipping the deployment of '" + artifactPath +
                        "' due to the defined include-exclude patterns.");
                continue;
            }
            try {
                ArtifactoryUploadResponse response = artifactoryManager.upload(deployDetails, logPrefix, minChecksumDeploySizeKb);
                detail.getDeployDetails().setDeploySucceeded(true);
                detail.getDeployDetails().setSha256(response.getChecksums().getSha256());
            } catch (IOException e) {
                detail.getDeployDetails().setDeploySucceeded(false);
                detail.getDeployDetails().setSha256("");
                throw e;
            }
        }
    }

    private List<ArtifactoryTask> findArtifactoryPublishTasks(TaskExecutionGraph graph) {
        List<ArtifactoryTask> tasks = new ArrayList<ArtifactoryTask>();
        for (Task task : graph.getAllTasks()) {
            if (task instanceof ArtifactoryTask) {
                tasks.add(((ArtifactoryTask) task));
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
