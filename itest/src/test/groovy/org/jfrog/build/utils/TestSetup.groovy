package org.jfrog.build.utils

import org.jfrog.build.launcher.GradleLauncher
import org.jfrog.build.launcher.IvyLauncher
import org.jfrog.build.launcher.MavenLauncher

/**
 * @author Aviad Shikloshi
 */
class TestSetup {

    public ConfigObject testConfig
    public String buildPropertiesPath
    public def artifactory
    public def buildLauncher
    public def buildProperties
    public def exitCode
    public def buildInfo
    public def buildName
    public def buildNumber
    public def repositories

    TestSetup(File testConfigFile, def artifactory) {
        repositories = []
        this.testConfig = new ConfigSlurper().parse(testConfigFile.toURI().toURL())
        buildProperties = PropertyFileAggregator.aggregateBuildProperties(this.testConfig)
        this.artifactory = artifactory
        initSpecialProperties()
        buildPropertiesPath = PropertyFileAggregator.toFile(buildProperties)
        createBuildLauncher()
    }

    def initSpecialProperties() {

        def timestamp = System.currentTimeMillis()
        def artifactoryUrl = artifactory.getUri() + "/" + artifactory.getContextName()

        createRepositories(buildProperties.get(TestConstants.buildName), timestamp)

        buildProperties.put(TestConstants.artifactoryUrl, artifactoryUrl)
        buildProperties.put(TestConstants.artifactoryResolveUrl, artifactoryUrl)
        buildProperties.put(TestConstants.artifactoryPublishUsername, artifactory.getUsername())
        buildProperties.put(TestConstants.artifactoryPublishPassword, "password")

        buildProperties.put(TestConstants.artifactoryResolveUsername, artifactory.getUsername())
        buildProperties.put(TestConstants.artifactoryResolvePassword, "password")

        buildProperties.put(TestConstants.timestamp, timestamp.toString())
        //buildProperties.put(TestConstants.started, (new Date()).toString());

    }

    private void createRepositories(buildName, long timestamp) {
        createRepo(TestConstants.repoKey, buildName + "_", timestamp)
        createRepo(TestConstants.snapshotRepoKey, buildName + "_snapshot_", timestamp)

        //TODO
        buildProperties.put(TestConstants.resolveSnapshotKey, "remote-repos")
        buildProperties.put(TestConstants.resolveRepokey, "remote-repos")
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

    private void createBuildLauncher() {
        def buildScriptPath = getClass().getResource(testConfig.buildLauncher.buildScriptPath).path;
        switch (testConfig.buildLauncher.buildTool.toLowerCase()) {
            case 'gradle':
                def commandPath = getClass().getResource(testConfig.buildLauncher.commandPath).path;
                buildLauncher = new GradleLauncher(commandPath, buildScriptPath)
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
                buildLauncher = new IvyLauncher(testConfig.buildLauncher.commandPath, buildScriptPath, buildPropertiesPath)
                break
            default:
                throw new IllegalArgumentException("Build tool is invalid.")
        }

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
        exitCode = buildLauncher.launch()
    }

}
