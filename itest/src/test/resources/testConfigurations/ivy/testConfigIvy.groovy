package testConfigurations.ivy

labels=['buildInfoProperties', 'artifacts']

artifacts {
    buildArtifacts{
        mappings=[[input:"(.+).jar"], [input:"(.+).xml"]]
    }
    expected{
        numberExpected=12
    }
}

buildInfoProperties {
    buildInfo {
        build {
            name = "ivyBuild"
            number = "1"
        }
        /*agent {
            version="1.607"
        }*/
    }
    artifactory{
        publish{
            /*snapshot{
                repoKey = "gradle-local"
            }
            repoKey = "gradle-local"*/

            buildInfo="true"
            artifacts="true"
        }
        deploy{
            build{
                name="ivyBuild"
                number = "1"
            }
        }
    }
}

buildLauncher {
    buildTool = "ivy"
    buildToolVersions = ["2.4.0"]
    tasks = ["publish-ci"]
    switches = []
    systemProperties = [:]
    projVariables = []
    javaHome = "java"
    projectPath = ["/projects/ivy/ivy-example"]
    buildInfoClassPath="/cache/artifactory-plugin"
    antPath = "ant.bat"
    buildScriptFile = "build.xml"
}