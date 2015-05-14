package org.jfrog.build.testConfigurations

artifacts {
}

buildInfoProperties {
    buildInfo {
        build {
            name = "ivyBuild"
        }
        agent {
            version = "1.5"
        }
    }
}

buildLauncher {
    buildTool = "ivy"
    buildToolVersions = "2.4.0"
    tasks = ["publish-ci"]
    switches = []
    systemProperties = [:]
    projVariables = []
    javaHome = "java"
    buildScriptPath = "/org/jfrog/build/ivy/projects/ivy-example/build.xml"
    commandPath = "<Path to ant.bat>"
}