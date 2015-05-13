package org.jfrog.build.testConfigurations

artifacts {
    mappings{
        input=["(.+).jar"]
    }

    artifactNames=["", ""]
    archiveType= "zip"
    publishedArtifacts = ["api", "shared", "webservice"]
    propertyKey = ["prop1", "prop2"]
    propertyValue = ["1", "2"]
}

buildInfoProperties {
    buildInfo{
        build{
            name="testing-gradle-3"
            number="10"
            //started="" !!!!!
        }
        agent {
            version="1.607"
        }
    }
    artifactory{
        publish{
            snapshot{
                repoKey = "gradle-local"
            }
            repoKey = "gradle-local"

            //buildInfo.build.timestamp !!!!!!
        }
        deploy{
            build{
                number = "10"
            }
        }
    }
}

buildLauncher {
    buildTool="gradle"
    buildToolVersion="2.3"
    tasks = ["clean", "artifactoryPublish", "--stacktrace"]
    systemVariables= []
    projVariables = []
    buildScriptPath = "/org/jfrog/build/gradle/projects/gradle-example/build.gradle"
    commandPath="/org/jfrog/build/gradle/projects/gradle-example/gradlew.bat"
    workingDir="/org/jfrog/build/gradle/projects/gradle-example"
}