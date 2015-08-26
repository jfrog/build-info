package docker

import org.junit.rules.ExternalResource
import org.junit.runner.Description
import org.junit.runners.model.Statement

import java.lang.annotation.*

/**
 * This annotation would make sure to run docker container before the tests will start
 *
 * Requirements:
 * Make sure to add environment variable or system property to the test process:
 *  key - "DOCKER_HTTP_HOST"
 *  value - http url to the docker server host
 *
 * For example: DOCKER_HTTP_HOST="http://boot2docker:2375/"
 *
 * There are two ways to use this annotation:
 * 1) Give registry and repo input in order to fetch the image from docker registry
 * 2) Give docker file path (dockerFilePath) in order to build the image from it
 *
 * Either way image ID (imageId) is mandatory
 *
 *
 * @author Lior Hasson
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@RuleAnnotation(RunWithDocker.RuleImpl.class)
@interface RunWithDocker {

    /**
     * registry from which docker should download the image
     */
    String registry() default ""

    /**
     * Repo in the registry from which docker should look for the image
     */
    String repo() default ""

    /**
     * Image ID, mandatory field
     */
    String imageId()

    /**
     * Image tag, default is 'latest'
     */
    String tag() default ""

    String containerId() default ""

    /**
     * Path to a local docker file which docker should build the image from
     */
    String dockerFilePath() default ""

    /**
     * Port that is exposed from the container.
     *
     * <p>
     * When a container is started, this port from the container are mapped to
     * port on the host (hostPort)
     */
    int containerPort() default 0

    /**
    * Port that is mapped from the container to the host.
    */
    int hostPort() default 0

    class RuleImpl extends ExternalResource implements TestAnnotationExtension {
        private Annotation hostAnnotation
        private Docker docker = new Docker()

        @Override
        void init(Annotation hostAnnotation) {
            this.hostAnnotation = hostAnnotation
        }

        @Override
        Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                void evaluate() throws Throwable {
                    RunWithDocker runWithDocker = ((RunWithDocker)hostAnnotation)
                    buildDocker(runWithDocker)
                    docker.run()
                    base.evaluate()
                }

                private Docker buildDocker(RunWithDocker runWithDocker) {
                    def registry = System.getenv("DOCKER_REGISTRY")?:System.getProperty("dockerRegistry")
                    def repo = System.getenv("DOCKER_REPO")?:System.getProperty("dockerRepo")
                    def imageId = System.getenv("DOCKER_IMAGE_ID")?:System.getProperty("dockerImageId")
                    def tag = System.getenv("DOCKER_TAG")?:System.getProperty("dockerTag")
                    def dockerFilePath = System.getenv("DOCKER_FILE_PATH")?:System.getProperty("dockerFilePath")

                    def containerPort = System.getenv("DOCKER_CONTAINER_PORT")?:System.getProperty("dockerContainerPort")
                    if(containerPort){
                        containerPort = containerPort.toInteger()
                    }

                    def hostPort = System.getenv("DOCKER_HOST_PORT")?:System.getProperty("dockerHostPort")
                    if(hostPort){
                        hostPort = hostPort.toInteger()
                    }

                    docker.registry(registry?:runWithDocker.registry()).
                            repo(repo?:runWithDocker.repo()).
                            imageId(imageId?:runWithDocker.imageId()).
                            tag(tag?:runWithDocker.tag()).
                            containerId(runWithDocker.containerId()).
                            dockerFilePath(dockerFilePath?:runWithDocker.dockerFilePath()).
                            containerPort(containerPort?:runWithDocker.containerPort()).
                            hostPort(hostPort?:runWithDocker.hostPort())
                }
            }
        }

        @Override
        void after() {
            if(docker.ping()) {
                docker.close()
            }
        }


    }
}
