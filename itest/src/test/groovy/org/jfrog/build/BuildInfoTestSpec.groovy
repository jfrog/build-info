package org.jfrog.build

import org.jfrog.artifactory.client.ArtifactoryRequest
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl
import org.jfrog.build.utils.TestConstants
import org.jfrog.build.utils.TestUtils

/**
 * @author Aviad Shikloshi
 */
class BuildInfoTestSpec extends BuildInfoTestBase {

    def "build info test"() {
        setup:
        testConfig.buildName = testConfig.buildProperties.get(TestConstants.buildName)
        testConfig.buildNumber = testConfig.buildProperties.get(TestConstants.buildNumber)
        testConfig.launch()
        testConfig.buildInfo = TestUtils.getBuildInfo(testConfig.artifactory, testConfig.buildName, testConfig.buildNumber)

        expect:
        testConfig.exitCode == 0
        testConfig.buildInfo != null

        where:
        testConfig << testConfigurations
    }

//    def "blackduck test"(){
//        expect:
//        // blackduck
//        def blackDuckProperties = ["runChecks", "includePublishedArtifacts", "autoCreateMissingComponentRequests", "autoDiscardStaleComponentRequests"]
//        blackDuckProperties.each {
//            def key = "buildInfo.governance.blackduck." + it
//            def input = testConfigurations.buildProperties.getBuildProperty(key)
//            def output = TestUtils.getBuildInfoFieldFromProperty(buildInfo, "buildInfo.governance.blackDuckProperties." + it)
//            input.equals(output)
//        }
//    }
//
//    def "license check test"() {
//        expect:
//        def licenseFields = ['runChecks', 'includePublishedArtifacts', 'autoDiscover']
//        licenseFields.each {
//            def key = "buildInfo.licenseControl." + it
//            def input = testConfigurations.buildProperties.getBuildProperty(key)
//            def output = TestUtils.getBuildInfoFieldFromProperty(buildInfo, key)
//            input.equals(output)
//        }
//    }
//
//    def "publish to artifactory test"() {
//
//        setup:
//        Map<String,Object> body = testConfigurations.get(0).testConfig.artifacts.mappings
//
//        expect:
//        ArtifactoryRequest searchArtifacts = new ArtifactoryRequestImpl()
//                .apiUrl("api/search/artifact")
//                .method(ArtifactoryRequest.Method.POST)
//                .requestType(ArtifactoryRequest.ContentType.JSON)
//                .responseType(ArtifactoryRequest.ContentType.JSON)
//                .requestBody()
////                .addQueryParam("name", artifactName)
////                .addQueryParam("repos", currentConf.get(TestConstants.repoKey))
//
////        artifactory.repositories()searches().artifactsByName(artifactName).repositories()
//
//        Map<String, Object> response = artifactory.restCall(searchArtifacts)
//        response.get("results").size() != 0
//
//        where:
//        testConfig << testConfigurations
//
////        where:
////        artifactName << testConfigurations.testConfig.artifacts.publishedArtifacts
////        currentConf = testConfigurations.buildProperties
////        respositories = testConfigurations.buildProperties.
//    }
//
//    def "include environment variable test"() {
///*
//        when:
//        Boolean.valueOf(propertyConfiguration.getProperty(BuildInfoConstants.includeEnvVars))
//        def currentBuildInfo = buildInfo
//        then:
//        Map<String, String> properties = getBuildInfoFieldFromProperty(currentBuildInfo, "buildInfo.properties")
//        properties.forEach{ key, value ->
//            if (key.startsWith('buildInfo'))
//               propertyConfiguration.getProperty(key).equals(value)
//        }
//*/
//
//    }
//
//    def "artifacts properties deployment test"() {
//        expect:
//        ArtifactoryRequest propertySearch = new ArtifactoryRequestImpl()
//                .apiUrl("api/search/prop")
//                .responseType(ArtifactoryRequest.ContentType.JSON)
//                .method(ArtifactoryRequest.Method.GET)
//                .addQueryParam(key, value)
//        Map<String, Object> files = testConfigurations.artifactory.restCall(propertySearch)
//        files.get("results").size() != 0
//        where:
//        key << testConfigurations.testConfig.artifacts.propertyKey
//        value << testConfigurations.testConfig.artifacts.propertyValue
//
//    }
//

}