package org.jfrog.build.extractor.nuget.extractor;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.builder.ModuleType;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.nuget.drivers.DotnetDriver;
import org.jfrog.build.extractor.nuget.drivers.NugetDriver;
import org.jfrog.build.extractor.nuget.drivers.ToolchainDriverBase;
import org.jfrog.build.extractor.nuget.types.NugetPackgesConfig;
import org.jfrog.build.extractor.nuget.types.NugetProjectAssets;
import org.jfrog.build.extractor.packageManager.PackageManagerExtractor;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.jfrog.build.extractor.packageManager.PackageManagerUtils.createArtifactoryClientConfiguration;

public class NugetRun extends PackageManagerExtractor {
    private static final String TEMP_DIR_PREFIX = "artifactory.plugin";
    private static final String NUGET_CONFIG_FILE_PREFIX = TEMP_DIR_PREFIX + ".nuget.config";
    private static final String PACKAGES_CONFIG = "packages.config";
    private static final String PROJECT_ASSETS = "project.assets.json";
    private static final String PROJECT_ASSETS_DIR = "obj";
    private static final String CONFIG_FILE_FORMAT = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<configuration>\n" +
            "\t<packageSources>\n" +
            "\t\t<add key=\"JFrogJenkins\" value=\"%s\" protocolVersion=\"%s\" />\n" +
            "\t</packageSources>\n" +
            "\t<packageSourceCredentials>\n" +
            "\t\t<JFrogJenkins>\n" +
            "\t\t\t<add key=\"Username\" value=\"%s\" />\n" +
            "\t\t\t<add key=\"ClearTextPassword\" value=\"%s\" />\n" +
            "\t\t</JFrogJenkins>\n" +
            "\t</packageSourceCredentials>\n" +
            "</configuration>";
    private static final String SOURCE_NAME = "BuildInfo.extractor.nuget";
    private static final String SLN_FILE_PARSING_REGEX = "^Project\\(\\\"(.*)";
    private static final String SHA1 = "SHA1";
    private static final String MD5 = "MD5";
    private static final String ABSENT_NUPKG_WARN_MSG = " Skipping adding this dependency to the build info. " +
            "This might be because the package already exists in a different NuGet cache," +
            " possibly the SDK's NuGetFallbackFolder cache. Removing the package from this cache may resolve the issue.";

    private static final long serialVersionUID = 1L;

    private final ArtifactoryManagerBuilder artifactoryManagerBuilder;
    private ToolchainDriverBase toolchainDriver;
    private Path workingDir;
    private Log logger;
    private Path path;
    private String resolutionRepo;
    private String username;
    private String password;
    private String apiProtocol;
    private String module;
    private String nugetCmdArgs;
    private List<String> dependenciesSources;
    private List<Module> modulesList = new ArrayList<>();

    /**
     * Run NuGet.
     *
     * @param artifactoryManagerBuilder  - ArtifactoryManager builder builder.
     * @param resolutionRepo - The repository it'll resolve from.
     * @param useDotnetCli   - Boolean indicates if .Net cli will be used.
     * @param nugetCmdArgs   - NuGet exec args.
     * @param logger         - The logger.
     * @param path           - Path to the directory containing the .sln file.
     * @param env            - Environment variables to use during npm execution.
     * @param module         - NuGet module
     * @param username       - JFrog platform username.
     * @param password       - JFrog platform password.
     * @param apiProtocol    - A string indicates which NuGet protocol should be used (V2/V3).
     */

    public NugetRun(ArtifactoryManagerBuilder artifactoryManagerBuilder, String resolutionRepo, boolean useDotnetCli, String nugetCmdArgs, Log logger, Path path, Map<String, String> env, String module, String username, String password, String apiProtocol) {
        this.artifactoryManagerBuilder = artifactoryManagerBuilder;
        this.toolchainDriver = useDotnetCli ? new DotnetDriver(env, path, logger) : new NugetDriver(env, path, logger);
        this.workingDir = Files.isDirectory(path) ? path : path.toAbsolutePath().getParent();
        this.logger = logger;
        this.path = path;
        this.resolutionRepo = resolutionRepo;
        this.nugetCmdArgs = StringUtils.isBlank(nugetCmdArgs) ? StringUtils.EMPTY : nugetCmdArgs;
        this.username = username;
        this.password = password;
        this.apiProtocol = apiProtocol;
        this.module = module;
    }

    private static String removeQuotes(String str) {
        if (str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    /**
     * NuGet allows the version include/exclude unnecessary zeros.
     * For example the alternative versions of "1.0.0" are: "1","1.0" and "1.0.0.0".
     * This method will return a list of the possible alternative versions.
     */
    protected static List<String> createAlternativeVersionForms(String originalVersion) {
        List<String> alternativeVersions = new ArrayList<>();
        List<String> versionParts = new ArrayList<>();
        Collections.addAll(versionParts, originalVersion.split("\\."));

        while (versionParts.size() < 4) {
            versionParts.add("0");
        }

        for (int i = 4; i > 0; i--) {
            String version = String.join(".", versionParts.subList(0, i));
            if (!version.equals(originalVersion)) {
                alternativeVersions.add(version);
            }
            if (!version.endsWith(".0")) {
                return alternativeVersions;
            }
        }
        return alternativeVersions;
    }

    /**
     * Allow running nuget restore using a new Java process.
     * Used only in Jenkins to allow running 'rtNuget run' in a docker container.
     */
    public static void main(String[] ignored) {
        try {
            ArtifactoryClientConfiguration clientConfiguration = createArtifactoryClientConfiguration();
            ArtifactoryManagerBuilder clientBuilder = new ArtifactoryManagerBuilder().setClientConfiguration(clientConfiguration, clientConfiguration.resolver);
            ArtifactoryClientConfiguration.PackageManagerHandler handler = clientConfiguration.packageManagerHandler;
            NugetRun nugetRun = new NugetRun(clientBuilder,
                    clientConfiguration.resolver.getRepoKey(),
                    clientConfiguration.dotnetHandler.useDotnetCoreCli(),
                    handler.getArgs(),
                    clientConfiguration.getLog(),
                    Paths.get(handler.getPath() != null ? handler.getPath() : "."),
                    clientConfiguration.getAllProperties(),
                    handler.getModule(),
                    clientConfiguration.resolver.getUsername(),
                    clientConfiguration.resolver.getPassword(),
                    clientConfiguration.dotnetHandler.apiProtocol());
            nugetRun.executeAndSaveBuildInfo(clientConfiguration);
        } catch (RuntimeException e) {
            ExceptionUtils.printRootCauseStackTrace(e, System.out);
            System.exit(1);
        }
    }

    @Override
    public Build execute() {
        Build build = null;
        try {
            prepareAndRunCmd();
            if (!StringUtils.isBlank(module)) {
                ModuleBuilder builder = new ModuleBuilder().type(ModuleType.NUGET).id(module);
                modulesList.add(builder.build());
            }
            collectDependencies();
            build = new Build();
            build.setModules(modulesList);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return build;
    }

    private void prepareAndRunCmd() throws Exception {
        try (ArtifactoryManager artifactoryManager = artifactoryManagerBuilder.build()) {
            List<String> extraArgs = new ArrayList<>();
            File configFile = prepareConfig(artifactoryManager);
            if (configFile != null) {
                String configPath = configFile.getAbsolutePath();
                extraArgs = StringUtils.isBlank(configPath) ? null : Arrays.asList(toolchainDriver.getFlagSyntax(ToolchainDriverBase.CONFIG_FILE_FLAG), configPath);
            }
            toolchainDriver.runCmd(nugetCmdArgs, extraArgs, null, true);
        }
    }

    /**
     * Writes a temporary NuGet configuration which will be used during the restore.
     * The resolution repository will be set as a source in the configuration.
     */
    private File prepareConfig(ArtifactoryManager artifactoryManager) throws Exception {
        File configFile = null;
        if (!nugetCmdArgs.contains(toolchainDriver.getFlagSyntax(ToolchainDriverBase.CONFIG_FILE_FLAG)) && !nugetCmdArgs.contains(toolchainDriver.getFlagSyntax(ToolchainDriverBase.SOURCE_FLAG))) {
            configFile = File.createTempFile(NUGET_CONFIG_FILE_PREFIX, null);
            configFile.deleteOnExit();
            addSourceToConfigFile(configFile.getAbsolutePath(), artifactoryManager, resolutionRepo, username, password, apiProtocol);
        }
        return configFile;
    }

    /**
     * We will write a temporary NuGet configuration using a string formater in order to support NuGet v3 protocol.
     * Currently the NuGet configuration utility doesn't allow setting protocolVersion.
     */
    private void addSourceToConfigFile(String configPath, ArtifactoryManager client, String repo, String username, String password, String apiProtocol) throws Exception{
        String sourceUrl = toolchainDriver.buildNugetSourceUrl(client, repo, apiProtocol);
        String protocolVersion = apiProtocol.substring(apiProtocol.length() - 1);
        String configFileText = String.format(CONFIG_FILE_FORMAT, sourceUrl, protocolVersion, username, password);
        try (PrintWriter out = new PrintWriter(configPath)) {
            out.println(configFileText);
        }
    }

    /**
     * projectPath specifies the location of a solution or a packages.config/.csproj file.
     * The operation mode of the restore command is determined as follows:
     * 1. projectPath is a folder - NuGet looks for a .sln file and uses that if found.
     * 2. projectPath is a .sln file - Restore packages identified by the solution.
     * 3. projectPath is a packages.config/.csproj file - Restore packages listed in the file.
     * <p>
     * if projectPath not specified:
     * NuGet looks for solution files in the current folder (single file is expected),
     * if there are no such files, NuGet looks for a packages.config/.csproj file
     *
     * @return string that represent the projectPath or EMPTY in case of non existing path.
     */
    File getProjectRootPath() throws IOException {
        File projectPath = workingDir.toFile();
        // We will check the first argument after the restore sub-command
        String[] args = nugetCmdArgs.split(" ");
        // If projectPath argument was specified we will work with it.
        if (args.length > 1 && !StringUtils.startsWith(args[1], "-")) {
            projectPath = new File(args[1]);
            if (!projectPath.isAbsolute()) {
                projectPath = new File(FilenameUtils.concat(workingDir.toString(), args[1]));
            }
        }
        if (projectPath.exists()) {
            if (projectPath.isFile()) {
                return projectPath;
            } else {
                return findProjectPathInDir(projectPath);
            }
        }
        return null;
    }

    private File findProjectPathInDir(File projectPathRoot) {
        File[] slnFiles = projectPathRoot.listFiles((dir, name) -> name.toLowerCase().endsWith(".sln"));
        if (slnFiles.length == 1) {
            return slnFiles[0];
        }
        File[] pkgsConfig = projectPathRoot.listFiles((dir, name) -> name.toLowerCase().endsWith(PACKAGES_CONFIG));
        if (pkgsConfig.length == 1) {
            return pkgsConfig[0];
        }
        File[] csprojFiles = projectPathRoot.listFiles((dir, name) -> name.toLowerCase().endsWith(".csproj"));
        if (csprojFiles.length == 1) {
            return csprojFiles[0];
        }
        return null;
    }

    /**
     * NuGet dependencies can be declared in one of the following methods:
     * 1. Using packages.config xml file
     * 2. in <project-name>.project.assets.json file
     * We search for all these files under the solution root dir, and later will match each project with its dependencies source file.
     */
    private List<String> findDependenciesSources(Path rootDir) {
        try (Stream<Path> walk = Files.walk(rootDir)) {
            Predicate<String> assetsJson = p -> p.endsWith(PROJECT_ASSETS);
            Predicate<String> packagesConfig = p -> p.endsWith(PACKAGES_CONFIG);
            List<String> result = walk.map(x -> x.toString())
                    .filter(assetsJson.or(packagesConfig)).collect(Collectors.toList());
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void collectDependencies() throws Exception {
        this.dependenciesSources = findDependenciesSources(workingDir);
        File projectPathRoot = getProjectRootPath();
        if (projectPathRoot.toString().endsWith(".sln")) {
            collectDependenciesFromSln(projectPathRoot);
            return;
        }
        collectDependenciesFromProjectDir(projectPathRoot.getParentFile());
    }

    /**
     * Parse the .sln file and collect dependencies for each project defined.
     */
    private void collectDependenciesFromSln(File slnFile) throws IOException, InterruptedException {
        Pattern pattern = Pattern.compile(SLN_FILE_PARSING_REGEX);
        String globalCachePath = toolchainDriver.globalPackagesCache();
        try (Stream<String> lines = Files.lines(slnFile.toPath())) {
            lines.filter(pattern.asPredicate()).forEach(line -> {
                try {
                    projectLineHandler(line, slnFile.getParentFile(), globalCachePath);
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage());
                }
            });
        }
    }

    private void collectDependenciesFromProjectDir(File projectRoot) throws Exception {
        List<Path> csprojFiles = Files.list(projectRoot.toPath())
                .filter(path -> path.toString().endsWith(".csproj")).collect(Collectors.toList());
        if (csprojFiles.size() == 1) {
            String csprojPath = csprojFiles.get(0).toString();
            String projectName = csprojFiles.get(0).getFileName().toString().replace(".csproj", "");
            String globalCachePath = toolchainDriver.globalPackagesCache();
            singleProjectHandler(projectName, csprojPath, globalCachePath);
        }
    }

    private void projectLineHandler(String line, File slnRootDir, String globalCachePath) throws Exception {
        // Fetch the project's name and path from the project line
        String[] projectDetails = line.split("=")[1].split(",");
        String projectName = removeQuotes(projectDetails[0].trim());
        String csprojPath = FilenameUtils.separatorsToSystem(removeQuotes(projectDetails[1].trim()));
        if (!csprojPath.endsWith(".csproj")) {
            logger.debug("Skipping project " + projectName + ", since it doesn't have a csproj file path.");
            return;
        }
        // We build a full path for the csproj file for single-project solutions.
        String csprojFullPath = (new File(slnRootDir, csprojPath)).getPath();
        singleProjectHandler(projectName, csprojFullPath, globalCachePath);
    }

    private void singleProjectHandler(String projectName, String csprojPath, String globalCachePath) throws Exception {
        String dependenciesSource = getDependenciesSource(projectName, csprojPath);
        if (StringUtils.isEmpty(dependenciesSource)) {
            logger.debug("Project dependencies was not found for project: " + projectName);
            return;
        }
        // Collect dependencies according to the correct method:
        // Check if project uses packages.config or project.assets.json
        List<Dependency> dependencies = new ArrayList<>();
        if (dependenciesSource.endsWith(PACKAGES_CONFIG)) {
            dependencies = collectDependenciesFromPackagesConfig(dependenciesSource, globalCachePath);
        } else if (dependenciesSource.endsWith(PROJECT_ASSETS)) {
            dependencies = collectDependenciesFromProjectAssets(dependenciesSource);
        }
        Module projectModule = new ModuleBuilder().type(ModuleType.NUGET).id(projectName).dependencies(dependencies).build();
        if (StringUtils.isBlank(module)) {
            modulesList.add(projectModule);
        } else {
            // If a custom module name was provided, we will aggregate all projects under the same module.
            modulesList.get(0).append(projectModule);
        }
    }

    /**
     * Iterate the dependencies sources list and look for the project's source
     */
    private String getDependenciesSource(String projectName, String csprojPath) {
        Path projectRootPath = Paths.get(csprojPath).getParent().normalize();
        String projectNamePattern = File.separator + projectName + File.separator;
        String projectPathPattern = projectRootPath + File.separator + PROJECT_ASSETS_DIR + File.separator;
        for (String source : dependenciesSources) {
            Path sourceRootPath = Paths.get(source).getParent().normalize();
            if (sourceRootPath.equals(projectRootPath) || source.contains(projectNamePattern) || source.contains(projectPathPattern)) {
                return source;
            }
        }
        return StringUtils.EMPTY;
    }

    private List<Dependency> collectDependenciesFromPackagesConfig(String packagesConfigPath, String globalCachePath) throws Exception {
        File packagesConfig = new File(packagesConfigPath);
        NugetPackgesConfig config = new NugetPackgesConfig();
        config.readPackageConfig(packagesConfig);
        List<Dependency> dependenciesList = new ArrayList<>();
        for (NugetPackgesConfig.ConfigPackage pkg : config.getPackages()) {
            Dependency pkgDependency = createDependency(pkg, globalCachePath);
            if (pkgDependency == null) {
                logger.warn(String.format("The following NuGet package %s with version %s was not found in the NuGet cache %s.%s",
                        pkg.getId(), pkg.getVersion(), globalCachePath, ABSENT_NUPKG_WARN_MSG));
                continue;
            }
            dependenciesList.add(pkgDependency);
        }
        return dependenciesList;
    }

    private Dependency createDependency(NugetPackgesConfig.ConfigPackage pkg, String globalCachePath) throws IOException, NoSuchAlgorithmException {
        boolean found = true;
        File nupkg = createNupkgFile(pkg.getId(), pkg.getVersion(), globalCachePath);
        if (!nupkg.exists()) {
            // If the original version can not be found in cache, we will check if one of the alternative version forms do exist.
            found = false;
            for (String v : createAlternativeVersionForms(pkg.getVersion())) {
                nupkg = createNupkgFile(pkg.getId(), v, globalCachePath);
                if (nupkg.exists()) {
                    found = true;
                    break;
                }
            }
        }
        if (found) {
            Map<String, String> checksums = FileChecksumCalculator.calculateChecksums(nupkg, MD5, SHA1);
            Dependency dependency = new DependencyBuilder()
                    .id(pkg.getId() + ':' + pkg.getVersion())
                    .md5(checksums.get(MD5)).sha1(checksums.get(SHA1))
                    .build();
            return dependency;
        }
        return null;
    }

    /**
     * The .nupkg file is placed in <cachePath>/<id>/<version>/<id>.<version>.nupgk
     */
    private File createNupkgFile(String id, String version, String cachePath) {
        String nupkgFileName = id + "." + version + ".nupkg";
        String nupkgBasePath = FilenameUtils.concat(FilenameUtils.concat(cachePath, id), version);
        return new File(nupkgBasePath, nupkgFileName);
    }

    private List<Dependency> collectDependenciesFromProjectAssets(String projectAssetsPath) throws Exception {
        File projectAssets = new File(projectAssetsPath);
        List<Dependency> dependenciesList = new ArrayList<>();
        NugetProjectAssets assets = new NugetProjectAssets();
        assets.readProjectAssets(projectAssets);
        for (Map.Entry<String, NugetProjectAssets.Library> entry : assets.getLibraries().entrySet()) {
            String pkgKey = entry.getKey();
            NugetProjectAssets.Library library = entry.getValue();
            if (library.getType().equals("project")) {
                continue;
            }
            File nupkg = new File(assets.getPackagesPath(), library.getNupkgFilePath());
            if (nupkg.exists()) {
                Map<String, String> checksums = FileChecksumCalculator.calculateChecksums(nupkg, MD5, SHA1);
                Dependency dependency = new DependencyBuilder()
                        .id(pkgKey.replace('/', ':'))
                        .md5(checksums.get(MD5)).sha1(checksums.get(SHA1))
                        .build();
                dependenciesList.add(dependency);
            } else {
                if (isPackagePartOfTargetDependencies(library.getPath(), assets.getTargets())) {
                    logger.warn(String.format("The file %s doesn't exist in the NuGet cache directory but it does exist as a target in the assets files. %s",
                            nupkg.getPath(), ABSENT_NUPKG_WARN_MSG));
                    continue;
                }
                throw new Exception(String.format("The file %s doesn't exist in the NuGet cache directory.", nupkg.getPath()));
            }
        }
        return dependenciesList;
    }

    /**
     * If the package is included in the targets section of the assets.json file,
     * this is a .NET dependency that shouldn't be included in the dependencies list (it come with the SDK).
     * Those files are located under <sdk path>/NuGetFallbackFolder
     */
    private boolean isPackagePartOfTargetDependencies(String dependencyId, Map<String, Map<String, NugetProjectAssets.TargetDependency>> targets) {
        String dependencyName = dependencyId.split("/")[0];
        for (Map<String, NugetProjectAssets.TargetDependency> dependencies : targets.values()) {
            for (String dependencyKey : dependencies.keySet()) {
                if (dependencyName.equalsIgnoreCase(dependencyKey)) {
                    return true;
                }
            }
        }
        return false;
    }
}
