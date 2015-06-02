package build.extensions

import org.spockframework.runtime.extension.ExtensionAnnotation

import java.lang.annotation.Documented
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * @author Lior Hasson  
 */

/**
 * <p>
 * Use this extension only on List object
 * Use this extension in order to populate the relevant {@link utils.TestProfile} to your list member
 * in the Spec
 * The population logic is by matching labels from the test config file to the specific Spec
 *
 * example:
 * For the following closure
 * <pre>
 *  labels=['artifacts']
 * </pre>
 *
 * You need to add the following annotation
 * <pre>
 *  @LabelMatch(['artifacts'])
 * </pre>
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
//Specify the extension class that backs this annotation
@ExtensionAnnotation(LabelMatchExtension)
@Documented
public @interface LabelMatch {
    //Accept a string values that represents the supported Closures for a particular Spec
    String[] value() default []
}
