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
    buildScriptPath = ""
    commandPath=""
}