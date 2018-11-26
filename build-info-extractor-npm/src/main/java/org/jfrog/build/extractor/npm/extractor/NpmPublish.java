package org.jfrog.build.extractor.npm.extractor;

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
public class NpmPublish extends NpmCommand {
    private static final long serialVersionUID = 1L;

    private transient ArtifactoryBuildInfoClient client;
    private Path publishPath;
    private Path packedFilePath;
    private boolean tarballProvided;
    private Artifact deployedArtifact;

    public NpmPublish(ArtifactoryBuildInfoClient client, String executablePath, File ws, String deploymentRepository, String publishArgs) {
        super(executablePath, publishArgs, deploymentRepository, ws);
        this.client = client;
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
        setPublishPath();
        validateRepoExists();
        setPackageInfo();
    }

    private void setPublishPath() {
        // Look for the publish path in the arguments
        String publishPathStr = args.stream()
                .filter(arg -> !arg.startsWith("-")) // Filter flags
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Target repo must be specified"))
                .replaceFirst("^~", System.getProperty("user.home")); // Replace tilde with user home

        publishPath = Paths.get(publishPathStr);
        if (!publishPath.isAbsolute()) {
            publishPath = ws.toPath().resolve(publishPath);
        }
        packedFilePath = publishPath;
    }

    private void validateRepoExists() throws IOException {
        if (!client.isRepoExist(repo)) {
            throw new IOException("Repo " + repo + " doesn't exist");
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
                if (StringUtils.endsWith(entry.getName(), "package.json")) { // TODO - Check with debugger!!!
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
        DeployDetails deployDetails = new DeployDetails.Builder().file(packedFilePath.toFile()).targetRepository(repo).artifactPath(npmPackageInfo.getDeployPath()).build();
        ArtifactoryUploadResponse response = client.deployArtifact(deployDetails);
        ArtifactBuilder artifactBuilder = new ArtifactBuilder(npmPackageInfo.getModuleId()).
                md5(response.getChecksums().getMd5()).
                sha1(response.getChecksums().getSha1());
        deployedArtifact = artifactBuilder.build();
    }

    private void deleteCreatedTarball() throws IOException {
        Files.deleteIfExists(packedFilePath);
    }
}
