package org.jfrog.build.extractor.npm.extractor;

import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.PackageInfo;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBaseClient;
import org.jfrog.build.extractor.npm.utils.NpmDriver;
import org.jfrog.build.util.VersionCompatibilityType;
import org.jfrog.build.util.VersionException;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
abstract class NpmCommand implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final ArtifactoryVersion MIN_SUPPORTED_NPM_VERSION = new ArtifactoryVersion("5.4.0");

    PackageInfo npmPackageInfo = new PackageInfo();
    ArtifactoryBaseClient client;
    NpmDriver npmDriver;
    List<String> args;
    String repo;
    File ws;


    NpmCommand(ArtifactoryBaseClient client, String executablePath, String args, String repo, File ws) {
        this.client = client;
        this.args = Arrays.asList(StringUtils.trimToEmpty(args).split("\\s+"));
        this.npmDriver = new NpmDriver(executablePath);
        this.repo = repo;
        this.ws = ws;
    }

    void validateNpmVersion() throws IOException, InterruptedException, VersionException {
        String npmVersionStr = npmDriver.version(ws);
        ArtifactoryVersion npmVersion = new ArtifactoryVersion(npmVersionStr);
        if (!npmVersion.isAtLeast(MIN_SUPPORTED_NPM_VERSION)) {
            throw new VersionException("Couldn't execute npm task. Version must be at least " + MIN_SUPPORTED_NPM_VERSION.toString() + ".", VersionCompatibilityType.INCOMPATIBLE);
        }
    }

    void validateRepoExists() throws IOException {
        if (!client.isRepoExist(repo)) {
            throw new IOException("Repo " + repo + " doesn't exist");
        }
    }
}
