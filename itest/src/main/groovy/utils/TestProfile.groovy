package utils

import build.launcher.GradleLauncher
import build.launcher.InputContributor
import build.launcher.IvyLauncher
import build.launcher.MavenLauncher
import org.jfrog.build.api.Build

/**
 * This class represents one test profile that will run on all the tests under the spec
 *
 * @author Aviad Shikloshi
 */
class TestProfile {
    public ConfigObject testConfig
    public String buildPropertiesPath
    public def artifactory
    public InputContributor buildLauncher
    public def buildProperties
    public def exitCode
    public def buildInfo
    public def buildName
    public def buildNumber
    public def repositories = []

    TestProfile(ConfigObject testConfig, def artifactory) {
        this.testConfig = testConfig
        this.artifactory = artifactory
        buildProperties = PropertyFileAggregator.aggregateBuildProperties(this.testConfig)
    }

    public void createBuildLauncher() {
        def projectPath = getClass().getResource(testConfig.buildLauncher.projectPath).path
        def buildScriptPath = projectPath + "/" + testConfig.buildLauncher.buildScriptFile

        switch (testConfig.buildLauncher.buildTool.toLowerCase()) {
            case 'gradle':
                //def commandPath = getClass().getResource(testConfig.buildLauncher.commandPath).path
                projectPath = projectPath + "/" + TestUtils.gradleWrapperScript()
                buildLauncher = new GradleLauncher(projectPath, buildScriptPath)
                buildLauncher.workingDirectory = new File(getClass().getResource(testConfig.buildLauncher.projectPath).path)
                buildLauncher.addProjProp("buildInfoConfig.propertiesFile", buildPropertiesPath)
                    testConfig.buildLauncher.systemProperties.each { key, val->
                    ((GradleLauncher)buildLauncher).addProjProp(key, val)
                }
                break
            case 'maven':
                buildLauncher = new MavenLauncher(testConfig.buildLauncher.javaHome, testConfig.buildLauncher.mavenHome, buildScriptPath)
                buildLauncher.addSystemProp("buildInfoConfig.propertiesFile", buildPropertiesPath)
                buildLauncher.addSystemProp("m3plugin.lib", getClass().getResource(testConfig.buildLauncher.m3pluginLib).path)
                buildLauncher.addSystemProp("classworlds.conf", getClass().getResource(testConfig.buildLauncher.classworldsConf).path)
                break
            case 'ivy':
                buildLauncher = new IvyLauncher(testConfig.buildLauncher.antPath, buildScriptPath, buildPropertiesPath)
                break
            default:
                throw new IllegalArgumentException("Build tool ${testConfig.buildLauncher.buildTool} is invalid.")
        }

        buildLauncher.addToolVersions(testConfig.buildLauncher.buildToolVersions)
        testConfig.buildLauncher.tasks.each {
            buildLauncher.addTask(it)
        }
        testConfig.buildLauncher.switches.each {
            buildLauncher.addSwitch(it)
        }
        testConfig.buildLauncher.systemProperties.each { key, val->
            buildLauncher.addSystemProp(key, val)
        }

        //TODO: delete new file
    }

    def launch(){
        exitCode = buildLauncher.contribute()
    }

    /**
     * Add dynamic properties to the test buildInfo properties, such as unique build name and repositories
     *
     * @param config {@link TestsConfig}
     * @return
     */
    def initSpecialProperties(def config) {

        def timestamp = System.currentTimeMillis()
        def artifactoryUrl = artifactory.getUri() + "/" + artifactory.getContextName()
        createRepositories(buildProperties.get(TestConstants.buildName), timestamp)

        buildProperties.put(TestConstants.buildName, buildProperties.get(TestConstants.buildName) + "_" + timestamp)
        buildProperties.put(TestConstants.artifactoryDeployBuildName, buildProperties.get(TestConstants.artifactoryDeployBuildName) + "_" + timestamp)

        buildProperties.put(TestConstants.artifactoryUrl, artifactoryUrl)
        buildProperties.put(TestConstants.artifactoryResolveUrl, artifactoryUrl)
        buildProperties.put(TestConstants.artifactoryPublishUsername, config.artifactory.username)
        buildProperties.put(TestConstants.artifactoryPublishPassword, config.artifactory.password)

        buildProperties.put(TestConstants.artifactoryResolveUsername, config.artifactory.username)
        buildProperties.put(TestConstants.artifactoryResolvePassword, config.artifactory.username)

        buildProperties.put(TestConstants.buildTimestamp, timestamp.toString())
        buildProperties.put(TestConstants.deployTimestamp, timestamp.toString())

        buildProperties.put(TestConstants.started, Build.formatBuildStarted(timestamp));

        buildPropertiesPath = PropertyFileAggregator.toFile(buildProperties)
    }

    ConfigObject getTestConfig() {
        return testConfig
    }

    private void createRepositories(buildName, long timestamp) {
        createRepo(TestConstants.repoKey, buildName + "_", timestamp)
        createRepo(TestConstants.snapshotRepoKey, buildName + "_snapshot_", timestamp)

        buildProperties.put(TestConstants.resolveSnapshotKey, "remote-repos")
        buildProperties.put(TestConstants.resolveRepokey, "remote-repos")

        //TODO
        //createRepo(TestConstants.resolveSnapshotKey, buildName + "_resolve_snapshot_", timestamp)
        //createRepo(TestConstants.resolveRepokey, buildName + "_resolve_", timestamp)
    }

    private void createRepo(String propertyKey, String newName, long timestamp) {
        def repoName = buildProperties.get(propertyKey)
        if (repoName == null) {
            repoName = newName + timestamp
        }
        TestUtils.createRepository(artifactory, repoName)
        repositories << repoName
        buildProperties.put(propertyKey, repoName)
    }


    @Override
    public String toString() {
        return "TestProfile{" +
                "testConfig=" + testConfig +
                ", buildPropertiesPath='" + buildPropertiesPath + '\'' +
                ", exitCode=" + exitCode +
                ", buildInfo=" + buildInfo +
                ", buildName=" + buildName +
                ", buildNumber=" + buildNumber +
                '}';
    }
}
