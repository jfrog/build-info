node('java') {

    // Define projects configurations.
    def projectsConfig = [
        "build-info-extractor-npm": [
            releaseVersion: "${EXTRACTOR_RELEASE_VERSION}",
            nextVersion: "${EXTRACTOR_NEXT_VERSION}",
            buildTasks: 'build-info-extractor-npm:clean build-info-extractor-npm:artifactoryPublish -x test',
            releaseTasks: '-PextractorRelease=true build-info-extractor-npm:clean build-info-extractor-npm:artifactoryPublish -x test',
            buildName: 'EcoSystemRelease :: build-info-npm-extractor',
            tagName: 'build-info-npm-extractor'
        ],
        "build-info-extractor-ivy": [
            releaseVersion: "${EXTRACTOR_RELEASE_VERSION}",
            nextVersion: "${EXTRACTOR_NEXT_VERSION}",
            buildTasks: 'build-info-extractor-ivy:clean build-info-extractor-ivy:artifactoryPublish -x test',
            releaseTasks: 'build-info-extractor-ivy:clean build-info-extractor-ivy:artifactoryPublish -x test',
            buildName: 'EcoSystemRelease :: build-info-ivy-extractor',
            tagName: 'build-info-ivy-extractor'
        ],
        "build-info-extractor-gradle": [
            releaseVersion: "${GRADLE_RELEASE_VERSION}",
            nextVersion: "${GRADLE_NEXT_VERSION}",
            buildTasks: 'build-info-extractor-gradle:clean build-info-extractor-gradle:build build-info-extractor-gradle:artifactoryPublish -x  test',
            releaseTasks: 'build-info-extractor-gradle:clean build-info-extractor-gradle:build build-info-extractor-gradle:artifactoryPublish -x  test',
            buildName: 'EcoSystemRelease :: build-info-gradle-extractor',
            tagName: 'build-info-gradle-extractor'
        ],
        "build-info-extractor-go": [
            releaseVersion: "${EXTRACTOR_RELEASE_VERSION}",
            nextVersion: "${EXTRACTOR_NEXT_VERSION}",
            buildTasks: 'build-info-extractor-go:clean build-info-extractor-go:artifactoryPublish -x test',
            releaseTasks: '-PextractorRelease=true  build-info-extractor-go:clean build-info-extractor-go:artifactoryPublish -x test',
            buildName: 'EcoSystemRelease :: build-info-go-extractor',
            tagName: 'build-info-go-extractor'
        ],
        "build-info-extractor-pip": [
            releaseVersion: "${EXTRACTOR_RELEASE_VERSION}",
            nextVersion: "${EXTRACTOR_NEXT_VERSION}",
            buildTasks: 'build-info-extractor-pip:clean build-info-extractor-pip:artifactoryPublish -x test',
            releaseTasks: '-PextractorRelease=true  build-info-extractor-pip:clean build-info-extractor-pip:artifactoryPublish -x test',
            buildName: 'EcoSystemRelease :: build-info-pip-extractor',
            tagName: 'build-info-pip-extractor'
        ],
        "build-info-extractor-nuget": [
            releaseVersion: "${EXTRACTOR_RELEASE_VERSION}",
            nextVersion: "${EXTRACTOR_NEXT_VERSION}",
            buildTasks: 'build-info-extractor-nuget:clean build-info-extractor-nuget:artifactoryPublish -x test',
            releaseTasks: '-PextractorRelease=true  build-info-extractor-nuget:clean build-info-extractor-nuget:artifactoryPublish -x test',
            buildName: 'EcoSystemRelease :: build-info-nuget-extractor',
            tagName: 'build-info-nuget-extractor'
        ],
        "build-info-extractor-maven3": [
            releaseVersion: "${EXTRACTOR_RELEASE_VERSION}",
            nextVersion: "${EXTRACTOR_NEXT_VERSION}",
            buildTasks: 'build-info-extractor-maven3:clean build-info-extractor-maven3:artifactoryPublish -x test',
            releaseTasks: '-PextractorRelease=true  build-info-extractor-maven3:clean build-info-extractor-maven3:artifactoryPublish -x test',
            buildName: 'EcoSystemRelease :: build-info-maven3-extractor',
            tagName: 'build-info-extractor-maven3'
        ],
        "build-info": [
            releaseVersion: "${EXTRACTOR_RELEASE_VERSION}",
            nextVersion: "${EXTRACTOR_NEXT_VERSION}",
            buildTasks: 'clean build-info-api:artifactoryPublish build-info-client:artifactoryPublish build-info-extractor:artifactoryPublish build-info-vcs:artifactoryPublish -x test',
            releaseTasks: 'clean build-info-api:artifactoryPublish build-info-client:artifactoryPublish build-info-extractor:artifactoryPublish build-info-vcs:artifactoryPublish -x test',
            buildName: 'EcoSystemRelease :: build-info-extractor',
            tagName: 'build-info-extractor'
        ]
    ]

    // Prepare build env.
    cleanWs()
    def jdktool = tool name: "jdk-8u111-linux-x64-jce-unlimited-policy"
    env.JAVA_HOME = jdktool

    // Get selected projects to build.
    def buildProjList = getProjectsToBuild(BUILD_PROJECTS)
    if (!buildProjList) {
        error("No projects were selected for build.")
    }

    // Set gradle configurations.
    def deployServer = Artifactory.server 'oss.jfrog.org'
    def resolveServer = Artifactory.server 'oss.jfrog.org'
    def rtGradle = Artifactory.newGradleBuild()
    rtGradle.usesPlugin = true
    rtGradle.useWrapper = true

    // Clone.
    git(
        url: 'https://github.com/jfrog/build-info.git',
        branch: 'master'
    )

    stage('Build') {
        if ("$EXECUTION_MODE".toString().equals("Build")) {
            rtGradle.deployer server: deployServer, repo: 'oss-snapshot-local'
            rtGradle.resolver server: resolveServer, repo: 'remote-repos'
            rtGradle.deployer.deployIvyDescriptors = false

            buildProjList.each { proj ->
                stage("Building ${proj}") {
                    def buildConfig = projectsConfig[proj]
                    buildProject(deployServer, rtGradle, buildConfig.buildName, buildConfig.buildTasks)
                }
            }
        }
    }

    stage('Release Staging') {
        if ("$EXECUTION_MODE".toString().equals("Release Staging")) {
            def latestReleaseVersion = "${EXTRACTOR_LATEST_RELEASE_VERSION}"
            def latestNextVersion = "${EXTRACTOR_LATEST_NEXT_VERSION}"

            echo "Bump release version"
            if (!buildProjList) {
                error("No projects were selected for Release Staging.")
            }
            bumpVersion(buildProjList, projectsConfig, latestReleaseVersion, 'releaseVersion')
            
            wrap([$class: 'MaskPasswordsBuildWrapper', varPasswordPairs: [[password: 'GITHUB_API_KEY', var: 'SECRET']]]) {
                sh 'git commit -am "[artifactory-release] Release version"'
                sh 'git push https://${GITHUB_USERNAME}:${GITHUB_API_KEY}@github.com/jfrog/build-info.git'
            }

            echo "Build release versions"
            rtGradle.deployer server: deployServer, repo: 'oss-release-local'
            rtGradle.resolver server: resolveServer, repo: 'remote-repos'
            rtGradle.deployer.deployIvyDescriptors = false

            buildProjList.each { proj ->
                stage ("Building ${proj}") {
                    // Build project.
                    def buildConfig = projectsConfig[proj]
                    buildProject(deployServer, rtGradle, buildConfig.buildName, buildConfig.releaseTasks)
                    // Create tag.
                    def tag = buildConfig.tagName + "-" + buildConfig.releaseVersion
                    sh "git tag $tag"
                    wrap([$class: 'MaskPasswordsBuildWrapper', varPasswordPairs: [[password: 'GITHUB_API_KEY', var: 'SECRET']]]) {
                        sh 'git push https://${GITHUB_USERNAME}:${GITHUB_API_KEY}@github.com/jfrog/build-info.git --tags'
                    }
                }
            }

            echo "Bump development version"
            bumpVersion(buildProjList, projectsConfig, latestNextVersion, 'nextVersion')
            sh 'git commit -am "[artifactory-release] Next development version"'
            wrap([$class: 'MaskPasswordsBuildWrapper', varPasswordPairs: [[password: 'GITHUB_API_KEY', var: 'SECRET']]]) {
                sh 'git push https://${GITHUB_USERNAME}:${GITHUB_API_KEY}@github.com/jfrog/build-info.git'
            }
        }
    }
}

def getProjectsToBuild(projects) {
    def buildProjArr
    if (projects?.trim()) {
        buildProjArr = "$BUILD_PROJECTS".split(',') as String[]
        return buildProjArr.toList()
    }
}

def buildProject(server, rtGradle, buildName, tasks) {
    def buildInfo = Artifactory.newBuildInfo()
    buildInfo.name = buildName
    rtGradle.run buildFile: 'build.gradle', tasks: tasks, buildInfo: buildInfo
    //buildInfo.env.collect()
    server.publishBuildInfo buildInfo
}

def bumpVersion(buildProjList, projectsConfig, buildInfoLatestVersion, versionField) {
    // Read gradle.properties.
    def props = readProperties file: "gradle.properties"
    // Update build-info latest version.
    props['build-info-latest-release-version'] = buildInfoLatestVersion
    // Update required values.
    buildProjList.each { proj ->
        propsKey = proj + "-version"
        versionValue = projectsConfig[proj][versionField]
        // Check not null or empty.
        if (versionValue?.trim()) {
            props[propsKey] = versionValue
        } else {
            error("the project '" + proj + "' was selected for release but release" +
            " version was not provided.")
        }
    }
    // Save new values to gradle.properties.
    def content = ""
    for(s in props) {
        content += s.toString() + "\n"
    }
    writeFile file: 'gradle.properties', text: content
    sh 'cat gradle.properties'
}