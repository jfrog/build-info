package docker

import org.apache.commons.lang.StringUtils
import org.jfrog.util.docker.DockerClient
import org.jfrog.util.docker.DockerContainer
import org.jfrog.util.docker.DockerImage
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * @author Lior Hasson
 */
class Docker {
    private static final Logger log = LoggerFactory.getLogger(Docker.class);

    private String dockerUrl = System.getenv("DOCKER_HTTP_HOST")?:System.getProperty("dockerHttpHost")
    private String repo
    private String registry
    private String imageId
    private String containerId
    private String tag
    private String dockerFilePath
    private int containerPort
    private int hostPort

    DockerClient dockerClient
    DockerImage image
    DockerContainer container

    Docker() {
        if (dockerUrl != null) {
            dockerClient = new DockerClient(dockerUrl)
        }
    }

    void run() {
        if (dockerUrl == null) {
            println "Docker URL not configured. Skipping Artifactory server setup."
            return
        }
        if (!ping()) {
            println "No ping to Docker server. Skipping Artifactory server setup."
            return
        }
        if (StringUtils.isEmpty(containerId)){
            containerId = imageId
        }

        containerId = "itest_$containerId"

        //Create Image from Docker File
        if (StringUtils.isNotEmpty(dockerFilePath)){
            image = dockerClient.getImage(imageId).withTag(tag)
            container = image.getNewContainer(containerId)
            buildArtifactoryServer()
        }
        //Pull exists image
        else {
            image = dockerClient.getImage(imageId).withTag(tag).fromRepo(repo).fromRegistry(registry)
            container = image.getNewContainer(containerId)
            pullImage()
        }

        createContainer()
        startContainer()
    }

    /**
     * Delete the container include its volume
     */
    void close() {
        if (dockerUrl == null) {
            return
        }
        try {
            container.doDelete(true, true)
        }
        finally {
            dockerClient.close()
        }
    }

    boolean ping() {
        if (dockerUrl != null) {
            return dockerClient.ping()
        }
    }

    private void buildArtifactoryServer() {
        dockerClient.build(new File(
                        this.getClass().getResource(dockerFilePath).getPath()),
                image.getFullImageName(false), image.getTag()
        )
    }

    private void pullImage() {
        image.doCreate()
    }

    private void createContainer() {
        Map<String, Map> portMapping = new HashMap<>()
        portMapping.put("$containerPort/tcp", new HashMap())
        container.getCreateConfig().setExposedPorts(portMapping)
        container.getCreateConfig().setHostname("artifactory")
        container.doCreate()
    }

    private void startContainer() {
        if (containerPort != 0 && hostPort != 0) {
            container.getStartConfig().addPortBinding(containerPort, "tcp", "", hostPort)
        }

        container.doStart()
    }

    Docker repo(String repo) {
        this.repo = repo
        return this
    }

    Docker registry(String registry) {
        this.registry = registry
        return this
    }

    Docker imageId(String imageId) {
        this.imageId = imageId
        return this
    }

    Docker containerId(String containerId) {
        this.containerId = containerId
        return this
    }

    Docker tag(String tag) {
        this.tag = tag
        return this
    }

    Docker dockerFilePath(String dockerFilePath) {
        this.dockerFilePath = dockerFilePath
        return this
    }

    Docker containerPort(int containerPort) {
        this.containerPort = containerPort
        return this
    }

    Docker hostPort(int hostPort) {
        this.hostPort = hostPort
        return this
    }
}
