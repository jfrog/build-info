package testConfigurations.gradle

labels = ['buildInfoProperties', 'artifacts', 'licenseControl']


artifacts {
    deployed {
        mappings=[[input:"(.+).xml"]]
        expected {
            numberExpected = 1
        }
    }
    attachedProperties {
        key1 {
            numberExpected = 3
        }
        key3 {
            numberExpected = 2
        }
        "qa.level" {
            value = "basic"
            numberExpected = 8
        }
    }
}

buildInfoProperties {
    buildInfo{
        build{
            name = "testing-gradle-ivy-publish"
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
                name="testing-gradle-ivy-publish"
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
    projectPath = ["/projects/gradle/gradle-example-publish"]
    buildScriptFile = "build.gradle"
}