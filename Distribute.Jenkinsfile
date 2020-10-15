node('java') {
    cleanWs()
    
    def server = Artifactory.server('oss.jfrog.org')
        
    stage('Distribute') {
        withCredentials([usernamePassword(credentialsId: 'oss-deployer', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_PASS')]) {
            server.username = ARTIFACTORY_USER
            server.password = ARTIFACTORY_PASS

            def distributionConfig = [
            // Mandatory parameters
            'buildName'             : P_BUILD_NAME,
            'buildNumber'           : P_BUILD_NUMBER,
            'targetRepo'            : 'jfrog-packages',
            
            // Optional parameters
            'publish'               : false, // Default: true. If true, artifacts are published when deployed to Bintray.
            'overrideExistingFiles' : true, // Default: false. If true, Artifactory overwrites builds already existing in the target path in Bintray.
            'async'                 : false, // Default: false. If true, the build will be distributed asynchronously. Errors and warnings may be viewed in the Artifactory log.
            'dryRun'                : false, // Default: false. If true, distribution is only simulated. No files are actually moved.
            ]
            server.distribute distributionConfig
        }
    }
}