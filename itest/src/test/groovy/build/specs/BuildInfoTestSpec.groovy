package build.specs

import build.extensions.LabelMatch
import spock.lang.Shared
import spock.lang.Specification
import utils.TestProfile

//import spock.lang.Unroll

/**
 * @author Aviad Shikloshi
 */

class BuildInfoTestSpec extends Specification {

    @Shared
    @LabelMatch(['buildInfoProperties'])
    List<TestProfile> buildInfoTestProfiles;

    def "process executed good and build info exists"() {
        expect:
        testProfile.exitCode == 0
        testProfile.buildInfo != null

        where:
        testProfile << buildInfoTestProfiles
    }

    def "include environment variable build info values"() {
        //TODO test for exclude and include

        when:
        Map envVars = testProfile.testConfig.get("buildInfoProperties").get("buildInfoConfig")

        then:
        def input = testProfile.buildInfo.get("buildInfo").get("properties")
        def expected = envVars

        (expected == null && input == null) ||
        (Boolean.valueOf(expected.get("includeEnvVars")) || input != null) ||
        (!Boolean.valueOf(expected.get("includeEnvVars")) || input == null)

        where:
        testProfile << buildInfoTestProfiles
    }
//
//    def "artifacts properties deployment test"() {
//        expect:
//        ArtifactoryRequest propertySearch = new ArtifactoryRequestImpl()
//                .apiUrl("api/search/prop")
//                .responseType(ArtifactoryRequest.ContentType.JSON)
//                .method(ArtifactoryRequest.Method.GET)
//                .addQueryParam(key, value)
//        Map<String, Object> files = testProfiles.artifactory.restCall(propertySearch)
//        files.get("results").size() != 0
//        where:
//        key << testProfiles.testProfile.artifacts.propertyKey
//        value << testProfiles.testProfile.artifacts.propertyValue
//
//    }
//
}