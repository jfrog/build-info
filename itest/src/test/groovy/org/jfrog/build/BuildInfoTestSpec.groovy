package org.jfrog.build

import org.jfrog.build.utils.TestConstants
import org.jfrog.build.utils.TestUtils

/**
 * @author Aviad Shikloshi
 */
class BuildInfoTestSpec extends BuildInfoTestBase {

    def "build info test"() {
        when:
        testSetup.each {
            it.buildName = it.buildProperties.get(TestConstants.buildName)
            it.buildNumber = it.buildProperties.get(TestConstants.buildNumber)
            it.launch()
            it.buildInfo = TestUtils.getBuildInfo(it.artifactory, it.buildName, it.buildNumber)
        }

        then:
        tmpTestSetup.exitCode == 0
        tmpTestSetup.buildInfo != null
//            assert it.exitCode == 0 : "exitcode fail on build: $it.buildName"
//            assert it.buildInfo != null : "fail on build: $it.buildName"
        where:
        tmpTestSetup << testSetup
    }

//    def "blackduck test"(){
//        expect:
//        // blackduck
//        def blackDuckProperties = ["runChecks", "includePublishedArtifacts", "autoCreateMissingComponentRequests", "autoDiscardStaleComponentRequests"]
//        blackDuckProperties.each {
//            def key = "buildInfo.governance.blackduck." + it
//            def input = testSetup.buildProperties.getBuildProperty(key)
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
//            def input = testSetup.buildProperties.getBuildProperty(key)
//            def output = TestUtils.getBuildInfoFieldFromProperty(buildInfo, key)
//            input.equals(output)
//        }
//    }
//
//    def "publish to artifactory test"() {
//        expect:
//        ArtifactoryRequest searchArtifacts = new ArtifactoryRequestImpl()
//                .apiUrl("api/search/artifact")
//                .method(ArtifactoryRequest.Method.GET)
//                .responseType(ArtifactoryRequest.ContentType.JSON)
//                .addQueryParam("name", artifactName)
//                .addQueryParam("repos", currentConf.getBuildProperty(TestConstants.repoKey))
//        Map<String, Object> response = testSetup.artifactory.restCall(searchArtifacts)
//        response.get("results").size() != 0
//        response.get("results").size() != 0
//        where:
//        artifactName << testSetup.testConfig.artifacts.publishedArtifacts
//        currentConf = testSetup.buildProperties
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
//        Map<String, Object> files = testSetup.artifactory.restCall(propertySearch)
//        files.get("results").size() != 0
//        where:
//        key << testSetup.testConfig.artifacts.propertyKey
//        value << testSetup.testConfig.artifacts.propertyValue
//
//    }
//
    def cleanupSpec() {
        testSetup.each {
            def repoName = it.buildProperties.get(TestConstants.repoKey)
            def buildName = it.buildProperties.get(TestConstants.buildName)
            def buildNumber = it.buildProperties.get(TestConstants.buildNumber)
            TestUtils.deleteBuildFromArtifactory(it.artifactory, buildName, buildNumber)
            TestUtils.deleteRepository(it.artifactory, repoName)
            it.artifactory.close()
        }
    }
}