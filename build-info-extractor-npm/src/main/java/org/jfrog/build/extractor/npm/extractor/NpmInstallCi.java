package org.jfrog.build.extractor.npm.extractor;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.npm.types.NpmProject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.jfrog.build.extractor.packageManager.PackageManagerUtils.createArtifactoryClientConfiguration;

/**
 * @author Yahav Itzhak
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class NpmInstallCi extends NpmCommand {

    NpmBuildInfoExtractor buildInfoExtractor;
    List<String> commandArgs;
    boolean isCiCommand;

    /**
     * Run npm install or npm ci commands.
     *
     * @param artifactoryManagerBuilder - ArtifactoryManagerBuilder.
     * @param resolutionRepository      - The repository it'll resolve from.
     * @param commandArgs               - Npm command args.
     * @param buildName                 - The build's name.
     * @param module                    - The build's module.
     * @param logger                    - The logger.
     * @param path                      - Path to directory contains package.json or path to '.tgz' file.
     * @param env                       - Environment variables to use during npm execution.
     */
    public NpmInstallCi(ArtifactoryManagerBuilder artifactoryManagerBuilder, String resolutionRepository, String commandArgs, Log logger, Path path, Map<String, String> env, String module, String buildName, boolean isCiCommand, String project) {
        super(artifactoryManagerBuilder, resolutionRepository, logger, path, env);
        buildInfoExtractor = new NpmBuildInfoExtractor(artifactoryManagerBuilder, npmDriver, logger, module, buildName, project);
        this.commandArgs = StringUtils.isBlank(commandArgs) ? new ArrayList<>() : Arrays.asList(commandArgs.trim().split("\\s+"));
        this.isCiCommand = isCiCommand;
    }

    /**
     * Allow running npm install using a new Java process.
     * Used only in Jenkins to allow running 'rtNpm install' in a docker container.
     */
    public static void main(String[] ignored) {
        try {
            ArtifactoryClientConfiguration clientConfiguration = createArtifactoryClientConfiguration();
            ArtifactoryManagerBuilder artifactoryManagerBuilder = new ArtifactoryManagerBuilder().setClientConfiguration(clientConfiguration, clientConfiguration.resolver);
            ArtifactoryClientConfiguration.PackageManagerHandler packageManagerHandler = clientConfiguration.packageManagerHandler;
            ArtifactoryClientConfiguration.NpmHandler npmHandler = clientConfiguration.npmHandler;
            NpmInstallCi npmInstall = new NpmInstallCi(artifactoryManagerBuilder,
                    clientConfiguration.resolver.getRepoKey(),
                    packageManagerHandler.getArgs(),
                    clientConfiguration.getLog(),
                    Paths.get(packageManagerHandler.getPath() != null ? packageManagerHandler.getPath() : "."),
                    clientConfiguration.getAllProperties(),
                    packageManagerHandler.getModule(),
                    clientConfiguration.info.getBuildName(),
                    npmHandler.isCiCommand(),
                    clientConfiguration.info.getProject());
            npmInstall.executeAndSaveBuildInfo(clientConfiguration);
        } catch (RuntimeException e) {
            ExceptionUtils.printRootCauseStackTrace(e, System.out);
            System.exit(1);
        }
    }

    @Override
    public BuildInfo execute() {
        try (ArtifactoryManager artifactoryManager = artifactoryManagerBuilder.build()) {
            this.artifactoryManager = artifactoryManager;
            validatePath();
            validateArtifactoryVersion();
            validateNpmVersion();
            validateRepoExists(artifactoryManager, repo, "Source repo must be specified");

            NpmProject npmProject = new NpmProject(commandArgs, repo, workingDir, isCiCommand);
            return buildInfoExtractor.extract(npmProject);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
