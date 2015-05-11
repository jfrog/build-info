package org.jfrog.build.utils

import org.jfrog.artifactory.client.ArtifactoryClient
import org.jfrog.build.launcher.GradleLauncher
import org.jfrog.build.launcher.IvyLauncher
import org.jfrog.build.launcher.MavenLauncher

/**
 * @author Aviad Shikloshi
 */
class TestSetup {

    public def artifactory
    public def buildLauncher
    public def buildProperties
    public ConfigObject testConfig
    public String buildPropertiesPath
    public def exitCode;
    public def buildInfo;
    public def buildName;
    public def buildNumber;

    TestSetup(File testConfigFile){
        this.testConfig = new ConfigSlurper().parse(testConfigFile.toURI().toURL())
        buildProperties = PropertyFileAggregator.aggregateBuildProperties(this.testConfig)
        buildPropertiesPath = PropertyFileAggregator.toFile(buildProperties)
        createArtifactoryClient()
        createBuildLauncher()
    }

    TestSetup(String testConfigPath){
        TestSetup(new File(testConfigPath))
    }

    private void createBuildLauncher(){
        switch (testConfig.buildLauncher.buildTool.toLowerCase()){
            case 'gradle':
                def buildScriptPath = getClass().getResource(testConfig.buildLauncher.buildScriptPath).path;
                def commandPath = getClass().getResource(testConfig.buildLauncher.commandPath).path;
                buildLauncher = new GradleLauncher(commandPath, buildScriptPath)
                buildLauncher.addProjProp("buildInfoConfig.propertiesFile", buildPropertiesPath)
                testConfig.buildLauncher.systemProperties.each { key, val->
                    ((GradleLauncher)buildLauncher).addProjProp(key, val)
                }
                break
            case 'maven':
                def buildScriptPath = getClass().getResource(testConfig.buildLauncher.buildScriptPath).path;
                buildLauncher = new MavenLauncher(testConfig.buildLauncher.javaHome, testConfig.buildLauncher.mavenHome, buildScriptPath)
                buildLauncher.addSystemProp("buildInfoConfig.propertiesFile", buildPropertiesPath)
                buildLauncher.addSystemProp("m3plugin.lib", getClass().getResource(testConfig.buildLauncher.m3pluginLib).path)
                buildLauncher.addSystemProp("classworlds.conf", getClass().getResource(testConfig.buildLauncher.classworldsConf).path)

                break
            case 'ivy':
                buildLauncher = new IvyLauncher(testConfig.buildLauncher.commandPath, testConfig.buildLauncher.buildScriptPath, testConfig.buildLauncher.propertiesFilesPath[0])
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

        //TODO: add environment, system properties etc.
        //TODO: delete new file
    }

    private void createArtifactoryClient(){
        artifactory = ArtifactoryClient.create(testConfig.artifactory.url, testConfig.artifactory.username,
                testConfig.artifactory.password)
    }

    def launch(){
        exitCode = buildLauncher.launch()
    }

}
