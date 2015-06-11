package utils

import groovyx.net.http.HttpResponseException
import org.jfrog.artifactory.client.Artifactory
import org.jfrog.artifactory.client.ArtifactoryRequest
import org.jfrog.artifactory.client.UploadableArtifact
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl
import org.jfrog.artifactory.client.model.Repository

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Aviad Shikloshi
 */
class TestUtils {
    static final OS_WIN = System.getProperty("os.name").contains("Windows")
    private static final Logger logger = LoggerFactory.getLogger(TestUtils.class);

    static def gradleWrapperScript(){
        (OS_WIN ? "gradlew.bat" : "gradlew")
    }

    static def uploadArtifacts(Artifactory client, String repoKey, String targetPath, File file, properties){
        UploadableArtifact uploadableArtifact = client.repository(repoKey).upload(targetPath, file)

        if(properties){
            properties.each{key, val ->
                uploadableArtifact.withProperty(key, val)
            }
        }

        try{
            def deployed = uploadableArtifact.doUpload()
            deployed
        }
        catch (Exception e){
            if (logger != null) {
                logger.error("Error while uploading artifact: ${repoKey}:${targetPath} in Artifactory: ${client.getUri()}, error: ${e.getMessage()}")
            }
            throw e
        }
    }

    static def createRepository(Artifactory client, String repoName) {
        try {
            Repository testRepository = client.repositories().builders().localRepositoryBuilder().key(repoName).build()
            client.repositories().create(0, testRepository)
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Error while creating repository: ${repoName} in Artifactory: ${client.getUri()}, error: ${e.getMessage()}")
            }
            throw e
        }
    }

    static def getBuildArtifacts(Artifactory client, TestProfile testConfig){
        Map<String, Object> response;
        Map<String,Object> body = testConfig.testConfig.artifacts.buildArtifacts
        body.put("buildName", testConfig.buildProperties.get(TestConstants.buildName))
        body.put("buildNumber", testConfig.buildProperties.get(TestConstants.buildNumber))
        body.put("repos", [
                            testConfig.buildProperties.get(TestConstants.repoKey),
                            testConfig.buildProperties.get(TestConstants.snapshotRepoKey)
                          ]
        )

        ArtifactoryRequest searchArtifacts = new ArtifactoryRequestImpl()
                .apiUrl("api/search/buildArtifacts")
                .method(ArtifactoryRequest.Method.POST)
                .requestType(ArtifactoryRequest.ContentType.JSON)
                .responseType(ArtifactoryRequest.ContentType.JSON)
                .requestBody(body)

        try{
            response = client.restCall(searchArtifacts)
            response
        }
        catch(HttpResponseException e){
            //Result is empty - no artifacts
            if(e.statusCode == 404){
                response = ["results":[]]
                return response
            }
            if (logger != null) {
                logger.error("Error in rest call to Artifactory: ${e.getMessage()}")
            }
            throw e
        }
        catch (Exception e) {
            if (logger != null) {
                logger.error("Error in rest call to Artifactory: ${e.getMessage()}")
            }
            throw e
        }
    }

    static def deleteBuildFromArtifactory(Artifactory client, String buildName, String buildNumber) {
        def removeBuildRequest = new ArtifactoryRequestImpl()
                .method(ArtifactoryRequest.Method.DELETE)
                .responseType(ArtifactoryRequest.ContentType.TEXT)
                .apiUrl("api/build/${buildName}")
                .addQueryParam("buildNumbers", buildNumber)
        try {
            client.restCall(removeBuildRequest)
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Error while deleting build: ${buildName} :: ${buildNumber} from Artifactory: ${client.getUri()}, error: ${e.getMessage()}")
            }
            throw e
        }
    }

    static def getBuildInfo(Artifactory client, String buildName, String buildNumber) {
        ArtifactoryRequest getBuildInfo = new ArtifactoryRequestImpl()
                .method(ArtifactoryRequest.Method.GET)
                .responseType(ArtifactoryRequest.ContentType.JSON)
                .apiUrl("api/build/${buildName}/${buildNumber}")
        try {
            client.restCall(getBuildInfo)
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Error while getting build info for build: ${buildName} :: ${buildNumber} from Artifactory: ${client.getUri()}, error: ${e.getMessage()}")
            }
            throw e
        }
    }

    static def deleteRepository(Artifactory client, String repoName) {
        try {
            client.repositories().repository(repoName).delete()
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Error while deleting repository: ${repoName} from Artifactory: ${client.getUri()}, error: ${e.getMessage()}")
            }
            throw e
        }
    }

    static def saveLicense(Artifactory client) {
        def lic = this.getClass().getResource("/artifactory/pro.lic").text
        try {
            ArtifactoryRequest addLicense = new ArtifactoryRequestImpl()
                    .method(ArtifactoryRequest.Method.POST)
                    .requestType(ArtifactoryRequest.ContentType.JSON)
                    .requestBody([licenseKey:lic])
                    .apiUrl("api/system/license")

            client.restCall(addLicense)
        } catch (ConnectException con) {
            logger.debug('fail to save license, good luck next attempt')
        } catch (HttpResponseException ed) {
            logger.debug('fail to save license, good luck next attempt')
        }
    }
}
