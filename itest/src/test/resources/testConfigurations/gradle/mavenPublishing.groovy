package testConfigurations.gradle

labels = ['buildInfoProperties', 'artifacts', 'licenseControl']


artifacts {
    deployed{
        mappings=[[input:"(.+).pom"]]
        expected{
            numberExpected = 3
        }
    }
}

buildInfoProperties {
    buildInfo{
        build{
            name="testing-gradle-maven-publish"
            number="1"
        }
        /*agent {
            version="1.607"
        }*/
        governance{
            blackduck{
                runChecks="false"
            }
        }

        licenseControl{
            runChecks="false"
            autoDiscover="false"
            includePublishedArtifacts="false"
            licenseViolationsRecipientsList=""
            scopesList=""
        }
    }
    artifactory{
        publish{
            /*snapshot{
                repoKey = "gradle-local"
            }
            repoKey = "gradle-local"*/

            buildInfo="true"
            artifacts="true"
            ivy="false"
            maven="true"
        }
        deploy{
            build{
                name="testing-gradle-maven-publish"
                number = "1"
            }
        }
    }
    buildInfoConfig{
        includeEnvVars="false"
    }
}

buildLauncher {
    buildTool ="gradle"
    buildToolVersions = ["2.4"]
    tasks = ["clean", "artifactoryPublish", "--stacktrace"]
    systemVariables= []
    projVariables = []
    projectPath = ["/projects/gradle/gradle-example-publish"]
    buildScriptFile = "build.gradle"
}