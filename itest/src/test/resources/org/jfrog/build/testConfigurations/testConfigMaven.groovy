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
            name="mavenFreeStyleArfifactoryPlugin"
        }
        agent {
            version="1.5"
        }
    }
}

buildLauncher {
    buildTool="maven"
    buildToolVersions="2.3"
    tasks = ["clean", "install"]
    switches = []
    systemProperties= [:]
    m3pluginLib="/org/jfrog/build/cache/artifactory-plugin"
    classworldsConf="/org/jfrog/build/maven/classworlds-freestyle.conf"
    projVariables = []
    javaHome="java"
    buildScriptPath = "/org/jfrog/build/maven/projects/maven-example/pom.xml"
    commandPath=""
    mavenHome="C:\\Software\\apache-maven-3.2.5"
}