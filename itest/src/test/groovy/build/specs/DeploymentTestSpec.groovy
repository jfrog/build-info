package build.specs

import build.BuildTestBaseSpec
import build.extensions.LabelMatch
import spock.lang.Shared
import spock.lang.Specification
import utils.TestProfile
import utils.TestUtils

/**
 * @author Lior Hasson  
 */
class DeploymentTestSpec extends Specification {

    @Shared
    @LabelMatch(['artifacts'])
    List<TestProfile> deploymentTestProfiles;

    def "publish artifacts test"() {
        when:
        Map<String, Object> response = TestUtils.getBuildArtifacts(BuildTestBaseSpec.artifactory, testProfile)

        then:
        response != null
        response.get("results").size() == testProfile.testConfig.artifacts.expected.numberExpected

        where:
        testProfile << deploymentTestProfiles
    }
}
