package org.jfrog.build.utils

import org.jfrog.artifactory.client.Artifactory
import org.jfrog.artifactory.client.ArtifactoryRequest
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl
import org.jfrog.artifactory.client.model.Repository

/**
 * @author Aviad Shikloshi
 */
class TestUtils {

    static def createRepository(Artifactory client, String repoName) {
        try {
            Repository testRepository = client.repositories().builders().localRepositoryBuilder().key(repoName).build()
            client.repositories().create(0, testRepository)
        } catch (Exception e) {

        }
    }

    static def deleteBuildFromArtifactory(def client, def buildName, def buildNumber) {
        def removeBuildRequest = new ArtifactoryRequestImpl()
                .method(ArtifactoryRequest.Method.DELETE)
                .responseType(ArtifactoryRequest.ContentType.TEXT)
                .apiUrl("api/build/" + buildName)
                .addQueryParam("buildNumbers", buildNumber)
        try {
            client.restCall(removeBuildRequest)
        } catch (Exception e) {

        }
    }

    static def getBuildInfo(def client, def buildName, def buildNumber) {
        ArtifactoryRequest getBuildInfo = new ArtifactoryRequestImpl()
                .method(ArtifactoryRequest.Method.GET)
                .responseType(ArtifactoryRequest.ContentType.JSON)
                .apiUrl("api/build/" + buildName + "/" + buildNumber)
        try {
            client.restCall(getBuildInfo)
        } catch (Exception e) {

        }
    }

    static def deleteRepository(def client, def repoName) {
        try {
            client.repositories().repository(repoName).delete()
        } catch (Exception e) {

        }
    }
}
