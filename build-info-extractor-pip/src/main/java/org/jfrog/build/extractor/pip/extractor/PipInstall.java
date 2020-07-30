package org.jfrog.build.extractor.pip.extractor;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientBuilderBase;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.packageManager.PackageManagerExtractor;
import org.jfrog.build.extractor.packageManager.PackageManagerUtils;
import org.jfrog.build.extractor.pip.PipDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.jfrog.build.extractor.packageManager.PackageManagerUtils.createArtifactoryClientConfiguration;

/**
 * Created by Bar Belity on 09/07/2020.
 */
public class PipInstall extends PackageManagerExtractor {
    private static final long serialVersionUID = 1L;
    private static final String ARTIFACTORY_PIP_API_START = "/api/pypi/";
    private static final String ARTIFACTORY_PIP_API_END = "/simple";

    private ArtifactoryClientBuilderBase clientBuilder;
    private PipDriver pipDriver;
    private Path workingDir;
    private String repo;
    private Log logger;
    private Path path;
    private List<String> installArgs;
    private String username;
    private String password;
    private String module;

    public PipInstall(ArtifactoryDependenciesClientBuilder clientBuilder, String resolutionRepository, String installArgs, Log logger, Path path, Map<String, String> env, String module, String username, String password, String envActivation) {

        this.clientBuilder = clientBuilder;
        this.path = path;
        this.logger = logger;
        this.repo = resolutionRepository;
        this.username = username;
        this.password = password;
        this.pipDriver = StringUtils.isNotBlank(envActivation)
                ? new PipDriver(envActivation + " && pip", env)
                : new PipDriver("pip", env);
        this.workingDir = Files.isDirectory(path)
                ? path
                : path.toAbsolutePath().getParent();
        this.installArgs = StringUtils.isBlank(installArgs)
                ? new ArrayList<>()
                : Arrays.asList(installArgs.trim().split("\\s+"));
        this.module = StringUtils.isBlank(module)
                ? this.workingDir.getFileName().toString()
                : module;
    }

    public Build execute() {
        try (ArtifactoryDependenciesClient dependenciesClient = (ArtifactoryDependenciesClient) clientBuilder.build()) {
            validateRepoExists(dependenciesClient, repo, "Source repo must be specified");
            String artifactoryUrlWithCredentials = PackageManagerUtils.createArtifactoryUrlWithCredentials(dependenciesClient.getArtifactoryUrl(), username, password, ARTIFACTORY_PIP_API_START + repo + ARTIFACTORY_PIP_API_END);

            // Run pip install with URL and get output.
            String installLog = pipDriver.install(path.toFile(), artifactoryUrlWithCredentials, installArgs, logger);
            logger.info(installLog);

            // Parse output and get all dependencies.
            PipBuildInfoExtractor buildInfoExtractor = new PipBuildInfoExtractor();
            try {
                return buildInfoExtractor.extract(dependenciesClient, repo, installLog, path, module, logger);
            } catch (IOException e) {
                throw new IOException("Build info collection failed", e);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Allow running pip install using a new Java process.
     * Used only in Jenkins to allow running 'rtPip install' in a docker container.
     */
    public static void main(String[] ignored) {
        try {
            ArtifactoryClientConfiguration clientConfiguration = createArtifactoryClientConfiguration();
            ArtifactoryDependenciesClientBuilder clientBuilder = new ArtifactoryDependenciesClientBuilder().setClientConfiguration(clientConfiguration, clientConfiguration.resolver);
            ArtifactoryClientConfiguration.PackageManagerHandler pipHandler = clientConfiguration.packageManagerHandler;
            PipInstall pipInstall = new PipInstall(clientBuilder,
                    clientConfiguration.resolver.getRepoKey(),
                    pipHandler.getPackageManagerArgs(),
                    clientConfiguration.getLog(),
                    Paths.get(pipHandler.getPackageManagerPath() != null ? pipHandler.getPackageManagerPath() : "."),
                    clientConfiguration.getAllProperties(),
                    pipHandler.getPackageManagerModule(),
                    clientConfiguration.resolver.getUsername(),
                    clientConfiguration.resolver.getPassword(),
                    clientConfiguration.pipHandler.getEnvActivation());
            pipInstall.executeAndSaveBuildInfo(clientConfiguration);
        } catch (RuntimeException e) {
            ExceptionUtils.printRootCauseStackTrace(e, System.out);
            System.exit(1);
        }
    }
}
