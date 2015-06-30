package build.specs

import build.BuildTestBaseSpec
import build.extensions.LabelMatch
import spock.lang.Shared
import spock.lang.Specification
import utils.TestConstants
import utils.TestProfile
import utils.TestUtils

/**
 * @author Lior Hasson  
 */
class DeploymentTestSpec extends Specification {

    @Shared
    @LabelMatch(['artifacts'])
    List<TestProfile> deploymentTestProfiles;

    def "number of publish artifacts"() {
        when:
        Map<String, Object> response = TestUtils.getBuildArtifacts(BuildTestBaseSpec.artifactory, testProfile)

        then:
        response != null
        response.get("results").size() == testProfile.testConfig.artifacts.deployed.expected.numberExpected

        where:
        testProfile << deploymentTestProfiles
    }

    def "properties attachment on publish artifacts"() {
        when:
        Map attachedProperties = testProfile.testConfig.get("artifacts").get("attachedProperties")
        if(!attachedProperties)
            return

        Map propertiesResult = getPropertiesResult(testProfile)

        then:
        attachedProperties.each { key, val ->
            assert propertiesResult.get(key)
            assert val.numberExpected == propertiesResult.get(key)
        }

        where:
        testProfile << deploymentTestProfiles
    }

    private Map getPropertiesResult(testProfile){
        def result = [:]
        Map attachedProperties = testProfile.testConfig.get("artifacts").get("attachedProperties")

        def repos = [
                testProfile.buildProperties.get(TestConstants.repoKey),
                testProfile.buildProperties.get(TestConstants.snapshotRepoKey)
        ]

        attachedProperties.each { key, val ->
            def value = "*"
            if(val.value){
                value = val.value
            }
            def properties = ["$key":value]
            Map<String, Object> response = TestUtils.propertySearch(BuildTestBaseSpec.artifactory, properties, repos)
            result.put key , response.get("results").size()
        }

        result
    }
}
