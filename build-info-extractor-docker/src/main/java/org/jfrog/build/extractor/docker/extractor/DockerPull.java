package org.jfrog.build.extractor.docker.extractor;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.docker.DockerJavaWrapper;
import org.jfrog.build.extractor.docker.DockerUtils;
import org.jfrog.build.extractor.docker.types.DockerImage;
import org.jfrog.build.extractor.packageManager.PackageManagerExtractor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.jfrog.build.extractor.docker.DockerUtils.initTempDir;
import static org.jfrog.build.extractor.packageManager.PackageManagerUtils.createArtifactoryClientConfiguration;

public class DockerPull extends PackageManagerExtractor {
    private final ArtifactoryDependenciesClientBuilder dependenciesClientBuilder;
    private final ArtifactoryBuildInfoClientBuilder buildInfoClientBuilder;
    private final Map<String, String> env;
    private final String username;
    private final String password;
    private final List<Module> modulesList = new ArrayList<>();
    private final String imageTag;
    private final String host;
    private final String targetRepository;
    private final Log logger;

    /**
     * Docker pull.
     *
     * @param imageTag - Docker image to pull.
     * @param host     - Docker daemon ip.
     * @param username - The username to the docker registry.
     * @param password - The password to the docker registry.
     * @param logger   - log output.
     * @param env      - Environment variables to use during docker push execution.
     */
    public DockerPull(ArtifactoryBuildInfoClientBuilder buildInfoClientBuilder, ArtifactoryDependenciesClientBuilder dependenciesClientBuilder,
                      String imageTag, String host, String targetRepository, String username, String password, Log logger, Map<String, String> env) {
        this.dependenciesClientBuilder = dependenciesClientBuilder;
        this.buildInfoClientBuilder = buildInfoClientBuilder;
        this.targetRepository = targetRepository;
        this.logger = logger;
        this.imageTag = imageTag;
        this.host = host;
        this.username = username;
        this.password = password;
        this.env = env;
    }

    /**
     * Allow running docker pull using a new Java process.
     *
     * @param ignored ignores input incoming params.
     */
    public static void main(String[] ignored) {
        try {
            // Client builders.
            ArtifactoryClientConfiguration clientConfiguration = createArtifactoryClientConfiguration();
            ArtifactoryDependenciesClientBuilder dependenciesClientBuilder = new ArtifactoryDependenciesClientBuilder().setClientConfiguration(clientConfiguration, clientConfiguration.resolver);
            ArtifactoryBuildInfoClientBuilder buildInfoClientBuilder = new ArtifactoryBuildInfoClientBuilder().setClientConfiguration(clientConfiguration, clientConfiguration.resolver);
            // Load artifact and BuildInfo properties from resolver section in the BuildInfo.properties file.
            ArtifactoryClientConfiguration.DockerHandler dockerHandler = clientConfiguration.dockerHandler;
            // Init DockerPull.
            DockerPull dockerPush = new DockerPull(buildInfoClientBuilder, dependenciesClientBuilder,
                    dockerHandler.getImageTag(),
                    dockerHandler.getHost(),
                    clientConfiguration.resolver.getRepoKey(),
                    clientConfiguration.resolver.getUsername(),
                    clientConfiguration.resolver.getPassword(),
                    clientConfiguration.getLog(),
                    clientConfiguration.getAllProperties());
            // Init temp dir.
            initTempDir(new File(clientConfiguration.info.getGeneratedBuildInfoFilePath()));
            // Exe docker pull & collect build info.
            dockerPush.executeAndSaveBuildInfo(clientConfiguration);
        } catch (RuntimeException e) {
            ExceptionUtils.printRootCauseStackTrace(e, System.out);
            System.exit(1);
        }
    }

    @Override
    public Build execute() {
        String message = "Pulling image: " + imageTag;
        if (StringUtils.isNotEmpty(host)) {
            message += " using docker daemon host: " + host;
        }
        logger.info(message);
        try {
            DockerJavaWrapper.pullImage(imageTag, username, password, host, env, logger);
            String imageId = DockerJavaWrapper.getImageIdFromTag(imageTag, host, env, logger);
            List<String> archDetails = DockerJavaWrapper.getImageArch(imageTag, host, env, logger);
            DockerImage image = new DockerImage(imageId, imageTag, targetRepository, buildInfoClientBuilder, dependenciesClientBuilder, archDetails.get(0), archDetails.get(1));
            Module module = image.generateBuildInfoModule(logger, DockerUtils.CommandType.Pull);
            if (module.getDependencies() == null || module.getDependencies().size() == 0) {
                logger.warn("Could not find docker image: " + imageTag + " in Artifactory.");
            }
            Build build = new Build();
            modulesList.add(module);
            build.setModules(modulesList);
            logger.info("Successfully pulled docker image: " + imageTag);
            return build;
        } catch (IOException | InterruptedException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
