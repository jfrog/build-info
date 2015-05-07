package org.jfrog.build.utils

import org.jfrog.artifactory.client.ArtifactoryRequest
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl
import spock.lang.Shared
import spock.lang.Specification

/**
 * @author Aviad Shikloshi
 */
class BuildInfoTest extends Specification {

    @Shared
    private def testSetup = org.jfrog.build.utils.TestSetup.getTestConfig()

    def setupSpec() {
        testSetup = org.jfrog.build.utils.TestSetup.getTestConfig()
        TestUtils.setupArtifactory(testSetup.artifactory, testSetup.buildProperties.getBuildProperty(TestConstants.repoKey))
    }

    private static Map<String, Object> buildInfo

    def "build info test"() {

        when:
        def exitCode = testSetup.buildLauncher.launch()
        testSetup = org.jfrog.build.utils.TestSetup.getTestConfig()
        def buildName = testSetup.buildProperties.getBuildProperty(TestConstants.buildName)
        def buildNumber = testSetup.buildProperties.getBuildProperty(TestConstants.buildNumber)
        buildInfo = TestUtils.getBuildInfo(testSetup.artifactory, buildName, buildNumber)
        then:
        exitCode == 0

        buildInfo != null
        buildInfo.get("buildInfo").get("name").equals(buildName)
        buildInfo.get("buildInfo").get("number").equals(buildNumber)
    }

    def "blackduck test"(){
        expect:
        // blackduck
        def blackDuckProperties = ["runChecks", "includePublishedArtifacts", "autoCreateMissingComponentRequests", "autoDiscardStaleComponentRequests"]
        blackDuckProperties.each {
            def key = "buildInfo.governance.blackduck." + it
            def input = testSetup.buildProperties.getBuildProperty(key)
            def output = TestUtils.getBuildInfoFieldFromProperty(buildInfo, "buildInfo.governance.blackDuckProperties." + it)
            input.equals(output)
        }
    }


    def "license check test"() {
        expect:
        def licenseFields = ['runChecks', 'includePublishedArtifacts', 'autoDiscover']
        licenseFields.each {
            def key = "buildInfo.licenseControl." + it
            def input = testSetup.buildProperties.getBuildProperty(key)
            def output = TestUtils.getBuildInfoFieldFromProperty(buildInfo, key)
            input.equals(output)
        }
    }

    def "publish to artifactory test"() {
        expect:
        ArtifactoryRequest searchArtifacts = new ArtifactoryRequestImpl()
                .apiUrl("api/search/artifact")
                .method(ArtifactoryRequest.Method.GET)
                .responseType(ArtifactoryRequest.ContentType.JSON)
                .addQueryParam("name", artifactName)
                .addQueryParam("repos", currentConf.getBuildProperty(TestConstants.repoKey))
        Map<String, Object> response = testSetup.artifactory.restCall(searchArtifacts)
        response.get("results").size() != 0
        response.get("results").size() != 0
        where:
        artifactName << testSetup.config.artifacts.publishedArtifacts
        currentConf = testSetup.buildProperties
    }

    def "include environment variable test"() {
/*
        when:
        Boolean.valueOf(propertyConfiguration.getProperty(BuildInfoConstants.includeEnvVars))
        def currentBuildInfo = buildInfo
        then:
        Map<String, String> properties = getBuildInfoFieldFromProperty(currentBuildInfo, "buildInfo.properties")
        properties.forEach{ key, value ->
            if (key.startsWith('buildInfo'))
               propertyConfiguration.getProperty(key).equals(value)
        }
*/

    }

    def "artifacts properties deployment test"() {
        expect:
        ArtifactoryRequest propertySearch = new ArtifactoryRequestImpl()
                .apiUrl("api/search/prop")
                .responseType(ArtifactoryRequest.ContentType.JSON)
                .method(ArtifactoryRequest.Method.GET)
                .addQueryParam(key, value)
        Map<String, Object> files = testSetup.artifactory.restCall(propertySearch)
        files.get("results").size() != 0
        where:
        key << testSetup.config.artifacts.propertyKey
        value << testSetup.config.artifacts.propertyValue

    }

    def cleanupSpec() {
        def repoName = testSetup.buildProperties.getBuildProperty(TestConstants.repoKey)
        def buildName = testSetup.buildProperties.getBuildProperty(TestConstants.buildName)
        def buildNumber = testSetup.buildProperties.getBuildProperty(TestConstants.buildNumber)
        TestUtils.deleteBuildFromArtifactory(testSetup.artifactory, buildName, buildNumber)
        TestUtils.deleteRepository(testSetup.artifactory, repoName)
        testSetup.artifactory.close()
    }
}