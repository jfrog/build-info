package org.jfrog.build.extractor.go.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.builder.ModuleType;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ArtifactoryUploadResponse;
import org.jfrog.build.extractor.builder.ArtifactBuilder;
import org.jfrog.build.extractor.builder.ModuleBuilder;
import org.jfrog.build.extractor.ci.Artifact;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.extractor.ci.Module;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.build.extractor.go.GoDriver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.jfrog.build.api.util.FileChecksumCalculator.*;
import static org.jfrog.build.extractor.packageManager.PackageManagerUtils.createArtifactoryClientConfiguration;

@SuppressWarnings({"unused", "WeakerAccess"})
public class GoPublish extends GoCommand {

    private static final String LOCAL_PKG_FILENAME = "package";
    private static final String LOCAL_TMP_PKG_PREFIX = "tmp.";
    private static final String LOCAL_INFO_FILENAME = "package.info";
    private static final String GO_VERSION_PREFIX = "v";
    private static final String PKG_ZIP_FILE_EXTENSION = "zip";
    private static final String PKG_MOD_FILE_EXTENSION = "mod";
    private static final String PKG_INFO_FILE_EXTENSION = "info";

    private ArrayListMultimap<String, String> properties;
    private List<Artifact> artifactList = new ArrayList<>();
    private String deploymentRepo;
    private String version;

    /**
     * Publish go package.
     *
     * @param artifactoryManagerBuilder - Artifactory Manager builder for deployment.
     * @param properties                - Properties to set on each deployed artifact (Build name, Build number, etc...).
     * @param repo                      - Artifactory's repository for deployment.
     * @param path                      - Path to directory contains go.mod.
     * @param version                   - The package's version.
     * @param logger                    - The logger.
     */
    public GoPublish(ArtifactoryManagerBuilder artifactoryManagerBuilder, ArrayListMultimap<String, String> properties, String repo, Path path, String version, String module, Log logger) throws IOException {
        super(artifactoryManagerBuilder, path, module, logger);
        this.goDriver = new GoDriver(GO_CLIENT_CMD, null, path.toFile(), logger);
        this.moduleName = goDriver.getModuleName();
        this.properties = properties;
        this.deploymentRepo = repo;
        this.version = GO_VERSION_PREFIX + version;
    }

    public BuildInfo execute() {
        try (ArtifactoryManager artifactoryManager = artifactoryManagerBuilder.build()) {
            preparePrerequisites(deploymentRepo, artifactoryManager);
            publishPkg(artifactoryManager);
            return createBuild();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Allow publishing Go packages using a new Java process.
     * Used only in Jenkins to allow running 'rtGo publish' in a docker container.
     */
    public static void main(String[] ignored) {
        try {
            ArtifactoryClientConfiguration clientConfiguration = createArtifactoryClientConfiguration();
            ArtifactoryManagerBuilder artifactoryManagerBuilder = new ArtifactoryManagerBuilder().setClientConfiguration(clientConfiguration, clientConfiguration.publisher);
            ArtifactoryClientConfiguration.PackageManagerHandler packageManagerHandler = clientConfiguration.packageManagerHandler;

            GoPublish goPublish = new GoPublish(
                    artifactoryManagerBuilder,
                    ArrayListMultimap.create(clientConfiguration.publisher.getMatrixParams().asMultimap()),
                    clientConfiguration.publisher.getRepoKey(),
                    Paths.get(packageManagerHandler.getPath() != null ? packageManagerHandler.getPath() : ".").toAbsolutePath(),
                    clientConfiguration.goHandler.getGoPublishedVersion(),
                    packageManagerHandler.getModule(),
                    clientConfiguration.getLog()
            );
            goPublish.executeAndSaveBuildInfo(clientConfiguration);
        } catch (RuntimeException | IOException e) {
            ExceptionUtils.printRootCauseStackTrace(e, System.out);
            System.exit(1);
        }
    }

    /**
     * The deployment of a Go package requires 3 files:
     * 1. zip file of source files.
     * 2. go.mod file.
     * 3. go.info file.
     */
    private void publishPkg(ArtifactoryManager artifactoryManager) throws Exception {
        createAndDeployZip(artifactoryManager);
        deployGoMod(artifactoryManager);
        createAndDeployInfo(artifactoryManager);
    }

    private void createAndDeployZip(ArtifactoryManager artifactoryManager) throws Exception {
        // First, we create a temporary zip file of all project files.
        File tmpZipFile = archiveProjectDir();

        // Second, filter the raw zip file according to Go rules and create deployable zip can be later resolved.
        // We use the same code as Artifactory when he resolve a Go module directly from Github.
        File deployableZipFile = File.createTempFile(LOCAL_PKG_FILENAME, PKG_ZIP_FILE_EXTENSION, path.toFile());
        try (GoZipBallStreamer pkgArchiver = new GoZipBallStreamer(new ZipFile(tmpZipFile), moduleName, version, logger)) {
            pkgArchiver.writeDeployableZip(deployableZipFile);
            Artifact deployedPackage = deploy(artifactoryManager, deployableZipFile, PKG_ZIP_FILE_EXTENSION);
            artifactList.add(deployedPackage);
        } finally {
            Files.deleteIfExists(tmpZipFile.toPath());
            Files.deleteIfExists(deployableZipFile.toPath());
        }
    }

    private void deployGoMod(ArtifactoryManager artifactoryManager) throws Exception {
        String modLocalPath = getModFilePath();
        Artifact deployedMod = deploy(artifactoryManager, new File(modLocalPath), PKG_MOD_FILE_EXTENSION);
        artifactList.add(deployedMod);
    }

    private void createAndDeployInfo(ArtifactoryManager artifactoryManager) throws Exception {
        String infoLocalPath = path.toString() + File.separator + LOCAL_INFO_FILENAME;
        File infoFile = writeInfoFile(infoLocalPath);
        Artifact deployedInfo = deploy(artifactoryManager, infoFile, PKG_INFO_FILE_EXTENSION);
        artifactList.add(deployedInfo);
        infoFile.delete();
    }

    private File archiveProjectDir() throws IOException {
        File zipFile = File.createTempFile(LOCAL_TMP_PKG_PREFIX + LOCAL_PKG_FILENAME, PKG_ZIP_FILE_EXTENSION, path.toFile());
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile.toPath()));
            Stream<Path> pathFileTree = Files.walk(path)) {
            List<Path> pathsList = pathFileTree
                    // Remove .git dir content, directories and the temp zip file
                    .filter(p -> !path.relativize(p).startsWith(".git/") && !Files.isDirectory(p) && !p.toFile().getName().equals(zipFile.getName()))
                    .collect(Collectors.toList());
            for (Path filePath : pathsList) {
                // We need to have the parent hierarchy of the project in order to use the zip packaging code in org.jfrog.build.extractor.go.extractor.GoZipBallStreamer
                ZipEntry zipEntry = new ZipEntry(path.getParent().relativize(filePath).toString());
                zos.putNextEntry(zipEntry);
                Files.copy(filePath, zos);
                zos.closeEntry();
            }
        }

        return zipFile;
    }

    /**
     * pkg info is a json file containing:
     * 1. The package's version.
     * 2. The package creation timestamp.
     */
    private File writeInfoFile(String localInfoPath) throws IOException {
        File infoFile = new File(localInfoPath);
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> infoMap = new HashMap();
        Date date = new Date();
        Instant instant = date.toInstant();

        infoMap.put("Version", version);
        infoMap.put("Time", instant.toString());

        mapper.writeValue(infoFile, infoMap);
        return infoFile;
    }

    /**
     * Deploy pkg file and add it as an buildInfo's artifact
     */
    private Artifact deploy(ArtifactoryManager artifactoryManager, File deployedFile, String extension) throws Exception {
        String artifactName = version + "." + extension;
        Map<String, String> checksums = FileChecksumCalculator.calculateChecksums(deployedFile, MD5_ALGORITHM, SHA1_ALGORITHM, SHA256_ALGORITHM);
        String remotePath = moduleName + "/@v";
        DeployDetails deployDetails = new DeployDetails.Builder()
                .file(deployedFile)
                .targetRepository(deploymentRepo)
                .addProperties(properties)
                .artifactPath(remotePath + "/" + artifactName)
                .md5(checksums.get(MD5_ALGORITHM)).sha1(checksums.get(SHA1_ALGORITHM)).sha256(checksums.get(SHA256_ALGORITHM))
                .packageType(DeployDetails.PackageType.GO)
                .build();

        ArtifactoryUploadResponse response = artifactoryManager.upload(deployDetails);

        return new ArtifactBuilder(moduleName + ":" + artifactName)
                .md5(response.getChecksums().getMd5())
                .sha1(response.getChecksums().getSha1())
                .sha256(response.getChecksums().getSha256())
                .remotePath(remotePath)
                .build();
    }

    private BuildInfo createBuild() {
        BuildInfo buildInfo = new BuildInfo();
        String moduleId = StringUtils.defaultIfBlank(buildInfoModuleId, moduleName);
        Module module = new ModuleBuilder()
                .type(ModuleType.GO)
                .id(moduleId)
                .repository(deploymentRepo)
                .artifacts(artifactList)
                .build();
        buildInfo.setModules(Collections.singletonList(module));
        return buildInfo;
    }
}
