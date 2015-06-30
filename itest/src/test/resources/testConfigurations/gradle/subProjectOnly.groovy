package testConfigurations.gradle

labels = ['buildInfoProperties', 'artifacts', 'licenseControl']


artifacts {
    deployed {
        mappings=[[input:"(.+).xml"], [input:"(.+).jar"], [input:"(.+).properties"], [input:"(.+).txt"]]
        expected {
            numberExpected = 4
        }
    }
}

buildInfoProperties {
    buildInfo{
        build{
            name = "testing-gradle-sub-project"
            number = "1"
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
            ivy="true"
            maven="false"
        }
        deploy{
            build{
                name="testing-gradle-sub-project"
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
    tasks = ["clean", "artifactoryPublish"]
    systemVariables= []
    projVariables = []
    projectPath = ["/projects/gradle/gradle-sub-project"]
    buildScriptFile = "build.gradle"
}