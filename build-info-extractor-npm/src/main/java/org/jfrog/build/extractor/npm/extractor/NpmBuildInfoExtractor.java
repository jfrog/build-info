package org.jfrog.build.extractor.npm.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.builder.ModuleType;
import org.jfrog.build.api.ci.BuildInfo;
import org.jfrog.build.api.ci.Dependency;
import org.jfrog.build.api.ci.Module;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.BuildInfoExtractor;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.jfrog.build.client.PreemptiveHttpClientBuilder.CONNECTION_POOL_SIZE;

/**
 * @author Yahav Itzhak
 */
public class NpmBuildInfoExtractor implements BuildInfoExtractor<NpmProject> {
    private static final String NPMRC_BACKUP_FILE_NAME = "jfrog.npmrc.backup";
    private static final String NPMRC_FILE_NAME = ".npmrc";

    private final ArtifactoryManagerBuilder artifactoryManagerBuilder;
    private NpmPackageInfo npmPackageInfo = new NpmPackageInfo();
    private TypeRestriction typeRestriction;
    private NpmDriver npmDriver;
    private String npmRegistry;
    private Properties npmAuth;
    private String buildName;
    private final String project;
    private String npmProxy;
    private String module;
    private Log logger;

    NpmBuildInfoExtractor(ArtifactoryManagerBuilder artifactoryManagerBuilder,
                          NpmDriver npmDriver, Log logger, String module, String buildName, String project) {
        this.artifactoryManagerBuilder = artifactoryManagerBuilder;
        this.npmDriver = npmDriver;
        this.logger = logger;
        this.module = module;
        this.buildName = buildName;
        this.project = project;
        this.typeRestriction = TypeRestriction.DEFAULT_RESTRICTION;
    }

    @Override
    public BuildInfo extract(NpmProject npmProject) throws Exception {
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
        try (ArtifactoryManager artifactoryManager = artifactoryManagerBuilder.build()) {
            setNpmAuth(artifactoryManager);
            setRegistryUrl(artifactoryManager, resolutionRepository);
            setNpmProxy(artifactoryManager);
        }
        readPackageInfoFromPackageJson(workingDir);
        backupProjectNpmrc(workingDir);
    }

    private void setNpmAuth(ArtifactoryManager artifactoryManage) throws IOException {
        npmAuth = artifactoryManage.getNpmAuth();
    }

    private void setRegistryUrl(ArtifactoryManager artifactoryManage, String resolutionRepository) {
        npmRegistry = artifactoryManage.getUrl();
        if (!StringUtils.endsWith(npmRegistry, "/")) {
            npmRegistry += "/";
        }
        npmRegistry += "api/npm/" + resolutionRepository;
    }

    private void setNpmProxy(ArtifactoryManager artifactoryManage) {
        ProxyConfiguration proxyConfiguration = artifactoryManage.getProxyConfiguration();
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

        try (Scanner configScanner = new Scanner(configList)) {
            while (configScanner.hasNextLine()) {
                String currOption = configScanner.nextLine();
                if (StringUtils.isBlank(currOption)) {
                    continue;
                }

                String[] splitOption = currOption.split("=", 2);
                if (splitOption.length < 2) {
                    continue;
                }

                String key = splitOption[0].trim();
                if (isValidKey(key)) {
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

        // Since we run the get config command with "--json=false", the returned config includes 'json=false' which we don't want to force
        boolean isJsonOutputRequired = this.npmDriver.isJson(workingDir.toFile(), commandArgs);
        npmrcBuilder.append("json = ").append(isJsonOutputRequired).append("\n");

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

    /**
     * Adds an array-value config to a StringBuilder of .npmrc file, in the following format:
     * key[] = value
     */
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

    // For testing
    TypeRestriction getTypeRestriction() {
        return typeRestriction;
    }

    void setTypeRestriction(String key, String value) {
        // From npm 7, type restriction is determined by 'omit' and 'include' (both appear in 'npm config ls').
        // Other options (like 'dev', 'production' and 'only') are deprecated, but if they're used anyway - 'omit' and 'include' are automatically calculated.
        // So 'omit' is always preferred, if it exists.
        if (key.equals("omit")) {
            if (StringUtils.contains(value, "dev")) {
                this.typeRestriction = TypeRestriction.PROD_ONLY;
            } else {
                this.typeRestriction = TypeRestriction.ALL;
            }
        }
        // Until npm 6, configurations in 'npm config ls' are sorted by priority in descending order, so typeRestriction should be set only if it was not set before
        else if (this.typeRestriction == TypeRestriction.DEFAULT_RESTRICTION) {
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
            populateDependenciesMap(dependencies, getDependenciesMapFromLatestBuild(), jsonNode, scope, workingDir);
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

    private BuildInfo createBuild(List<Dependency> dependencies, String moduleId) {
        Module module = new ModuleBuilder().type(ModuleType.NPM).id(moduleId).dependencies(dependencies).build();
        List<Module> modules = new ArrayList<>();
        modules.add(module);
        BuildInfo buildInfo = new BuildInfo();
        buildInfo.setModules(modules);
        return buildInfo;
    }

    /**
     * Populate the dependencies map for the specified scope by:
     * 1. Create npm dependency tree from root node of 'npm ls' command tree. Populate each node with name, version and scope.
     * 2. For each dependency, retrieve sha1 and md5 from Artifactory. Use the producer-consumer mechanism to parallelize it.
     */
    private void populateDependenciesMap(Map<String, Dependency> dependencies, Map<String, Dependency> previousBuildDependencies,
                                         JsonNode npmDependencyTree, NpmScope scope, Path workingDir) throws Exception {
        // Set of packages that could not be found in Artifactory.
        Set<NpmPackageInfo> badPackages = Collections.synchronizedSet(new HashSet<>());
        DefaultMutableTreeNode rootNode = NpmDependencyTree.createDependencyTree(npmDependencyTree, scope, workingDir);
        try (ArtifactoryManager artifactoryManager = artifactoryManagerBuilder.build()) {
            // Create producer Runnable.
            ProducerRunnableBase[] producerRunnable = new ProducerRunnableBase[]{new NpmExtractorProducer(rootNode)};
            // Create consumer Runnables.
            ConsumerRunnableBase[] consumerRunnables = new ConsumerRunnableBase[]{
                    new NpmExtractorConsumer(artifactoryManager, dependencies, previousBuildDependencies, badPackages),
                    new NpmExtractorConsumer(artifactoryManager, dependencies, previousBuildDependencies, badPackages),
                    new NpmExtractorConsumer(artifactoryManager, dependencies, previousBuildDependencies, badPackages)
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
        try (ArtifactoryManager artifactoryManager = artifactoryManagerBuilder.build()) {
            // Get previous build's dependencies.
            BuildInfo previousBuildInfo = artifactoryManager.getBuildInfo(buildName, "LATEST", project);
            if (previousBuildInfo == null) {
                return Collections.emptyMap();
            }

            return getDependenciesMapFromBuild(previousBuildInfo);
        }
    }

    static Map<String, Dependency> getDependenciesMapFromBuild(BuildInfo buildInfo) {
        Map<String, Dependency> previousBuildDependencies = new ConcurrentHashMap<>();
        // Iterate over all modules and extract dependencies.
        List<Module> modules = buildInfo.getModules();
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

    enum TypeRestriction {
        DEFAULT_RESTRICTION,
        ALL,
        DEV_ONLY,
        PROD_ONLY
    }
}
