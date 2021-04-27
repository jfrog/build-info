package org.jfrog.build.extractor.npm.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.builder.ModuleType;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.BuildInfoExtractor;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
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

import static org.jfrog.build.client.PreemptiveHttpClientBuilder.CONNECTION_POOL_SIZE;

/**
 * @author Yahav Itzhak
 */
public class NpmBuildInfoExtractor implements BuildInfoExtractor<NpmProject> {
    private static final String NPMRC_BACKUP_FILE_NAME = "jfrog.npmrc.backup";
    private static final String NPMRC_FILE_NAME = ".npmrc";

    private ArtifactoryDependenciesClientBuilder dependenciesClientBuilder;
    private ArtifactoryBuildInfoClientBuilder buildInfoClientBuilder;
    private NpmPackageInfo npmPackageInfo = new NpmPackageInfo();
    private NpmDriver npmDriver;
    private String npmRegistry;
    private Properties npmAuth;
    private String buildName;
    private String npmProxy;
    private String module;
    private TypeRestriction typeRestriction;
    private Log logger;

    NpmBuildInfoExtractor(ArtifactoryDependenciesClientBuilder dependenciesClientBuilder, ArtifactoryBuildInfoClientBuilder buildInfoClientBuilder,
                          NpmDriver npmDriver, Log logger, String module, String buildName) {
        this.dependenciesClientBuilder = dependenciesClientBuilder;
        this.buildInfoClientBuilder = buildInfoClientBuilder;
        this.npmDriver = npmDriver;
        this.logger = logger;
        this.module = module;
        this.buildName = buildName;
        this.typeRestriction = TypeRestriction.DEFAULT_RESTRICTION;
    }

    @Override
    public Build extract(NpmProject npmProject) throws Exception {
        String resolutionRepository = npmProject.getResolutionRepository();
        List<String> commandArgs = npmProject.getCommandArgs();
        Path workingDir = npmProject.getWorkingDir();

        preparePrerequisites(resolutionRepository, workingDir);
        createTempNpmrc(workingDir, commandArgs);
        try {
            if (npmProject.isCiCommand()) {
                runCi(workingDir, commandArgs);
            } else {
                runInstall(workingDir, commandArgs);
            }
        } finally {
            restoreNpmrc(workingDir);
        }
        List<Dependency> dependencies = collectDependencies(workingDir);
        String moduleId = StringUtils.isNotBlank(module) ? module : npmPackageInfo.toString();
        return createBuild(dependencies, moduleId);
    }

    private void preparePrerequisites(String resolutionRepository, Path workingDir) throws IOException {
        try (ArtifactoryDependenciesClient dependenciesClient = dependenciesClientBuilder.build()) {
            setNpmAuth(dependenciesClient);
            setRegistryUrl(dependenciesClient, resolutionRepository);
            setNpmProxy(dependenciesClient);
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

    private void setNpmProxy(ArtifactoryDependenciesClient dependenciesClient) {
        ProxyConfiguration proxyConfiguration = dependenciesClient.getProxyConfiguration();
        if (proxyConfiguration == null || StringUtils.isBlank(proxyConfiguration.host)) {
            return;
        }
        npmProxy = "http://";
        String username = proxyConfiguration.username;
        String password = proxyConfiguration.password;
        if (StringUtils.isNoneBlank(username) && StringUtils.isNotBlank(password)) {
            npmProxy += username + ":" + password + "@";
        }
        npmProxy += proxyConfiguration.host + ":" + proxyConfiguration.port;
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

    private void createTempNpmrc(Path workingDir, List<String> commandArgs) throws IOException, InterruptedException {
        final String configList = npmDriver.configList(workingDir.toFile(), commandArgs, logger);
        Path npmrcPath = workingDir.resolve(NPMRC_FILE_NAME);
        Files.deleteIfExists(npmrcPath); // Delete old npmrc file

        StringBuilder npmrcBuilder = new StringBuilder();
        Scanner configScanner = new Scanner(configList);

        while (configScanner.hasNextLine()) {
            String currOption = configScanner.nextLine();

            if (!currOption.equals("")) {
                String[] splitOption = currOption.split("=", 2);
                String key = splitOption[0].trim();

                if (splitOption.length == 2 && isValidKey(key)) {
                    String value = splitOption[1].trim();

                    if (value.startsWith("[") && value.endsWith("]")) {
                        addArrayConfigs(npmrcBuilder, key, value);
                    } else {
                        npmrcBuilder.append(currOption).append("\n");
                    }

                    setTypeRestriction(key, value);
                } else if (key.startsWith("@")) {
                    // Override scoped registries (@scope = xyz)
                    npmrcBuilder.append(key).append(" = ").append(this.npmRegistry).append("\n");
                }
            }
        }

        // Since we run the get config cmd with "--json" flag, we don't want to force the json output on the new npmrc we write.
        // We will get json output only if it was explicitly required in the command arguments.
        npmrcBuilder.append("json = ").append(String.valueOf(isJsonOutputRequired(commandArgs))).append("\n");

        // Save registry
        npmrcBuilder.append("registry = ").append(this.npmRegistry).append("\n");

        // Save npm proxy
        if (StringUtils.isNotBlank(this.npmProxy)) {
            npmrcBuilder.append("proxy = ").append(this.npmProxy).append("\n");
        }

        // Save npm auth
        npmAuth.forEach((key, value) -> npmrcBuilder.append(key).append("=").append(value).append("\n"));

        // Write npmrc file
        try (FileWriter fileWriter = new FileWriter(npmrcPath.toFile());
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
            bufferedWriter.write(npmrcBuilder.toString());
            bufferedWriter.flush();
        }
    }

    private static void addArrayConfigs(StringBuilder npmrcBuilder, String key, String arrayValue) {
        if (arrayValue.equals("[]")) {
            return;
        }

        String valuesString = arrayValue.substring(1, arrayValue.length() - 1);
        String[] separatedValues = valuesString.split(",");

        for (String val : separatedValues) {
            npmrcBuilder.append(key).append("[] = ").append(val).append("\n");
        }
    }

    /**
     * To avoid writing configurations that are used by us
     */
    private static boolean isValidKey(String key) {
        return !key.startsWith("//") &&
                !key.startsWith(";") && // Comments
                !key.startsWith("@") && // Scoped configurations
                !key.equals("registry") &&
                !key.equals("metrics-registry") &&
                !key.equals("json"); // Handled separately because 'npm c ls' should run with json=false
    }

    /**
     * Boolean argument can be provided in one of the following ways:
     * 1. --arg - which infers true
     * 2. --arg=value (true/false)
     * 3. --arg value (true/false)
     */
    static boolean isJsonOutputRequired(List<String> commandArgs) {
        int jsonIndex = commandArgs.indexOf("--json");
        if (jsonIndex > -1) {
            return jsonIndex == commandArgs.size() - 1 || !commandArgs.get(jsonIndex + 1).equals("false");
        }
        return commandArgs.contains("--json=true");
    }

    private void setTypeRestriction(String key, String value) {
        // From npm 7, type restriction is determined by 'omit' and 'include' (both appear in 'npm config ls').
        // Other options (like 'dev', 'production' and 'only') are deprecated, but if they're used anyway - 'omit' and 'include' are automatically calculated.
        // So 'omit' is always preferred, if it exists.
        if (key.equals("omit")) {
            if (StringUtils.startsWith(value, "dev")) {
                this.typeRestriction = TypeRestriction.PROD_ONLY;
            } else {
                this.typeRestriction = TypeRestriction.ALL;
            }
        } else if (this.typeRestriction == TypeRestriction.DEFAULT_RESTRICTION) { // Until npm 6, configurations in 'npm config ls' are sorted by priority in descending order, so typeRestriction should be set only if it was not set before
            if (key.equals("only")) {
                if (StringUtils.startsWith(value, "prod")) {
                    this.typeRestriction = TypeRestriction.PROD_ONLY;
                } else if (StringUtils.startsWith(value, "dev")) {
                    this.typeRestriction = TypeRestriction.DEV_ONLY;
                }
            } else if (key.equals("production") && value.equals("true")) {
                this.typeRestriction = TypeRestriction.PROD_ONLY;
            }
        }
    }

    private void runInstall(Path workingDir, List<String> installationArgs) throws IOException {
        logger.info(npmDriver.install(workingDir.toFile(), installationArgs, logger));
    }

    private void runCi(Path workingDir, List<String> installationArgs) throws IOException {
        logger.info(npmDriver.ci(workingDir.toFile(), installationArgs, logger));
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
        if (scopes.isEmpty()) {
            return new ArrayList<>();
        }
        for (NpmScope scope : scopes) {
            List<String> extraListArgs = new ArrayList<>();
            extraListArgs.add("--" + scope);
            JsonNode jsonNode = npmDriver.list(workingDir.toFile(), extraListArgs);
            populateDependenciesMap(dependencies, getDependenciesMapFromLatestBuild(), jsonNode, scope);
        }

        return new ArrayList<>(dependencies.values());
    }

    private List<NpmScope> getNpmScopes() {
        List<NpmScope> scopes = new ArrayList<>();

        // typeRestriction default is ALL
        if (this.typeRestriction != TypeRestriction.PROD_ONLY) {
            scopes.add(NpmScope.DEVELOPMENT);
        }

        if (this.typeRestriction != TypeRestriction.DEV_ONLY) {
            scopes.add(NpmScope.PRODUCTION);
        }

        return scopes;
    }

    private Build createBuild(List<Dependency> dependencies, String moduleId) {
        Module module = new ModuleBuilder().type(ModuleType.NPM).id(moduleId).dependencies(dependencies).build();
        List<Module> modules = new ArrayList<>();
        modules.add(module);
        Build build = new Build();
        build.setModules(modules);
        return build;
    }

    /**
     * Populate the dependencies map for the specified scope by:
     * 1. Create npm dependency tree from root node of 'npm ls' command tree. Populate each node with name, version and scope.
     * 2. For each dependency, retrieve sha1 and md5 from Artifactory. Use the producer-consumer mechanism to parallelize it.
     */
    private void populateDependenciesMap(Map<String, Dependency> dependencies, Map<String, Dependency> previousBuildDependencies, JsonNode npmDependencyTree, NpmScope scope) throws Exception {
        // Set of packages that could not be found in Artifactory.
        Set<NpmPackageInfo> badPackages = Collections.synchronizedSet(new HashSet<>());
        DefaultMutableTreeNode rootNode = NpmDependencyTree.createDependencyTree(npmDependencyTree, scope);
        try (ArtifactoryDependenciesClient dependenciesClient = dependenciesClientBuilder.build()) {
            // Create producer Runnable.
            ProducerRunnableBase[] producerRunnable = new ProducerRunnableBase[]{new NpmExtractorProducer(rootNode)};
            // Create consumer Runnables.
            ConsumerRunnableBase[] consumerRunnables = new ConsumerRunnableBase[]{
                    new NpmExtractorConsumer(dependenciesClient, dependencies, previousBuildDependencies, badPackages),
                    new NpmExtractorConsumer(dependenciesClient, dependencies, previousBuildDependencies, badPackages),
                    new NpmExtractorConsumer(dependenciesClient, dependencies, previousBuildDependencies, badPackages)
            };
            // Create the deployment executor.
            ProducerConsumerExecutor deploymentExecutor = new ProducerConsumerExecutor(logger, producerRunnable, consumerRunnables, CONNECTION_POOL_SIZE);
            deploymentExecutor.start();
            if (!badPackages.isEmpty()) {
                logger.info((Arrays.toString(badPackages.toArray())));
                logger.info("The npm dependencies above could not be found in Artifactory and therefore are not included in the build-info. " +
                        "Make sure the dependencies are available in Artifactory for this build. " +
                        "Deleting the local cache will force populating Artifactory with these dependencies.");
            }
        }
    }

    private Map<String, Dependency> getDependenciesMapFromLatestBuild() throws IOException {
        if (StringUtils.isBlank(buildName)) {
            return Collections.emptyMap();
        }
        try (ArtifactoryBuildInfoClient buildInfoClient = buildInfoClientBuilder.build()) {
            // Get previous build's dependencies.
            Build previousBuildInfo = buildInfoClient.getBuildInfo(buildName, "LATEST");
            if (previousBuildInfo == null) {
                return Collections.emptyMap();
            }

            return getDependenciesMapFromBuild(previousBuildInfo);
        }
    }

    static Map<String, Dependency> getDependenciesMapFromBuild(Build build) {
        Map<String, Dependency> previousBuildDependencies = new ConcurrentHashMap<>();
        // Iterate over all modules and extract dependencies.
        List<Module> modules = build.getModules();
        for (Module module : modules) {
            List<Dependency> dependencies = module.getDependencies();
            if (dependencies != null) {
                for (Dependency dependency : dependencies) {
                    previousBuildDependencies.put(dependency.getId(), dependency);
                }
            }
        }
        return previousBuildDependencies;
    }

    private enum TypeRestriction {
        DEFAULT_RESTRICTION,
        ALL,
        DEV_ONLY,
        PROD_ONLY
    }
}
