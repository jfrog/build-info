package build.generic

import org.jfrog.build.api.Dependency
import org.jfrog.build.api.dependency.BuildDependency
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient
import org.jfrog.build.extractor.clientConfiguration.util.BuildDependenciesHelper
import org.jfrog.build.extractor.clientConfiguration.util.DependenciesDownloader
import org.jfrog.build.extractor.clientConfiguration.util.DependenciesHelper
import utils.TestConstants

/**
 * @author Lior Hasson  
 */
class GenericArtifactsResolver {
    private final ArtifactoryDependenciesClient client
    private BuildInfoLog buildInfoLog = new BuildInfoLog(GenericArtifactsResolver.class)
    private ConfigObject testConfig
    private def buildProperties

    GenericArtifactsResolver(ConfigObject testConfig, buildProperties, ArtifactoryDependenciesClient client) {
        this.testConfig = testConfig
        this.buildProperties = buildProperties
        this.client = client
    }

    public List<Dependency> retrievePublishedDependencies() throws IOException, InterruptedException {
        DependenciesHelper helper = new DependenciesHelper(createDependenciesDownloader(), buildInfoLog)
        String resolvePattern = formatResolvePattern()
        return helper.retrievePublishedDependencies(resolvePattern)
    }

    public List<BuildDependency> retrieveBuildDependencies() throws IOException, InterruptedException {
        BuildDependenciesHelper helper = new BuildDependenciesHelper(createDependenciesDownloader(), buildInfoLog)
        String resolvePattern = formatResolvePattern()
        return helper.retrieveBuildDependencies(resolvePattern)
    }

    private DependenciesDownloader createDependenciesDownloader() {
        String workspace = getClass().getResource(testConfig.buildLauncher.projectPath).path
        return new DependenciesDownloaderImpl(client, workspace, buildInfoLog)
    }

    private String formatResolvePattern() {
        String sourceRepo = buildProperties.get(TestConstants.repoKey)
        String resolvePattern = testConfig.generic.resolvePattern.join("\n")
        resolvePattern = resolvePattern.replace('${sourceRepo}', sourceRepo)
        resolvePattern
    }
}
