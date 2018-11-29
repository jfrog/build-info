package org.jfrog.build.extractor.npm.extractor;

import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.PackageInfo;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientBuilderBase;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBaseClient;
import org.jfrog.build.extractor.npm.NpmDriver;
import org.jfrog.build.util.VersionCompatibilityType;
import org.jfrog.build.util.VersionException;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
abstract class NpmCommand implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final ArtifactoryVersion MIN_SUPPORTED_ARTIFACTORY_VERSION = new ArtifactoryVersion("5.5.2");
    private static final ArtifactoryVersion MIN_SUPPORTED_NPM_VERSION = new ArtifactoryVersion("5.4.0");

    PackageInfo npmPackageInfo = new PackageInfo();
    ArtifactoryClientBuilderBase clientBuilder;
    ArtifactoryBaseClient client;
    NpmDriver npmDriver;
    List<String> args;
    Path workingDir;
    String repo;
    Path path;


    NpmCommand(ArtifactoryClientBuilderBase clientBuilder, String executablePath, String args, String repo, Path path) {
        this.clientBuilder = clientBuilder;
        this.args = StringUtils.isBlank(args) ? new ArrayList<>() :  Arrays.asList(args.trim().split("\\s+"));
        this.npmDriver = new NpmDriver(executablePath);
        this.workingDir = Files.isDirectory(path) ? path : path.getParent();
        this.repo = repo;
        this.path = path;
    }

    void validateArtifactoryVersion() throws VersionException {
        if (!client.getArtifactoryVersion().isAtLeast(MIN_SUPPORTED_ARTIFACTORY_VERSION)) {
            throw new VersionException("Couldn't execute npm task. Artifactory version must be at least " + MIN_SUPPORTED_ARTIFACTORY_VERSION.toString() + ".", VersionCompatibilityType.INCOMPATIBLE);
        }
    }

    void validateNpmVersion() throws IOException, InterruptedException, VersionException {
        String npmVersionStr = npmDriver.version(workingDir.toFile());
        ArtifactoryVersion npmVersion = new ArtifactoryVersion(npmVersionStr);
        if (!npmVersion.isAtLeast(MIN_SUPPORTED_NPM_VERSION)) {
            throw new VersionException("Couldn't execute npm task. Npm version must be at least " + MIN_SUPPORTED_NPM_VERSION.toString() + ".", VersionCompatibilityType.INCOMPATIBLE);
        }
    }

    void validateRepoExists(String repoNotSpecifiedMsg) throws IOException {
        if (StringUtils.isBlank(repo)) {
            throw new IllegalArgumentException(repoNotSpecifiedMsg);
        }
        if (!client.isRepoExist(repo)) {
            throw new IOException("Repo " + repo + " doesn't exist");
        }
    }
}
