package org.jfrog.build.extractor.npm.extractor;

import com.google.common.collect.ArrayListMultimap;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.client.ArtifactoryUploadResponse;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.build.util.VersionException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
@SuppressWarnings("unused")
public class NpmPublish extends NpmCommand {
    private static final long serialVersionUID = 1L;

    private ArrayListMultimap<String, String> properties;
    private Path packedFilePath;
    private boolean tarballProvided;
    private Artifact deployedArtifact;

    public NpmPublish(ArtifactoryBuildInfoClient client, ArrayListMultimap<String, String> properties, String executablePath, File ws, String deploymentRepository, String publishArgs) {
        super(client, executablePath, publishArgs, deploymentRepository, ws);
        this.properties = properties;
    }

    public Module execute() throws InterruptedException, VersionException, IOException {
        preparePrerequisites();
        if (!tarballProvided) {
            pack();
        }
        deploy();
        if (!tarballProvided) {
            deleteCreatedTarball();
        }

        List<Artifact> artifactList = new ArrayList<>();
        artifactList.add(deployedArtifact);

        return new ModuleBuilder().
                id(npmPackageInfo.getModuleId()).
                artifacts(artifactList).
                build();
    }

    private void preparePrerequisites() throws InterruptedException, VersionException, IOException {
        validateNpmVersion();
        validateRepoExists("Target repo must be specified");
        setPublishPath();
        setPackageInfo();
    }

    private void setPublishPath() {
        // Look for the publish path in the arguments
        String publishPathStr = args.stream()
                .filter(arg -> !arg.startsWith("-")) // Filter flags
                .findAny()
                .orElse(".")
                .replaceFirst("^~", System.getProperty("user.home")); // Replace tilde with user home

        packedFilePath = Paths.get(publishPathStr);
        if (!packedFilePath.isAbsolute()) {
            packedFilePath = ws.toPath().resolve(packedFilePath);
        }
    }

    private void setPackageInfo() throws IOException {
        if (Files.isDirectory(packedFilePath)) {
            try (FileInputStream fis = new FileInputStream(packedFilePath.resolve("package.json").toFile())) {
                npmPackageInfo.readPackageInfo(fis);
            }
        } else {
            readPackageInfoFromTarball(); // The provided path is not a directory, we're assuming this is a compressed npm package
        }
    }

    private void readPackageInfoFromTarball() throws IOException {
        if (!StringUtils.endsWith(packedFilePath.toString(), ".tgz")) {
            throw new IOException("Publish path must be a '.tgz' file or a directory containing package.json");
        }
        try (TarArchiveInputStream inputStream = new TarArchiveInputStream(
                new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(packedFilePath.toFile()))))) {
            TarArchiveEntry entry;
            while ((entry = inputStream.getNextTarEntry()) != null) {
                if (StringUtils.endsWith(entry.getName(), "package.json")) {
                    npmPackageInfo.readPackageInfo(inputStream);
                    tarballProvided = true;
                    return;
                }
            }
        }
        throw new IOException("Couldn't find package.json in " + packedFilePath.toString());
    }

    private void pack() throws IOException {
        npmDriver.pack(ws, args);
        packedFilePath = ws.toPath().resolve(npmPackageInfo.getExpectedPackedFileName());
    }

    private void deploy() throws IOException {
        readPackageInfoFromTarball();
        doDeploy();
    }

    private void doDeploy() throws IOException {
        DeployDetails deployDetails = new DeployDetails.Builder()
                .file(packedFilePath.toFile())
                .targetRepository(repo)
                .addProperties(properties)
                .artifactPath(npmPackageInfo.getDeployPath())
                .build();

        ArtifactoryUploadResponse response = ((ArtifactoryBuildInfoClient) client).deployArtifact(deployDetails);

        deployedArtifact = new ArtifactBuilder(npmPackageInfo.getModuleId())
                .md5(response.getChecksums().getMd5())
                .sha1(response.getChecksums().getSha1())
                .build();
    }

    private void deleteCreatedTarball() throws IOException {
        Files.deleteIfExists(packedFilePath);
    }
}
