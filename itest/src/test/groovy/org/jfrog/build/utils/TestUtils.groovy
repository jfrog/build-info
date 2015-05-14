package org.jfrog.build.utils

import org.jfrog.artifactory.client.Artifactory
import org.jfrog.artifactory.client.ArtifactoryRequest
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl
import org.jfrog.artifactory.client.model.Repository

import org.slf4j.Logger

/**
 * @author Aviad Shikloshi
 */
class TestUtils {

    static def createRepository(def client, def repoName, def logger = null) {
        try {
            Repository testRepository = client.repositories().builders().localRepositoryBuilder().key(repoName).build()
            client.repositories().create(0, testRepository)
        } catch (Exception e) {
            if (logger != null) {
                logger.warn("Error while creating repository: ${repoName} in Artifactory: ${client.getUri()}")
            }
        }
    }

    static def deleteBuildFromArtifactory(def client, def buildName, def buildNumber, def logger = null) {
        def removeBuildRequest = new ArtifactoryRequestImpl()
                .method(ArtifactoryRequest.Method.DELETE)
                .responseType(ArtifactoryRequest.ContentType.TEXT)
                .apiUrl("api/build/${buildName}")
                .addQueryParam("buildNumbers", buildNumber)
        try {
            client.restCall(removeBuildRequest)
        } catch (Exception e) {
            if (logger != null) {
                logger.warn("Error while deleting build: ${buildName} :: ${buildNumber} from Artifactory: ${client.getUri()}")
            }
        }
    }

    static def getBuildInfo(def client, def buildName, def buildNumber, def logger = null) {
        ArtifactoryRequest getBuildInfo = new ArtifactoryRequestImpl()
                .method(ArtifactoryRequest.Method.GET)
                .responseType(ArtifactoryRequest.ContentType.JSON)
                .apiUrl("api/build/${buildName}/${buildNumber}")
        try {
            client.restCall(getBuildInfo)
        } catch (Exception e) {
            if (logger != null) {
                logger.warn("Error while getting build info for build: ${buildName} :: ${buildNumber} from Artifactory: ${client.getUri()}")
            }
        }
    }

    static def deleteRepository(def client, def repoName, def logger = null) {
        try {
            client.repositories().repository(repoName).delete()
        } catch (Exception e) {
            if (logger != null) {
                logger.warn("Error while deleting repository: ${repoName} from Artifactory: ${client.getUri()}")
            }
        }
    }
}
