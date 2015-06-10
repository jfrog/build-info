package build.generic

import build.TestInputContributor
import groovy.io.FileType
import org.jfrog.build.api.Dependency
import org.jfrog.build.api.dependency.BuildDependency
import org.jfrog.build.client.DeployDetails
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient
import utils.TestConstants
import utils.TestProfile
import utils.TestUtils

/**
 * @author Lior Hasson  
 */

//TODO deployment properties

class Generic implements TestInputContributor{
    private BuildInfoLog buildInfoLog = new BuildInfoLog(Generic.class)
    private TestProfile testProfile
    private List<BuildDependency> buildDependencies
    private List<Dependency> dependencies
    private Set<DeployDetails> artifactsToDeploy
    private def config


    Generic(TestProfile testProfile, config) {
        this.testProfile = testProfile
        this.config = config
    }

    @Override
    def contribute() {
        try {
            if (testProfile.testConfig.generic.prepareData) {
                prepareData()
            }

            if (testProfile.testConfig.generic.resolvePattern) {
                resolve()
            }

            if (testProfile.testConfig.generic.deployPattern) {
                deploy()
            }

            buildInfo()
        }
        catch (Exception e){
            buildInfoLog.error("Generic Build Error: $e.message")
            return -1
        }

        return 0
    }

    private void prepareData() {
        String dataPath = testProfile.testConfig.generic.prepareData.dataPath
        String prepareDataDir = getClass().getResource(testProfile.testConfig.buildLauncher.projectPath).path + "/" + dataPath
        def properties = testProfile.testConfig.generic.prepareData.properties
        def dir = new File(prepareDataDir)

        dir.eachFileRecurse(FileType.FILES) { file ->
            String targetRepository = testProfile.buildProperties.get(TestConstants.repoKey)
            String repositoryPath = file.path[file.path.indexOf(dataPath)..-1].replace("\\", "/")
            TestUtils.uploadArtifacts(testProfile.artifactory, targetRepository, repositoryPath, file, properties)
        }
    }

    private void resolve() {
        ArtifactoryDependenciesClient client = createBuildInfoDependenciesClient()
        GenericArtifactsResolver artifactsResolver = new GenericArtifactsResolver(testProfile.testConfig, testProfile.buildProperties, client)
        buildDependencies = artifactsResolver.retrieveBuildDependencies()
        dependencies = artifactsResolver.retrievePublishedDependencies()
    }

    private void deploy() {
        ArtifactoryBuildInfoClient client = createBuildInfoClient()
        GenericArtifactsDeployer artifactsDeployer = new GenericArtifactsDeployer(testProfile.testConfig, testProfile.buildProperties, client)
        artifactsDeployer.deploy()

        artifactsToDeploy = artifactsDeployer.getArtifactsToDeploy()
    }

    private void buildInfo(){
        ArtifactoryBuildInfoClient client = createBuildInfoClient()
        GenericBuildInfoDeployer buildInfoDeployer = new GenericBuildInfoDeployer(client, buildDependencies, dependencies, artifactsToDeploy)
        buildInfoDeployer.buildInfo(testProfile.buildProperties)
    }

    private ArtifactoryBuildInfoClient createBuildInfoClient() {
        ArtifactoryBuildInfoClient client = new ArtifactoryBuildInfoClient(config.artifactory.url,
                config.artifactory.username, config.artifactory.password, buildInfoLog)
        client
    }

    private ArtifactoryDependenciesClient createBuildInfoDependenciesClient() {
        ArtifactoryDependenciesClient client = new ArtifactoryDependenciesClient(config.artifactory.url,
                config.artifactory.username, config.artifactory.password, buildInfoLog)
        client
    }
}
