package org.jfrog.build.extractor.npm.extractor;

import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.util.VersionException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NpmPublish extends NpmCommand {
    private static final long serialVersionUID = 1L;

    private transient ArtifactoryBuildInfoClient client;
    private String publishPath;

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
    }

    private void setPublishPath() {
        // Look for the publish path in the arguments
        publishPath = args.stream()
                .filter(arg -> !arg.startsWith("-")) // Filter flags
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Target repo must be specified"))
                .replaceFirst("^~", System.getProperty("user.home")); // Replace tilde with user home

        Path path = Paths.get(publishPath);
        if (!path.isAbsolute()) {
            publishPath = ws.toPath().resolve(path).toAbsolutePath().toString();
        }
    }

    private void validateRepoExists() throws IOException {
        if (!client.isRepoExist(repo)) {
            throw new IOException("Repo " + repo + " doesn't exist");
        }
    }

}
