package org.jfrog.build.testConfigurations.maven


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
                name="maven-example"
                number = "1"
            }
        }
    }
}

buildLauncher {
    buildTool="maven"
    buildToolVersions = ["3.2.5"]
    tasks = ["clean", "install"]
    switches = []
    systemProperties= [:]
    m3pluginLib="/org/jfrog/build/cache/artifactory-plugin"
    classworldsConf="/org/jfrog/build/maven/classworlds-freestyle.conf"
    projVariables = []
    javaHome="java"
    //buildScriptPath = "/org/jfrog/build/maven/projects/maven-example/pom.xml"
    //commandPath=""
    projectPath = ["/org/jfrog/build/maven/projects/maven-example"]
    buildScriptFile = "pom.xml"

    mavenHome="C:\\Software\\apache-maven-3.2.5"
}