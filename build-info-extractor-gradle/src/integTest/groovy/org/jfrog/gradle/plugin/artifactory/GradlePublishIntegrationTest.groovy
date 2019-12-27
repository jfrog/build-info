package org.jfrog.gradle.plugin.artifactory

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.VersionNumber
import org.jfrog.build.api.Build
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientBuilderBase
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder
import org.junit.Rule
import spock.lang.Specification
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class GradlePublishIntegrationTest extends Specification {
    private static VersionNumber GRADLE_6 = VersionNumber.parse('6.0')

    @Rule TestBuildRule testBuild = new TestBuildRule()

    /**
     * Tests that when all projects in a multi-project build have the artifactory plugin applied, all artifacts
     * and build info are published to a local artifactory instance.
     */
    @Unroll
    @UsesTestBuild("all-projects-publish")
    def "can publish artifacts and build info to local artifactory instance when plugin is applied to all projects (Gradle #gradleVersion)"() {
        def version = VersionNumber.parse(gradleVersion)

        expect:
        BuildResult result = succeeds(gradleVersion, "artifactoryPublish")

        and:
        result.task(':services:webservice:artifactoryPublish').outcome == SUCCESS
        result.task(':api:artifactoryPublish').outcome == SUCCESS
        result.task(':shared:artifactoryPublish').outcome == SUCCESS
        result.task(':artifactoryDeploy').outcome == SUCCESS

        and:
        Build buildInfo = new PublishedBuildInfo(result.output).getPublishedInfo()
        buildInfo.modules.size() == 3

        def webservice = buildInfo.getModule("org.jfrog.test.gradle.publish:webservice:1.0-SNAPSHOT")
        webservice.artifacts.size() == (version < GRADLE_6 ? 3 : 4)
        webservice.dependencies.size() == 7

        def api = buildInfo.getModule("org.jfrog.test.gradle.publish:api:1.0-SNAPSHOT")
        api.artifacts.size() == (version < GRADLE_6 ? 5 : 6)
        api.dependencies.size() == 5

        def shared = buildInfo.getModule("org.jfrog.test.gradle.publish:shared:1.0-SNAPSHOT")
        shared.artifacts.size() == (version < GRADLE_6 ? 3 : 4)
        shared.dependencies.size() == 0

        and:
        assertAllArtifactsExist(
                'shared/1.0-SNAPSHOT/shared-1.0-SNAPSHOT.jar',
                'shared/1.0-SNAPSHOT/shared-1.0-SNAPSHOT.properties',
                'shared/1.0-SNAPSHOT/shared-1.0-SNAPSHOT.pom',
                'webservice/1.0-SNAPSHOT/webservice-1.0-SNAPSHOT.jar',
                'webservice/1.0-SNAPSHOT/webservice-1.0-SNAPSHOT.properties',
                'webservice/1.0-SNAPSHOT/webservice-1.0-SNAPSHOT.pom',
                'api/1.0-SNAPSHOT/api-1.0-SNAPSHOT.jar',
                'api/1.0-SNAPSHOT/api-1.0-SNAPSHOT.properties',
                'api/1.0-SNAPSHOT/api-1.0-SNAPSHOT.txt',
                'api/ivy-1.0-SNAPSHOT.xml',
                'api/1.0-SNAPSHOT/api-1.0-SNAPSHOT.pom'
        )

        and:
        if (version >= GRADLE_6) {
            assertAllArtifactsExist(
                    'webservice/1.0-SNAPSHOT/webservice-1.0-SNAPSHOT.module',
                    'api/1.0-SNAPSHOT/api-1.0-SNAPSHOT.module',
                    'shared/1.0-SNAPSHOT/shared-1.0-SNAPSHOT.module'
            )
        }
        true

        where:
        gradleVersion << ['4.10.3', '5.6.4', '6.0.1']
    }

    /**
     * Tests that when only certain projects in a multi-project build have the artifactory plugin applied, the plugin
     * correctly handles the case.  Checks that all artifacts and build info are published to a local artifactory
     * instance.
     */
    @Unroll
    @UsesTestBuild("some-projects-publish")
    def "can publish artifacts and build info to local artifactory instance when plugin is applied to only some projects (Gradle #gradleVersion)"() {
        def version = VersionNumber.parse(gradleVersion)

        expect:
        BuildResult result = succeeds(gradleVersion, "artifactoryPublish")

        and:
        result.task(':services:webservice:artifactoryPublish').outcome == SUCCESS
        result.task(':api:artifactoryPublish').outcome == SUCCESS
        result.task(':artifactoryDeploy').outcome == SUCCESS

        and:
        Build buildInfo = new PublishedBuildInfo(result.output).getPublishedInfo()
        buildInfo.modules.size() == 2

        def webservice = buildInfo.getModule("org.jfrog.test.gradle.publish:webservice:1.0-SNAPSHOT")
        webservice.artifacts.size() == (version < GRADLE_6 ? 3 : 4)
        webservice.dependencies.size() == 6

        def api = buildInfo.getModule("org.jfrog.test.gradle.publish:api:1.0-SNAPSHOT")
        api.artifacts.size() == (version < GRADLE_6 ? 5 : 6)
        api.dependencies.size() == 4

        and:
        assertAllArtifactsExist(
                'webservice/1.0-SNAPSHOT/webservice-1.0-SNAPSHOT.jar',
                'webservice/1.0-SNAPSHOT/webservice-1.0-SNAPSHOT.properties',
                'webservice/1.0-SNAPSHOT/webservice-1.0-SNAPSHOT.pom',
                'api/1.0-SNAPSHOT/api-1.0-SNAPSHOT.jar',
                'api/1.0-SNAPSHOT/api-1.0-SNAPSHOT.properties',
                'api/1.0-SNAPSHOT/api-1.0-SNAPSHOT.txt',
                'api/ivy-1.0-SNAPSHOT.xml',
                'api/1.0-SNAPSHOT/api-1.0-SNAPSHOT.pom'
        )

        and:
        if (version >= GRADLE_6) {
            assertAllArtifactsExist(
                    'webservice/1.0-SNAPSHOT/webservice-1.0-SNAPSHOT.module',
                    'api/1.0-SNAPSHOT/api-1.0-SNAPSHOT.module'
            )
        }
        true

        where:
        gradleVersion << ['4.10.3', '5.6.4', '6.0.1']
    }

    BuildResult succeeds(String gradleVersion, String... tasks) {
        String[] arguments = (['--stacktrace'] + tasks).flatten()
        BuildResult result = GradleRunner.create()
                .withGradleVersion(gradleVersion)
                .withProjectDir(testBuild.root)
                .withPluginClasspath()
                .withArguments(arguments)
                .build()
        println result.output
        return result
    }

    File file(String path) {
        return new File(testBuild.root, path)
    }

    void assertAllArtifactsExist(String... artifactUrls) {
        def client = configureClientBuilder(new ArtifactoryDependenciesClientBuilder()).build()
        artifactUrls.each { artifact ->
            client.getArtifactMetadata("http://127.0.0.1:8081/artifactory/example-repo-local/org/jfrog/test/gradle/publish/${artifact}")
        }
    }

    class PublishedBuildInfo {
        String buildName
        String buildNumber

        PublishedBuildInfo(String output) {
            String buildUrlLine = output.readLines().find { it.startsWith("Build successfully deployed.")}
            if (buildUrlLine == null) {
                throw new Exception("Cannot find build output with build info url.")
            }

            String buildUrl = buildUrlLine.split('\\s+').last()
            String[] buildUrlComponents = buildUrl.split('/')
            this.buildName = buildUrlComponents[-2]
            this.buildNumber = buildUrlComponents[-1]
        }

        Build getPublishedInfo() {
            def client = configureClientBuilder(new ArtifactoryBuildInfoClientBuilder()).build()
            return client.getBuildInfo(buildName, buildNumber)
        }
    }

    private <T extends ArtifactoryClientBuilderBase> ArtifactoryClientBuilderBase<T> configureClientBuilder(ArtifactoryClientBuilderBase<T> builder) {
        return builder.setArtifactoryUrl("http://localhost:8081/artifactory")
                .setUsername("admin")
                .setPassword("password")
    }
}
