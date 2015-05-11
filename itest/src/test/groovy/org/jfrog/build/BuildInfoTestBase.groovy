package org.jfrog.build

import groovy.io.FileType
import org.jfrog.build.utils.TestConstants
import org.jfrog.build.utils.TestSetup
import org.jfrog.build.utils.TestUtils
import spock.lang.Shared
import spock.lang.Specification

/**
 * @author Lior Hasson  
 */
class BuildInfoTestBase extends Specification{
    @Shared
    protected List<TestSetup> testSetup = new ArrayList<TestSetup>();
    protected static Map<String, Object> buildInfo

    def setupSpec() {

        def dir = new File(getClass().getResource("/org/jfrog/build/testConfigurations").path)
        dir.eachFileRecurse (FileType.FILES) { file ->
            testSetup.add(new TestSetup(file))
        }
        testSetup.each {
            TestUtils.setupArtifactory(it.artifactory, it.buildProperties.get(TestConstants.repoKey))
        }
    }
}
