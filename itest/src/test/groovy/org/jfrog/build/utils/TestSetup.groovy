package org.jfrog.build.utils

import org.jfrog.artifactory.client.ArtifactoryClient
import org.jfrog.build.launcher.GradleLauncher
import org.jfrog.build.launcher.MavenLauncher

/**
 * @author Aviad Shikloshi
 */
class TestSetup {

    private static TestSetup instance = new TestSetup()
    public def artifactory
    public def buildLauncher
    public def buildProperties
    public def config

    static TestSetup getTestConfig() {
        return instance
    }

    private TestSetup(){
        config = new ConfigSlurper().parse(new File('src/test/resources/build-info-tests/testConfig.groovy').toURI().toURL())
        buildProperties = PropertyFileAggregator.aggregateBuildProperties(config.buildLauncher.propertiesFilesPath)
        createArtifactoryClient()
        createBuildLauncher()
    }

    private void createBuildLauncher(){
        switch (config.buildLauncher.buildTool.toLowerCase()){
            case 'gradle':
                buildLauncher = new GradleLauncher(config.buildLauncher.commandPath, config.buildLauncher.buildScriptPath)
                break
            case 'maven':
                buildLauncher = new MavenLauncher(config.buildLauncher.javaHome, config.buildLauncher.commandPath, config.buildLauncher.buildScriptPath)
            case 'ivy':
                throw new UnsupportedOperationException("ivy is not yet supported")
            default:
                throw new IllegalArgumentException("Build tool is invalid.")
        }
        config.buildLauncher.tasks.each {
            buildLauncher.addTask(it)
        }
        config.buildLauncher.switches.each {
            buildLauncher.addSwitch(it)
        }
        // TODO: add environment, system properties etc.
        def finalFilePath = "src/test/resources/build-info-tests/finalBuildProperties.properties"
        PropertyFileAggregator.toFile(buildProperties, finalFilePath)
        buildLauncher.addSystemProp("buildInfoConfig.propertiesFile", finalFilePath)
        //TODO: delete new file
    }

    private void createArtifactoryClient(){
        artifactory = ArtifactoryClient.create(config.artifactory.url, config.artifactory.username,
                config.artifactory.password)
    }

}
