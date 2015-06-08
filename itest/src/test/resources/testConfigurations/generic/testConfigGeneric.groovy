package testConfigurations.generic

labels=['buildInfoProperties', 'artifacts']


artifacts {
    buildArtifacts{
        mappings = [[input:"(.+).sh"]]
    }
    expected{
        numberExpected = 2
    }
}

generic{

    //Prepare files before the generic starts, in order to test the resolve
    prepareData{
        dataPath = "prepare-data"
        properties = ['build.itest':'generic']
    }

    //Generic resolve patterns (without source repository, that is calculated on run time)
    resolvePattern = ['${sourceRepo}:prepare-data/org/jfrog/test/**/*.jar;build.itest=generic']
    deployPattern = ['**/*.sh', '**/*.properties=>gradle']
    deploymentProperties = ""
}

buildInfoProperties {
    buildInfo{
        build{
            name="testing-generic"
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
                name="testing-generic"
                number = "1"
            }
        }
    }
    buildInfoConfig{
        includeEnvVars="false"
    }
}

buildLauncher {
    buildTool = "generic"
    buildToolVersions = ["1.0"]
    systemVariables = []
    projVariables = []
    projectPath = ["/projects/generic/generic-example"]
}