package org.jfrog.build

import groovy.io.FileType
import org.jfrog.artifactory.client.Artifactory
import org.jfrog.artifactory.client.ArtifactoryClient
import org.jfrog.build.specs.BuildInfoTestSpec
import org.jfrog.build.specs.DeploymentTestSpec
import org.jfrog.build.utils.TestConstants
import org.jfrog.build.utils.TestProfile
import org.jfrog.build.utils.TestProfileBuilder
import org.jfrog.build.utils.TestUtils
import org.jfrog.build.utils.TestsConfig
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * @author Lior Hasson
 */

@RunWith(Suite)
@Suite.SuiteClasses([DeploymentTestSpec.class, BuildInfoTestSpec.class])
class TestBaseSpec {
    private static def config = TestsConfig.getInstance().config
    public static List<TestProfile> testProfiles = new ArrayList<TestProfile>()
    public static Artifactory artifactory

    @BeforeClass
    static void setup() {
        assert config
        assert config.artifactory.url
        assert config.artifactory.username
        assert config.artifactory.password

        artifactory = createArtifactoryClient()
        def dir = new File(((String) config.testConfigurationsPath))

        //Iterates on all the test Config files to create test profile from them
        dir.eachFileRecurse (FileType.FILES) { file ->
            def builder = new TestProfileBuilder(config, file, artifactory)
            testProfiles.addAll(builder
                    .splitByProperty('buildLauncher.buildToolVersions')
                    .splitByProperty('buildLauncher.projectPath')
                    .build()
            )
        }

        //Prepare the test profile information, include run build tool processes
        testProfiles.each {testProfile ->
            testProfile.buildName = testProfile.buildProperties.get(TestConstants.buildName)
            testProfile.buildNumber = testProfile.buildProperties.get(TestConstants.buildNumber)
            testProfile.launch()
            testProfile.buildInfo = TestUtils.getBuildInfo(testProfile.artifactory, testProfile.buildName, testProfile.buildNumber)
        }
    }

    /**
     * Clean all the data(repositories + builds) that the tests created
     */
    @AfterClass
    static void cleanup() {
        testProfiles.each {
            def repoName = it.buildProperties.get(TestConstants.repoKey)
            def buildName = it.buildProperties.get(TestConstants.buildName)
            def buildNumber = it.buildProperties.get(TestConstants.buildNumber)
            TestUtils.deleteBuildFromArtifactory(it.artifactory, buildName, buildNumber)
            TestUtils.deleteRepository(it.artifactory, repoName)
        }
        artifactory.close()
    }

    private static def createArtifactoryClient() {
        def artifactory = ArtifactoryClient.create(config.artifactory.url, config.artifactory.username,
                config.artifactory.password)

        artifactory
    }
}
