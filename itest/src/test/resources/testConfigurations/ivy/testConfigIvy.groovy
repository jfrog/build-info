package testConfigurations.ivy

artifacts {
    buildArtifacts{
        mappings=[[input:"(.+).jar"], [input:"(.+)-SNAPSHOT.jar"]]
    }
    expected{
        numberExpected=8
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

            //buildInfo.build.timestamp !!!!!!
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
    buildScriptPath = "/org/jfrog/build/ivy/projects/ivy-example/build.xml"
    buildInfoClassPath="/org/jfrog/build/cache/artifactory-plugin"
    antPath = "ant.bat"
}