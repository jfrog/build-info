package org.jfrog.build.extractor.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.core.command.PushImageResultCallback;
import com.github.dockerjava.netty.NettyDockerCmdExecFactory;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.util.Log;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;


public class DockerAgentUtils {

    /**
     * Push docker image using the docker java client.
     *
     * @param imageTag
     * @param username
     * @param password
     * @param host
     * @param envVars
     */
    public static void pushImage(String imageTag, String username, String password, String host, Map<String, String> envVars, Log logger) {
        final AuthConfig authConfig = new AuthConfig();
        authConfig.withUsername(username);
        authConfig.withPassword(password);
        DockerClient dockerClient = null;
        try {
            dockerClient = getDockerClient(host, envVars);
            dockerClient.pushImageCmd(imageTag).withAuthConfig(authConfig).exec(new PushImageResultCallback()).awaitSuccess();
        } finally {
            closeQuietly(dockerClient,logger);
        }
    }

    /**
     * Gets the docker java client.
     *
     * @param host    - Docker daemon ip.
     * @param envVars - System env variables.
     * @return
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
            if (!StringUtils.isEmpty(host) && !host.equalsIgnoreCase("null")) {
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
        return DockerClientBuilder.getInstance(config).withDockerCmdExecFactory(new NettyDockerCmdExecFactory()).build();
    }

    /**
     * Get image Id from imageTag using DockerClient.
     *
     * @param imageTag
     * @param host
     * @param envVars
     * @return
     */
    public static String getImageIdFromTag(String imageTag, String host, Map<String, String> envVars,Log logger) {
        DockerClient dockerClient = null;
        try {
            dockerClient = getDockerClient(host, envVars);
            return dockerClient.inspectImageCmd(imageTag).exec().getId();
        } finally {
            closeQuietly(dockerClient,logger);
        }
    }

    private static void closeQuietly(DockerClient dockerClient,Log logger) {
        if (dockerClient != null) {
            try {
                dockerClient.close();
            } catch (IOException e) {
                logger.error("Closes docker client failed.");            }
        }
    }

    /**
     * Execute pull docker image on agent.
     *
     * @param imageTag
     * @param username
     * @param password
     * @param host
     * @param envVars
     */
    public static void pullImage(final String imageTag, String username, String password, String host, Map<String, String> envVars,Log logger) {
        final AuthConfig authConfig = new AuthConfig();
        authConfig.withUsername(username);
        authConfig.withPassword(password);
        DockerClient dockerClient = null;
        try {
            dockerClient = getDockerClient(host, envVars);
            dockerClient.pullImageCmd(imageTag).withAuthConfig(authConfig).exec(new PullImageResultCallback()).awaitSuccess();
        } finally {
            closeQuietly(dockerClient,logger);
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
        DockerClient dockerClient = DockerAgentUtils.getDockerClient(host, envVars);
        // Build the docker image with the name provided from env.
        BuildImageCmd buildImageCmd = dockerClient.buildImageCmd(Paths.get(projectPath).toFile()).withTags(new HashSet<>(Arrays.asList(imageName)));
        buildImageCmd.exec(new BuildImageResultCallback()).awaitImageId();
    }
}
