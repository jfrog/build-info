package docker.docker;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


/**
 * Meta-annotation for annotations that introduces a {@link TestAnnotationExtension} for test.
 *
* @author Lior Hasson
*/
@Retention(RUNTIME)
@Target(ANNOTATION_TYPE)
@Documented
public @interface RuleAnnotation {
    Class<? extends TestAnnotationExtension> value();
}
