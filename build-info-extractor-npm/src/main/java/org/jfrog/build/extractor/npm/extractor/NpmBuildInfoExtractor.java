package org.jfrog.build.extractor.npm.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.BuildInfoExtractor;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.npm.NpmDriver;
import org.jfrog.build.extractor.npm.types.NpmPackageInfo;
import org.jfrog.build.extractor.npm.types.NpmProject;
import org.jfrog.build.extractor.npm.types.NpmScope;
import org.jfrog.build.extractor.producerConsumer.ConsumerRunnableBase;
import org.jfrog.build.extractor.producerConsumer.ProducerConsumerExecutor;
import org.jfrog.build.extractor.producerConsumer.ProducerRunnableBase;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Yahav Itzhak
 */
public class NpmBuildInfoExtractor implements BuildInfoExtractor<NpmProject> {
    private static final String NPMRC_BACKUP_FILE_NAME = "jfrog.npmrc.backup";
    private static final String NPMRC_FILE_NAME = ".npmrc";

    private ArtifactoryDependenciesClientBuilder dependenciesClientBuilder;
    private NpmPackageInfo npmPackageInfo = new NpmPackageInfo();
    private NpmDriver npmDriver;
    private String npmRegistry;
    private Properties npmAuth;
    private Log logger;

    NpmBuildInfoExtractor(ArtifactoryDependenciesClientBuilder dependenciesClientBuilder, NpmDriver npmDriver, Log logger) {
        this.dependenciesClientBuilder = dependenciesClientBuilder;
        this.npmDriver = npmDriver;
        this.logger = logger;
    }

    @Override
    public Build extract(NpmProject npmProject) throws Exception {
        String resolutionRepository = npmProject.getResolutionRepository();
        List<String> installationArgs = npmProject.getInstallationArgs();
        Path workingDir = npmProject.getWorkingDir();

        preparePrerequisites(resolutionRepository, workingDir);
        createTempNpmrc(workingDir, installationArgs);
        runInstall(workingDir, installationArgs);
        restoreNpmrc(workingDir);

        List<Dependency> dependencies = collectDependencies(workingDir);
        return createBuild(dependencies);
    }

    private void preparePrerequisites(String resolutionRepository, Path workingDir) throws IOException {
        try (ArtifactoryDependenciesClient dependenciesClient = dependenciesClientBuilder.build()) {
            setNpmAuth(dependenciesClient);
            setRegistryUrl(dependenciesClient, resolutionRepository);
        }
        readPackageInfoFromPackageJson(workingDir);
        backupProjectNpmrc(workingDir);
    }

    private void setNpmAuth(ArtifactoryDependenciesClient dependenciesClient) throws IOException {
        npmAuth = dependenciesClient.getNpmAuth();
    }

    private void setRegistryUrl(ArtifactoryDependenciesClient dependenciesClient, String resolutionRepository) {
        npmRegistry = dependenciesClient.getArtifactoryUrl();
        if (!StringUtils.endsWith(npmRegistry, "/")) {
            npmRegistry += "/";
        }
        npmRegistry += "api/npm/" + resolutionRepository;
    }

    private void readPackageInfoFromPackageJson(Path workingDir) throws IOException {
        try (FileInputStream fis = new FileInputStream(workingDir.resolve("package.json").toFile())) {
            npmPackageInfo.readPackageInfo(fis);
        }
    }

    /**
     * To make npm do the resolution from Artifactory we are creating .npmrc file in the project dir.
     * If a .npmrc file already exists we will backup it and override while running the command.
     */
    private void backupProjectNpmrc(Path workingDir) throws IOException {
        Path npmrcPath = workingDir.resolve(NPMRC_FILE_NAME);
        if (!Files.exists(npmrcPath)) {
            return;
        }
        Path npmrcBackupPath = workingDir.resolve(NPMRC_BACKUP_FILE_NAME);
        Files.copy(npmrcPath, npmrcBackupPath, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
    }

    private void createTempNpmrc(Path workingDir, List<String> installationArgs) throws IOException, InterruptedException {
        Path npmrcPath = workingDir.resolve(NPMRC_FILE_NAME);
        Files.deleteIfExists(npmrcPath); // Delete old npmrc file
        final String configList = npmDriver.configList(workingDir.toFile(), installationArgs);

        Properties npmrcProperties = new Properties();

        // Save npm config list results
        ObjectMapper mapper = new ObjectMapper();
        JsonNode manifestTree = mapper.readTree(configList);
        manifestTree.fields().forEachRemaining(entry -> npmrcProperties.setProperty(entry.getKey(), entry.getValue().asText()));

        // Save npm auth
        npmrcProperties.putAll(npmAuth);

        // Save registry
        npmrcProperties.setProperty("registry", npmRegistry);
        npmrcProperties.remove("metrics-registry");

        // Write npmrc file
        StringBuilder stringBuffer = new StringBuilder();
        npmrcProperties.forEach((key, value) -> stringBuffer.append(key).append("=").append(value).append("\n"));
        setScope(npmrcProperties);
        try (FileWriter fileWriter = new FileWriter(npmrcPath.toFile());
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
            bufferedWriter.write(stringBuffer.toString());
            bufferedWriter.flush();
        }
    }

    /**
     * npm install scope can be set by "--production" or "--only={prod[uction]|dev[elopment]}" flags.
     *
     * @param npmrcProperties - The results of 'npm config list' command.
     */
    private void setScope(Properties npmrcProperties) {
        String only = npmrcProperties.getProperty("only");
        if (StringUtils.startsWith(only, "prod") || Boolean.parseBoolean(npmrcProperties.getProperty("production"))) {
            npmPackageInfo.setScope(NpmScope.PRODUCTION.toString());
        } else if (StringUtils.startsWith(only, "dev")) {
            npmPackageInfo.setScope(NpmScope.DEVELOPMENT.toString());
        }
    }

    private void runInstall(Path workingDir, List<String> installationArgs) throws IOException {
        logger.info(npmDriver.install(workingDir.toFile(), installationArgs));
    }

    private void restoreNpmrc(Path workingDir) throws IOException {
        Path npmrcPath = workingDir.resolve(NPMRC_FILE_NAME);
        Path npmrcBackupPath = workingDir.resolve(NPMRC_BACKUP_FILE_NAME);
        if (Files.exists(npmrcBackupPath)) { // npmrc file did exist before - Restore it.
            Files.move(npmrcBackupPath, npmrcPath, StandardCopyOption.REPLACE_EXISTING);
        } else { // npmrc file didn't exist before - Delete the temporary npmrc file.
            Files.deleteIfExists(npmrcPath);
        }
    }

    /**
     * For each scope ('development' or 'production'):
     * 1. Run 'npm ls' command.
     * 2. From 'npm ls' command results - Create a tree of 'NpmPackageInfo's.
     * 3. Collect the dependencies from the tree.
     *
     * @param workingDir - Project's directory
     * @return List of dependencies
     * @see NpmPackageInfo
     */
    private List<Dependency> collectDependencies(Path workingDir) throws Exception {
        Map<String, Dependency> dependencies = new ConcurrentHashMap<>();
        List<NpmScope> scopes = getNpmScopes();
        for (NpmScope scope : scopes) {
            List<String> extraListArgs = new ArrayList<>();
            extraListArgs.add("--only=" + scope);
            JsonNode jsonNode = npmDriver.list(workingDir.toFile(), extraListArgs);
            populateDependenciesMap(dependencies, scope, jsonNode);
        }

        return new ArrayList<>(dependencies.values());
    }

    /**
     * This method returns the npm scopes of this npm install command.
     * It does this by checking the scopes on the npm package - dev, prod or no scope ("no scope" actually means both) and builds the list of scopes to be returned.
     *
     * @return list of "production", "development" or both.
     */
    private List<NpmScope> getNpmScopes() {
        List<NpmScope> scopes = new ArrayList<>();
        if (StringUtils.isBlank(npmPackageInfo.getScope())) {
            scopes.add(NpmScope.DEVELOPMENT);
            scopes.add(NpmScope.PRODUCTION);
        } else {
            // If this npm package is not installed with the dev scope, then it is installed with the prod scope.
            if (!StringUtils.containsIgnoreCase(NpmScope.DEVELOPMENT.toString(), npmPackageInfo.getScope())) {
                scopes.add(NpmScope.PRODUCTION);
            }
            // If this npm package is not installed with the prod scope, then it is installed with the dev scope.
            if (!StringUtils.containsIgnoreCase(NpmScope.PRODUCTION.toString(), npmPackageInfo.getScope())) {
                scopes.add(NpmScope.DEVELOPMENT);
            }
        }
        return scopes;
    }

    private Build createBuild(List<Dependency> dependencies) {
        Module module = new ModuleBuilder().id(npmPackageInfo.toString()).dependencies(dependencies).build();
        List<Module> modules = new ArrayList<>();
        modules.add(module);
        Build build = new Build();
        build.setModules(modules);
        return build;
    }

    /**
     * Populate the dependencies map for the specified scope by:
     * 1. Create npm dependencies tree from root node of 'npm ls' command tree. Populate each node with name, version and scope.
     * 2. For each dependency, retrieve sha1 and md5 from Artifactory. Use the producer-consumer mechanism to parallelize it.
     */
    private void populateDependenciesMap(Map<String, Dependency> dependencies, NpmScope scope, JsonNode npmDependenciesTree) throws Exception {
        // Set of packages that could not be found in Artifactory
        Set<NpmPackageInfo> badPackages = Collections.synchronizedSet(new HashSet<>());
        DefaultMutableTreeNode rootNode = NpmDependencyTree.createDependenciesTree(scope, npmDependenciesTree);
        try (ArtifactoryDependenciesClient client1 = dependenciesClientBuilder.build();
             ArtifactoryDependenciesClient client2 = dependenciesClientBuilder.build();
             ArtifactoryDependenciesClient client3 = dependenciesClientBuilder.build()
        ) {
            // Create producer Runnable
            ProducerRunnableBase[] producerRunnable = new ProducerRunnableBase[]{new NpmExtractorProducer(rootNode)};
            // Create consumer Runnables
            ConsumerRunnableBase[] consumerRunnables = new ConsumerRunnableBase[]{
                    new NpmExtractorConsumer(client1, dependencies, badPackages),
                    new NpmExtractorConsumer(client2, dependencies, badPackages),
                    new NpmExtractorConsumer(client3, dependencies, badPackages)
            };
            // Create the deployment executor
            ProducerConsumerExecutor deploymentExecutor = new ProducerConsumerExecutor(logger, producerRunnable, consumerRunnables, 10);
            deploymentExecutor.start();
            if (!badPackages.isEmpty()) {
                logger.info((Arrays.toString(badPackages.toArray())));
                logger.info("The npm dependencies above could not be found in Artifactory and therefore are not included in the build-info. " +
                        "Make sure the dependencies are available in Artifactory for this build. " +
                        "Deleting the local cache will force populating Artifactory with these dependencies.");
            }
        }
    }
}
