package utils

import build.generic.Generic
import build.launcher.GradleLauncher
import build.TestInputContributor
import build.launcher.IvyLauncher
import build.launcher.Launcher
import build.launcher.MavenLauncher
import org.jfrog.artifactory.client.Artifactory
import org.jfrog.build.api.Build

/**
 * This class represents one test profile that will run on all the tests under the spec
 *
 * @author Aviad Shikloshi
 */
class TestProfile {
     ConfigObject testConfig
     String buildPropertiesPath
     Artifactory artifactory
     TestInputContributor buildLauncher
     Properties buildProperties

    /**
    * The actual buildInfo from Artifactory
    * */
    def buildInfo
    def repositories = []
    def buildName
    def buildNumber
    def config
    def exitCode

    TestProfile(ConfigObject testConfig, Artifactory artifactory, def config) {
        this.testConfig = testConfig
        this.artifactory = artifactory
        this.config = config
        buildProperties = PropertyFileAggregator.aggregateBuildProperties(this.testConfig)
    }

    void createBuildLauncher() {
        def projectPath = getClass().getResource(testConfig.buildLauncher.projectPath).path
        def buildScriptPath = "${projectPath}/${testConfig.buildLauncher.buildScriptFile}"

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
                initLauncher(buildLauncher)
                break
            case 'maven':
                buildLauncher = new MavenLauncher(testConfig.buildLauncher.javaHome, testConfig.buildLauncher.mavenHome, buildScriptPath)
                buildLauncher.addSystemProp("buildInfoConfig.propertiesFile", buildPropertiesPath)
                buildLauncher.addSystemProp("m3plugin.lib", getClass().getResource(testConfig.buildLauncher.m3pluginLib).path)
                buildLauncher.addSystemProp("classworlds.conf", getClass().getResource(testConfig.buildLauncher.classworldsConf).path)
                initLauncher(buildLauncher)
                break
            case 'ivy':
                buildLauncher = new IvyLauncher(testConfig.buildLauncher.antPath, buildScriptPath, buildPropertiesPath)
                def artifactoryUrl = "${artifactory.getUri()}/${artifactory.getContextName()}"
                buildLauncher.addSystemProp("artifactory.url", artifactoryUrl)
                initLauncher(buildLauncher)
                break
            case 'generic':
                buildLauncher = new Generic(this, config)
                break
            default:
                throw new IllegalArgumentException("Build tool ${testConfig.buildLauncher.buildTool} is invalid.")
        }


        //TODO: delete new file
    }

    def launch(){
        exitCode = buildLauncher.contribute()
    }

    private void initLauncher(Launcher buildLauncher) {
        testConfig.buildLauncher.buildToolVersions.each {
            buildLauncher.addToolVersions(it)
        }
        testConfig.buildLauncher.tasks.each {
            buildLauncher.addTask(it)
        }
        testConfig.buildLauncher.switches.each {
            buildLauncher.addSwitch(it)
        }
        testConfig.buildLauncher.systemProperties.each { key, val ->
            buildLauncher.addSystemProp(key, val)
        }
    }

    /**
     * Add dynamic properties to the test buildInfo properties, such as unique build name and repositories
     *
     * @param config {@link TestsConfig}
     * @return
     */
    void initSpecialProperties() {

        long timestamp = System.currentTimeMillis()
        String artifactoryUrl = "${artifactory.getUri()}/${artifactory.getContextName()}".toString()
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

    private void createRepo(String propertyKey, newName, long timestamp) {
        String repoName = buildProperties.get(propertyKey)
        if (repoName == null) {
            repoName = newName + timestamp
        }
        TestUtils.createRepository(artifactory, repoName)
        repositories << repoName
        buildProperties.put(propertyKey, repoName)
    }


    @Override
    String toString() {
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
