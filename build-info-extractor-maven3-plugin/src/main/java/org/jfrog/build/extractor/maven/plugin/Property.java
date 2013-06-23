package org.jfrog.build.extractor.maven.plugin;

import java.lang.annotation.*;

/**
 * Annotation allowing to specify for mojo fields their corresponding property names.
 */
@Documented
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.FIELD )
@Inherited
public @interface Property
{
    String name()         default "";
    String defaultValue() default "";
}
