artifacts {
    artifactNames=["", ""]
    archiveType= "zip"
    publishedArtifacts = ["api", "shared", "webservice"]
    propertyKey = ["prop1", "prop2"]
    propertyValue = ["1", "2"]
}

artifactory {
    url="http://localhost:8081/artifactory"
    username="admin"
    password="password"
}

buildLauncher {
    buildTool="gradle"
    buildToolVersion="2.3"
    tasks = ["artifactoryPublish"]
    systemVariables= []
    projVariables = []
    javaHome=""
    propertiesFilesPath=["src\\test\\resources\\build-info-tests\\build.properties"]
    buildScriptPath = "C:\\dev\\project-examples\\gradle2-example\\build.gradle"
    commandPath="C:\\Installed\\gradle-2.3\\bin\\gradle.bat"
}