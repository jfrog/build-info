package org.jfrog.build.extractor.npm.extractor;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.util.VersionException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NpmPublish extends NpmCommand {
    private static final long serialVersionUID = 1L;

    private transient ArtifactoryBuildInfoClient client;
    private Path publishPath;

    public NpmPublish(ArtifactoryBuildInfoClient client, String executablePath, File ws, String deploymentRepository, String publishArgs) {
        super(executablePath, publishArgs, deploymentRepository, ws);
        this.client = client;
    }

    private void execute() throws InterruptedException, VersionException, IOException {
        preparePrerequisites();
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
    }

    private void validateRepoExists() throws IOException {
        if (!client.isRepoExist(repo)) {
            throw new IOException("Repo " + repo + " doesn't exist");
        }
    }

    private void setPackageInfo() throws IOException {
        if (Files.isDirectory(publishPath)) {
            npmPackageInfo.readPackageInfo(publishPath.toFile());
            return;
        }
        readPackageInfoFromTarball();
        // The provided path is not a directory, we assume this is a compressed npm package

    }

    private void readPackageInfoFromTarball() throws IOException {
        if (!StringUtils.endsWith(publishPath.toString(), ".tgz")) {
            throw new IOException("Publish path must be a '.tgz' file or a directory containing package.json");
        }

        try (TarArchiveInputStream inputStream = new TarArchiveInputStream(Files.newInputStream(publishPath))) {
            TarArchiveEntry entry;
            while ((entry = inputStream.getNextTarEntry()) != null) {
                if (StringUtils.equals(entry.getName(), "package.json")) { // TODO - Check with debugger!!!
                    npmPackageInfo.readPackageInfo(entry.getFile());
                }
            }
        }
        throw new IOException("Couldn't find package.json in " + publishPath.toString());
    }

}
