package docker;

import org.apache.commons.lang.StringUtils;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


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
@Retention(RUNTIME)
@Target({TYPE})
@Documented
@RuleAnnotation(RunWithDocker.RuleImpl.class)
public @interface RunWithDocker {

    /**
     * registry from which docker should download the image
     */
    String registry() default StringUtils.EMPTY;

    /**
     * repo in the registry from which docker should look for the image
     */
    String repo() default StringUtils.EMPTY;

    /**
     * image ID, mandatory field
     */
    String imageId();

    String tag() default StringUtils.EMPTY;

    String containerId() default StringUtils.EMPTY;

    /**
     * Path to a local docker file which docker should build the image from
     */
    String dockerFilePath() default StringUtils.EMPTY;

    /**
     * Port that is exposed from the container.
     *
     * <p>
     * When a container is started, this port from the container are mapped to
     * port on the host (hostPort)
     */
    int containerPort() default 0;

    /**
    * Port that is mapped from the container to the host.
    */
    int hostPort() default 0;

    class RuleImpl extends ExternalResource implements TestAnnotationExtension {
        private Annotation hostAnnotation;
        private Docker docker = new Docker();

        @Override
        public void init(Annotation hostAnnotation) {
            this.hostAnnotation = hostAnnotation;
        }

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    RunWithDocker runWithDocker = ((RunWithDocker)hostAnnotation);
                    docker.registry(runWithDocker.registry()).
                            repo(runWithDocker.repo()).
                            imageId(runWithDocker.imageId()).
                            tag(runWithDocker.tag()).
                            containerId(runWithDocker.containerId()).
                            dockerFilePath(runWithDocker.dockerFilePath()).
                            containerPort(runWithDocker.containerPort()).
                            hostPort(runWithDocker.hostPort());

                    docker.run();
                    base.evaluate();
                }
            };
        }

        @Override
        public void after() {
            docker.close();
        }
    }
}
