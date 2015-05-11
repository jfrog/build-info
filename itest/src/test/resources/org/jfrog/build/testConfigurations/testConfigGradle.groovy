package org.jfrog.build.testConfigurations

artifacts {
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
        }
        deploy{
            build{
                number = "10"
            }
        }
    }
}


artifactory {
    url="http://localhost:8080/artifactory"
    username="admin"
    password="password"
}

buildLauncher {
    buildTool="gradle"
    buildToolVersion="2.3"
    tasks = ["clean", "artifactoryPublish"]
    systemVariables= []
    projVariables = []
    buildScriptPath = "/org/jfrog/build/gradle/projects/gradle-example/build.gradle"
    commandPath="/org/jfrog/build/gradle/projects/gradle-example/gradlew.bat"
}