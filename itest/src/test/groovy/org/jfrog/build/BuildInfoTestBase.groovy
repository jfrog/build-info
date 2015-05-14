package org.jfrog.build

import groovy.io.FileType
import org.jfrog.artifactory.client.Artifactory
import org.jfrog.artifactory.client.ArtifactoryClient
import org.jfrog.build.utils.TestConstants
import org.jfrog.build.utils.TestSetup
import org.jfrog.build.utils.TestUtils
import org.jfrog.build.utils.TestsConfig
import spock.lang.Shared
import spock.lang.Specification

/**
 * @author Lior Hasson
 */
class BuildInfoTestBase extends Specification{
    @Shared
    protected List<TestSetup> testConfigurations = new ArrayList<TestSetup>()
    @Shared
    private def config = TestsConfig.getInstance().config
    @Shared
    protected Artifactory artifactory

    def setupSpec() {
        assert config
        assert config.artifactory.url
        assert config.artifactory.username
        assert config.artifactory.password

        artifactory = createArtifactoryClient()
        def dir = new File(((String) config.testConfigurationsPath))

        dir.eachFileRecurse (FileType.FILES) { file ->
            testConfigurations.add(new TestSetup(file, artifactory))
        }
    }

    def cleanupSpec() {
        testConfigurations.each {
            def repoName = it.buildProperties.get(TestConstants.repoKey)
            def buildName = it.buildProperties.get(TestConstants.buildName)
            def buildNumber = it.buildProperties.get(TestConstants.buildNumber)
            TestUtils.deleteBuildFromArtifactory(it.artifactory, buildName, buildNumber)
            TestUtils.deleteRepository(it.artifactory, repoName)
            it.artifactory.close()
        }
    }

    private def createArtifactoryClient() {
        def artifactory = ArtifactoryClient.create(config.artifactory.url, config.artifactory.username,
                config.artifactory.password)

        artifactory
    }
}
