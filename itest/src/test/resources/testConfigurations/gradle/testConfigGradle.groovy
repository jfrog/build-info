package testConfigurations.gradle

labels=['buildInfoProperties', 'artifacts', 'licenseControl']


artifacts {
    buildArtifacts{
        mappings=[[input:"(.+).jar"], [input:"(.+)-SNAPSHOT.jar"]]
    }
    expected{
        numberExpected=8
    }
}

buildInfoProperties {
    buildInfo{
        build{
            name="testing-gradle-3"
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
        }
        deploy{
            build{
                name="testing-gradle-3"
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
    buildToolVersions = ["2.1", "2.2", "2.3", "2.4"]
    tasks = ["clean", "artifactoryPublish", "--stacktrace"]
    systemVariables= []
    projVariables = []
    projectPath = ["/projects/gradle/gradle-example"]
    buildScriptFile = "build.gradle"
}