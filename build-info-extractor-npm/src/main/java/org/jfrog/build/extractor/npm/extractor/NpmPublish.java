package org.jfrog.build.extractor.npm.extractor;

import com.google.common.collect.ArrayListMultimap;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.builder.ModuleType;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ArtifactoryUploadResponse;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.build.util.VersionException;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jfrog.build.extractor.packageManager.PackageManagerUtils.createArtifactoryClientConfiguration;

/**
 * @author Yahav Itzhak
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class NpmPublish extends NpmCommand {
    private final ArrayListMultimap<String, String> properties;
    private Artifact deployedArtifact;
    private boolean tarballProvided;
    private final String module;

    /**
     * Publish npm package.
     *
     * @param artifactoryManagerBuilder - Artifactory manager builder builder.
     * @param properties                - The Artifact properties to set (Build name, Build number, etc...).
     * @param path                      - Path to directory contains package.json or path to '.tgz' file.
     * @param deploymentRepository      - The repository it'll deploy to.
     * @param logger                    - The logger.
     * @param env                       - Environment variables to use during npm execution.
     */
    public NpmPublish(ArtifactoryManagerBuilder artifactoryManagerBuilder, ArrayListMultimap<String, String> properties, Path path, String deploymentRepository, Log logger, Map<String, String> env, String module) {
        super(artifactoryManagerBuilder, deploymentRepository, logger, path, env);
        this.properties = properties;
        this.module = module;
    }

    /**
     * Allow running npm publish using a new Java process.
     * Used only in Jenkins to allow running 'rtNpm publish' in a docker container.
     */
    public static void main(String[] ignored) {
        try {
            ArtifactoryClientConfiguration clientConfiguration = createArtifactoryClientConfiguration();
            ArtifactoryManagerBuilder artifactoryManagerBuilder = new ArtifactoryManagerBuilder().setClientConfiguration(clientConfiguration, clientConfiguration.publisher);
            ArtifactoryClientConfiguration.PackageManagerHandler npmHandler = clientConfiguration.packageManagerHandler;
            NpmPublish npmPublish = new NpmPublish(artifactoryManagerBuilder,
                    ArrayListMultimap.create(clientConfiguration.publisher.getMatrixParams().asMultimap()),
                    Paths.get(npmHandler.getPath() != null ? npmHandler.getPath() : "."),
                    clientConfiguration.publisher.getRepoKey(),
                    clientConfiguration.getLog(),
                    clientConfiguration.getAllProperties(),
                    npmHandler.getModule());
            npmPublish.executeAndSaveBuildInfo(clientConfiguration);
        } catch (RuntimeException e) {
            ExceptionUtils.printRootCauseStackTrace(e, System.out);
            System.exit(1);
        }
    }

    @Override
    public Build execute() {
        try (ArtifactoryManager artifactoryManager = artifactoryManagerBuilder.build()) {
            this.artifactoryManager = artifactoryManager;
            preparePrerequisites();
            if (!tarballProvided) {
                pack();
            }
            deploy();
            if (!tarballProvided) {
                deleteCreatedTarball();
            }
            return createBuild();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void setPackageInfo() throws IOException {
        if (Files.isDirectory(path)) {
            try (FileInputStream fis = new FileInputStream(path.resolve("package.json").toFile())) {
                npmPackageInfo.readPackageInfo(fis);
            }
        } else {
            readPackageInfoFromTarball(); // The provided path is not a directory, we're assuming this is a compressed npm package
        }
    }

    private void readPackageInfoFromTarball() throws IOException {
        if (!StringUtils.endsWith(path.toString(), ".tgz")) {
            throw new IOException("Publish path must be a '.tgz' file or a directory containing package.json");
        }
        try (TarArchiveInputStream inputStream = new TarArchiveInputStream(
                new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(path.toFile()))))) {
            TarArchiveEntry entry;
            while ((entry = inputStream.getNextTarEntry()) != null) {
                Path parent = Paths.get(entry.getName()).getParent();
                if (parent != null && StringUtils.equals(parent.toString(), "package") && StringUtils.endsWith(entry.getName(), "package.json")) {
                    npmPackageInfo.readPackageInfo(inputStream);
                    tarballProvided = true;
                    return;
                }
            }
        }
        throw new IOException("Couldn't find package.json in " + path.toString());
    }

    private void pack() throws IOException {
        logger.info(npmDriver.pack(workingDir.toFile(), new ArrayList<>()));
        path = path.resolve(npmPackageInfo.getExpectedPackedFileName());
    }

    private void deploy() throws IOException {
        readPackageInfoFromTarball();
        doDeploy();
    }

    private void preparePrerequisites() throws InterruptedException, VersionException, IOException {
        validateArtifactoryVersion();
        validateNpmVersion();
        validateRepoExists(artifactoryManager, repo, "Target repo must be specified");
        setPackageInfo();
    }

    private void deleteCreatedTarball() throws IOException {
        Files.deleteIfExists(path);
    }

    private Build createBuild() {
        String moduleID = StringUtils.isNotBlank(module) ? module : npmPackageInfo.toString();
        List<Artifact> artifactList = Collections.singletonList(deployedArtifact);
        Module module = new ModuleBuilder().type(ModuleType.NPM).id(moduleID).repository(repo).artifacts(artifactList).build();
        List<Module> modules = Collections.singletonList(module);
        Build build = new Build();
        build.setModules(modules);
        return build;
    }

    private void doDeploy() throws IOException {
        DeployDetails deployDetails = new DeployDetails.Builder()
                .file(path.toFile())
                .targetRepository(repo)
                .addProperties(properties)
                .artifactPath(npmPackageInfo.getDeployPath())
                .packageType(DeployDetails.PackageType.NPM)
                .build();

        ArtifactoryUploadResponse response = artifactoryManager.upload(deployDetails);

        deployedArtifact = new ArtifactBuilder(npmPackageInfo.getModuleId())
                .md5(response.getChecksums().getMd5())
                .sha1(response.getChecksums().getSha1())
                .remotePath(StringUtils.substringBeforeLast(npmPackageInfo.getDeployPath(), "/"))
                .build();
    }
}
