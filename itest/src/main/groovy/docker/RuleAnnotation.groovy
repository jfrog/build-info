package docker

import java.lang.annotation.*

/**
 * Meta-annotation for annotations that introduces a {@link TestAnnotationExtension} for test.
 *
* @author Lior Hasson
*/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
@Documented
@interface RuleAnnotation {
    Class<? extends TestAnnotationExtension> value()
}
