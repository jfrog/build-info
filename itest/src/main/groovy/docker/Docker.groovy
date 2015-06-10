package docker

import org.apache.commons.lang.StringUtils
import org.jfrog.util.docker.DockerClient
import org.jfrog.util.docker.DockerContainer
import org.jfrog.util.docker.DockerImage


/**
 * @author Lior Hasson
 */
class Docker {
    String dockerUrl = System.getenv("DOCKER_HTTP_HOST")?:System.getProperty("dockerHttpHost")

    private String repo
    private String registry
    private String imageId
    private String containerId
    private String tag
    private String dockerFilePath
    private int containerPort
    private int hostPort

    DockerClient dockerClient = new DockerClient(dockerUrl)
    DockerImage image
    DockerContainer container


    void run() {
        if(StringUtils.isEmpty(containerId)){
            containerId = imageId
        }

        containerId = "itest_$containerId"

        //Create Image from Docker File
        if(StringUtils.isNotEmpty(dockerFilePath)){
            image = dockerClient.getImage(imageId).withTag(tag)
            container = image.getNewContainer(containerId)
            buildArtifactoryServer()
        }
        //Pull exists image
        else{
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
    void close(){
        try {
            container.doDelete(true, true)
        }
        finally {
            dockerClient.close()
        }
    }

    boolean ping(){
        return  dockerClient.ping()
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
        if(containerPort != 0 && hostPort != 0){
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
