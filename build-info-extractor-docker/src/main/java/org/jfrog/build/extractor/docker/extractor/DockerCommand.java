package org.jfrog.build.extractor.docker.extractor;

import org.jfrog.build.extractor.ci.Module;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.packageManager.PackageManagerExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

abstract class DockerCommand extends PackageManagerExtractor {
    ArtifactoryManagerBuilder artifactoryManagerBuilder;
    Map<String, String> env;
    String username;
    String password;
    String host;
    Log logger;
    String imageTag;
    String targetRepository;
    List<Module> modulesList = new ArrayList<>();

    /**
     * Docker command.
     *
     * @param imageTag - Docker image.
     * @param host     - Docker daemon ip.
     * @param username - The username to the docker registry.
     * @param password - The password to the docker registry.
     * @param logger   - log output.
     * @param env      - Environment variables to use during docker push execution.
     */
    public DockerCommand(ArtifactoryManagerBuilder artifactoryManagerBuilder,
                         String imageTag, String host, String targetRepository, String username, String password, Log logger, Map<String, String> env) {
        this.artifactoryManagerBuilder = artifactoryManagerBuilder;
        this.targetRepository = targetRepository;
        this.logger = logger;
        this.imageTag = imageTag;
        this.host = host;
        this.username = username;
        this.password = password;
        this.env = env;
    }
}
