package org.jfrog.build.extractor.docker.extractor;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.extractor.ci.Module;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.docker.DockerJavaWrapper;
import org.jfrog.build.extractor.docker.DockerUtils;
import org.jfrog.build.extractor.docker.types.DockerImage;

import java.io.IOException;
import java.util.Map;

import static org.jfrog.build.extractor.packageManager.PackageManagerUtils.createArtifactoryClientConfiguration;

public class DockerPull extends DockerCommand {
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
    public DockerPull(ArtifactoryManagerBuilder artifactoryManagerBuilder,
                      String imageTag, String host, String targetRepository, String username, String password, Log logger, Map<String, String> env) {
        super(artifactoryManagerBuilder, imageTag, host, targetRepository, username, password, logger, env);
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
            ArtifactoryManagerBuilder artifactoryManagerBuilder = new ArtifactoryManagerBuilder().setClientConfiguration(clientConfiguration, clientConfiguration.resolver);
            // Load artifact and BuildInfo properties from resolver section in the BuildInfo.properties file.
            ArtifactoryClientConfiguration.DockerHandler dockerHandler = clientConfiguration.dockerHandler;
            // Init DockerPull.
            DockerPull dockerPush = new DockerPull(artifactoryManagerBuilder,
                    dockerHandler.getImageTag(),
                    dockerHandler.getHost(),
                    clientConfiguration.resolver.getRepoKey(),
                    clientConfiguration.resolver.getUsername(),
                    clientConfiguration.resolver.getPassword(),
                    clientConfiguration.getLog(),
                    clientConfiguration.getAllProperties());

            // Exe docker pull & collect build info.
            dockerPush.executeAndSaveBuildInfo(clientConfiguration);
        } catch (RuntimeException e) {
            ExceptionUtils.printRootCauseStackTrace(e, System.out);
            System.exit(1);
        }
    }

    @Override
    public BuildInfo execute() {
        String message = "Pulling image: " + imageTag;
        if (StringUtils.isNotEmpty(host)) {
            message += " using docker daemon host: " + host;
        }
        logger.info(message);
        try {
            DockerJavaWrapper.pullImage(imageTag, username, password, host, env, logger);
            String imageId = DockerJavaWrapper.getImageIdFromTag(imageTag, host, env, logger);
            Pair<String, String> archDetails = DockerJavaWrapper.getImageArch(imageTag, host, env, logger);
            DockerImage image = new DockerImage(imageId, imageTag, "", targetRepository, artifactoryManagerBuilder, archDetails.getLeft(), archDetails.getRight());
            Module module = image.generateBuildInfoModule(logger, DockerUtils.CommandType.Pull);
            if (module.getDependencies() == null || module.getDependencies().size() == 0) {
                logger.warn("Could not find docker image: " + imageTag + " in Artifactory.");
            }
            BuildInfo buildInfo = new BuildInfo();
            modulesList.add(module);
            buildInfo.setModules(modulesList);
            logger.info("Successfully pulled docker image: " + imageTag);
            return buildInfo;
        } catch (IOException | InterruptedException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
