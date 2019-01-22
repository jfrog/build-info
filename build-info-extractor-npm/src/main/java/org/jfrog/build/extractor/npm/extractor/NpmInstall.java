package org.jfrog.build.extractor.npm.extractor;

import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.npm.types.NpmProject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Yahav Itzhak
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class NpmInstall extends NpmCommand {

    NpmBuildInfoExtractor buildInfoExtractor;
    List<String> installArgs;

    /**
     * Install npm package.
     *
     * @param clientBuilder        - Build Info client builder.
     * @param resolutionRepository - The repository it'll resolve from.
     * @param executablePath       - Npm executable path.
     * @param installArgs          - Npm install args.
     * @param logger               - The logger.
     * @param path                 - Path to directory contains package.json or path to '.tgz' file.
     */
    public NpmInstall(ArtifactoryDependenciesClientBuilder clientBuilder, String resolutionRepository, String installArgs, String executablePath, Log logger, Path path, Map<String, String> env) {
        super(clientBuilder, executablePath, resolutionRepository, logger, path, env);
        buildInfoExtractor = new NpmBuildInfoExtractor(clientBuilder, npmDriver, logger);
        this.installArgs = StringUtils.isBlank(installArgs) ? new ArrayList<>() : Arrays.asList(installArgs.trim().split("\\s+"));
    }

    public Build execute() {
        try (ArtifactoryDependenciesClient dependenciesClient = (ArtifactoryDependenciesClient) clientBuilder.build()) {
            client = dependenciesClient;
            validateArtifactoryVersion();
            validateNpmVersion();
            validateRepoExists("Source repo must be specified");

            NpmProject npmProject = new NpmProject(installArgs, repo, workingDir);
            return buildInfoExtractor.extract(npmProject);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }
}
