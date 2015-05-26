package org.jfrog.build.specs

import org.jfrog.build.TestBaseSpec
import org.jfrog.build.extensions.LabelMatch
import org.jfrog.build.utils.TestProfile
import org.jfrog.build.utils.TestUtils
import spock.lang.Shared
import spock.lang.Specification

/**
 * @author Lior Hasson  
 */


class DeploymentTestSpec extends Specification{

    @Shared
    @LabelMatch(['artifacts'])
    List<TestProfile> deploymentTestProfiles;

    def "publish artifacts test"() {
        when:
        Map<String, Object> response = TestUtils.getBuildArtifacts(TestBaseSpec.artifactory, testProfile)

        then:
        response != null
        response.get("results").size() == testProfile.testConfig.artifacts.expected.numberExpected

        where:
        testProfile << deploymentTestProfiles
    }
}
