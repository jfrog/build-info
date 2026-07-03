package org.jfrog.build.extractor.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.core.command.PushImageResultCallback;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jfrog.build.api.util.Log;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

public class DockerJavaWrapper {

    /**
     * Push docker image using the docker java client.
     *
     * @param imageTag - Docker image to push.
     * @param username - The username to the docker registry.
     * @param password - The username to the docker registry.
     * @param host     - Docker daemon ip.
     * @param envVars  - Environment variables to use during docker push execution.
     */
    public static void pushImage(String imageTag, String username, String password, String host, Map<String, String> envVars, Log logger) throws InterruptedException {
        final AuthConfig authConfig = new AuthConfig();
        authConfig.withUsername(username);
        authConfig.withPassword(password);
        DockerClient dockerClient = null;
        try {
            dockerClient = getDockerClient(host, envVars);
            dockerClient.pushImageCmd(imageTag).withAuthConfig(authConfig).exec(new PushImageResultCallback()).awaitCompletion();
        } finally {
            closeQuietly(dockerClient, logger);
        }
    }

    /**
     * Gets the docker java client.
     *
     * @param host    - Docker daemon ip.
     * @param envVars - System env variables.
     * @return - Docker java client
     */
    public static DockerClient getDockerClient(String host, Map<String, String> envVars) {
        if (envVars == null) {
            throw new IllegalStateException("envVars must not be null");
        }
        DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();
        if (envVars.containsKey(DefaultDockerClientConfig.DOCKER_HOST)) {
            configBuilder.withDockerHost(envVars.get(DefaultDockerClientConfig.DOCKER_HOST));
        } else {
            // If open JDK is used and the host is null
            // then instead of a null reference, the host is the string "null".
            if (StringUtils.isNotBlank(host) && !host.equalsIgnoreCase("null")) {
                configBuilder.withDockerHost(host);
            }
        }
        if (envVars.containsKey(DefaultDockerClientConfig.DOCKER_TLS_VERIFY)) {
            configBuilder.withDockerTlsVerify(envVars.get(DefaultDockerClientConfig.DOCKER_TLS_VERIFY));
        }
        if (envVars.containsKey(DefaultDockerClientConfig.DOCKER_CERT_PATH)) {
            configBuilder.withDockerCertPath(envVars.get(DefaultDockerClientConfig.DOCKER_CERT_PATH));
        }

        DockerClientConfig config = configBuilder.build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .build();

        return DockerClientBuilder.getInstance(config).withDockerHttpClient(httpClient).build();
    }

    /**
     * Get image Id from imageTag using DockerClient.
     *
     * @param imageTag - Docker image tag.
     * @param host     - Docker daemon ip.
     * @param envVars  - System env variables.
     * @return - Docker image tag
     */
    public static String getImageIdFromTag(String imageTag, String host, Map<String, String> envVars, Log logger) {
        return DockerJavaWrapper.InspectImage(imageTag, host, envVars, logger).getId();
    }

    /**
     * Get image Architecture from imageTag using DockerClient.
     *
     * @param imageTag - Docker image tag.
     * @param host     - Docker daemon ip.
     * @param envVars  - System env variables.
     * @return tuple of [Image-Architecture, Image-OS]
     */
    public static Pair<String, String> getImageArch(String imageTag, String host, Map<String, String> envVars, Log logger) {
        InspectImageResponse response = DockerJavaWrapper.InspectImage(imageTag, host, envVars, logger);
        String imageArch = response.getArch();
        String imageOs = response.getOs();
        return Pair.of(imageArch, imageOs);
    }

    /**
     * Get image Id from imageTag using DockerClient.
     *
     * @param imageTag - Docker image tag.
     * @param host     - Docker daemon ip.
     * @param envVars  - System env variables.
     * @return - Docker image tag
     */
    public static InspectImageResponse InspectImage(String imageTag, String host, Map<String, String> envVars, Log logger) {
        DockerClient dockerClient = null;
        try {
            dockerClient = getDockerClient(host, envVars);
            return dockerClient.inspectImageCmd(imageTag).exec();
        } finally {
            closeQuietly(dockerClient, logger);
        }
    }

    private static void closeQuietly(DockerClient dockerClient, Log logger) {
        if (dockerClient != null) {
            try {
                dockerClient.close();
            } catch (IOException e) {
                logger.error("Closes docker client failed.");
            }
        }
    }

    /**
     * Execute pull docker image on agent.
     *
     * @param imageTag - Docker image to pull.
     * @param username - The username to the docker registry.
     * @param password - The username to the docker registry.
     * @param host     - Docker daemon ip.
     * @param envVars  - Environment variables to use during docker pull execution.
     */
    public static void pullImage(final String imageTag, String username, String password, String host, Map<String, String> envVars, Log logger) throws InterruptedException {
        final AuthConfig authConfig = new AuthConfig();
        authConfig.withUsername(username);
        authConfig.withPassword(password);
        DockerClient dockerClient = null;
        try {
            dockerClient = getDockerClient(host, envVars);
            dockerClient.pullImageCmd(imageTag).withAuthConfig(authConfig).exec(new PullImageResultCallback()).awaitCompletion();
        } finally {
            closeQuietly(dockerClient, logger);
        }
    }

    /**
     * Build a docker image from docker file.
     *
     * @param imageName   - The image ID of the result docker build
     * @param host        - Docker daemon ip.
     * @param envVars     - System env variables.
     * @param projectPath - Location of the docker file
     */
    public static void buildImage(String imageName, String host, Map<String, String> envVars, String projectPath) {
        DockerClient dockerClient = DockerJavaWrapper.getDockerClient(host, envVars);
        // Build the docker image with the name provided from env.
        BuildImageCmd buildImageCmd = dockerClient.buildImageCmd(Paths.get(projectPath).toFile()).withTags(new HashSet<>(Collections.singletonList(imageName)));
        buildImageCmd.exec(new BuildImageResultCallback()).awaitImageId();
    }
}
