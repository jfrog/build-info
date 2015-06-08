package build.specs

import build.extensions.LabelMatch
import spock.lang.Shared
import spock.lang.Specification
import utils.TestProfile

/**
 * @author Lior Hasson  
 */
class LicensesTestSpec extends Specification{
    @Shared
    @LabelMatch(['governance', 'licenseControl'])
    List<TestProfile> licensesTestProfiles;

    def "blackduck build info values"(){
        when:
        Map blackduck = testProfile.testConfig.get("buildInfoProperties").get("buildInfo").get("governance").get("blackduck")
        blackduck != null

        then:
        testProfile.buildInfo.get("buildInfo").get("governance").get("blackDuckProperties").each{
            def input = it.value
            def expected = blackduck.get(it.key)
            input.equals(expected)
        }

        where:
        testProfile << licensesTestProfiles
    }

    def "license control build info values"() {
        when:
        Map licenseControl = testProfile.testConfig.get("buildInfoProperties").get("buildInfo").get("licenseControl")
        licenseControl != null

        then:
        testProfile.buildInfo.get("buildInfo").get("licenseControl").each{
            def input = it.value
            def expected = licenseControl.get(it.key)
            input.equals(expected)
        }

        where:
        testProfile << licensesTestProfiles
    }
}
