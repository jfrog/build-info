package testConfigurations.maven

labels=['buildInfoProperties', 'artifacts']

artifacts {
    buildArtifacts{
        mappings=[[input:"(.+).pom"]]
    }
    expected{
        numberExpected=4
    }
}

buildInfoProperties {
    buildInfo{
        build{
            name="maven-example"
            number="1"
        }
        /*agent {
            version="1.5"
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
                name="maven-example"
                number = "1"
            }
        }
    }
}

buildLauncher {
    buildTool="maven"
    buildToolVersions = []
    tasks = ["clean", "install"]
    switches = []
    systemProperties= [:]
    m3pluginLib="/cache/artifactory-plugin"
    classworldsConf="/projects/maven/classworlds-freestyle.conf"
    projVariables = []
    javaHome="java"
    projectPath = ["/projects/maven/maven-example"]
    buildScriptFile = "pom.xml"

    mavenHome=["C:\\Software\\apache-maven-3.3.3"]
}