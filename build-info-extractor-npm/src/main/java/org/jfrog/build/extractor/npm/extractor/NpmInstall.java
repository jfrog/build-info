package org.jfrog.build.extractor.npm.extractor;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.npm.types.NpmProject;

import java.nio.file.Path;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class NpmInstall extends NpmCommand {

    NpmBuildInfoExtractor buildInfoExtractor;

    /**
     * Install npm package.
     *
     * @param clientBuilder        - Build Info client builder.
     * @param resolutionRepository - The repository it'll resolve from.
     * @param executablePath       - Npm executable path.
     * @param args                 - Npm args.
     * @param logger               - The logger.
     * @param path                 - Path to directory contains package.json or path to '.tgz' file.
     */
    public NpmInstall(ArtifactoryDependenciesClientBuilder clientBuilder, String resolutionRepository, String args, String executablePath, Log logger, Path path) {
        super(clientBuilder, executablePath, args, resolutionRepository, logger, path);
        buildInfoExtractor = new NpmBuildInfoExtractor(clientBuilder, executablePath, logger);
    }

    public Build execute() {
        try (ArtifactoryDependenciesClient dependenciesClient = (ArtifactoryDependenciesClient) clientBuilder.build()) {
            client = dependenciesClient;
            validateArtifactoryVersion();
            validateNpmVersion();
            validateRepoExists("Source repo must be specified");

            NpmProject npmProject = new NpmProject(args, repo, workingDir);
            return buildInfoExtractor.extract(npmProject);
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e), e);
        }
        return null;
    }
}
