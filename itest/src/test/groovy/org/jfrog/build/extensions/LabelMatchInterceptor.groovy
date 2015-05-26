package org.jfrog.build.extensions

import org.jfrog.build.TestBaseSpec
import org.jfrog.build.utils.TestProfile
import org.spockframework.runtime.extension.AbstractMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation

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
            result = TestBaseSpec.testProfiles
        }

        //Override the annotated field with the relevant TestProfile instances
        currentlyRunningSpec.metaClass."${fieldName}" = result
        invocation.proceed()
    }

    private Iterable<TestProfile> filterTestProfile() {
        def result = []
        TestBaseSpec.testProfiles.each { profileTest ->
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
