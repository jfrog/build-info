package build.extensions

import org.spockframework.runtime.extension.ExtensionAnnotation

import java.lang.annotation.*

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
 * For the following labels under the test config file:
 * <pre>
 *  labels=['artifacts']
 * </pre>
 *
 * You need to add the following annotation:
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
@interface LabelMatch {
    //Accept a string values that represents the supported Closures for a particular Spec
    String[] value() default []
}
