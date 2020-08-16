package org.jfrog.build.extractor.docker.extractor;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.docker.DockerAgentUtils;
import org.jfrog.build.extractor.packageManager.PackageManagerExtractor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.jfrog.build.extractor.packageManager.PackageManagerUtils.createArtifactoryClientConfiguration;

public class DockerPull extends PackageManagerExtractor {
    ArtifactoryDependenciesClientBuilder clientBuilder;
    String imageTag;
    String host;
    Log logger;
    private Map<String, String> env;
    private String username;
    private String password;
    private List<Module> modulesList = new ArrayList<>();

    /**
     * Docker push.
     *
     * @param clientBuilder - Build Info client builder.
     * @param imageTag      - Docker image to pull.
     * @param host          - Docker daemon ip.
     * @param username      - The username to the docker registry.
     * @param password      - The password to the docker registry.
     * @param logger        - log output.
     * @param env           - Environment variables to use during docker push execution.
     */
    public DockerPull(ArtifactoryDependenciesClientBuilder clientBuilder, String imageTag, String host, String username, String password, Log logger, Map<String, String> env) {
        this.clientBuilder = clientBuilder;
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
     * @param ignored
     */
    public static void main(String[] ignored) {
        try {
            ArtifactoryClientConfiguration clientConfiguration = createArtifactoryClientConfiguration();
            ArtifactoryDependenciesClientBuilder clientBuilder = new ArtifactoryDependenciesClientBuilder().setClientConfiguration(clientConfiguration, clientConfiguration.resolver);
            ArtifactoryClientConfiguration.DockerHandler dockerHandler = clientConfiguration.dockerHandler;
            DockerPull dockerPush = new DockerPull(clientBuilder,
                    dockerHandler.getImageTag(),
                    dockerHandler.getHost(),
                    clientConfiguration.resolver.getUsername(),
                    clientConfiguration.resolver.getPassword(),
                    clientConfiguration.getLog(),
                    clientConfiguration.getAllProperties());
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
        DockerAgentUtils.pullImage(imageTag, username, password, host, env, logger);
        logger.info("Successfully pulled docker image: " + imageTag);
        return new Build();
    }
}
