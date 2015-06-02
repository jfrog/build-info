package build.extensions

import build.BuildTestBaseSpec
import org.spockframework.runtime.extension.AbstractMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation
import utils.TestProfile

/**
 * @author Lior Hasson  
 */
class LabelMatchInterceptor extends AbstractMethodInterceptor {

    String[] labels
    String fieldName

    @Override
    void interceptSpecExecution(IMethodInvocation invocation) throws Throwable {
        //Get the instance of the executed spec
        def currentlyRunningSpec = invocation.sharedInstance

        def result
        if (labels.size() != 0) {
            result = filterTestProfile()
        } else {
            result = BuildTestBaseSpec.testProfiles
        }

        //Override the annotated field with the relevant TestProfile instances
        currentlyRunningSpec.metaClass."${fieldName}" = result
        invocation.proceed()
    }

    private Iterable<TestProfile> filterTestProfile() {
        def result = []
        BuildTestBaseSpec.testProfiles.each { profileTest ->
            for (def annotationLabel : labels) {
                if (profileTest.testConfig.labels != null) {
                    if (((String[]) profileTest.testConfig.labels).contains(annotationLabel)) {
                        result.add(profileTest)
                        break
                    }
                }
            }
        }

    result
    }
}
