package org.jfrog.build.utils

import groovyx.net.http.ContentType
import org.jfrog.artifactory.client.Artifactory
import org.jfrog.artifactory.client.ArtifactoryRequest
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl
import org.jfrog.artifactory.client.model.Repository

/**
 * @author Aviad Shikloshi
 */
class TestUtils {

    def static setupArtifactory(Artifactory client, String repoName) {
        Repository testRepository = client.repositories().builders().localRepositoryBuilder().key(repoName).build()
        client.repositories().create(0, testRepository)
    }

    def static deleteBuildFromArtifactory(def client, def buildName, def buildNumber) {
        def removeBuildRequest = new ArtifactoryRequestImpl()
                .method(ArtifactoryRequest.Method.DELETE)
                .responseType(ArtifactoryRequest.ContentType.TEXT)
                .apiUrl("api/build/" + buildName)
                .addQueryParam("buildNumbers", buildNumber)
        client.restCall(removeBuildRequest)
    }

    def static getBuildInfo(def client, def buildName, def buildNumber) {
        ArtifactoryRequest getBuildInfo = new ArtifactoryRequestImpl()
                .method(ArtifactoryRequest.Method.GET)
                .responseType(ArtifactoryRequest.ContentType.JSON)
                .apiUrl("api/build/" + buildName + "/" + buildNumber)
        client.restCall(getBuildInfo)
    }

    def static deleteRepository(def client, def repoName) {
        client.repositories().repository(repoName).delete()
    }

    def static getBuildInfoFieldFromProperty(def map, def propKey) {
        if (propKey.contains(".")) {
            String[] fields = propKey.split("\\.");
            Map<String, Object> tmpMap = (Map<String, Object>) map.get(fields[0]);
            for (int i = 1; i < fields.length - 1; i++) {
                tmpMap = (Map<String, Object>) tmpMap.get(fields[i]);
            }
            return (String) tmpMap.get(fields[fields.length - 1]);
        } else {
            if (map.contains(propKey)) {
                return map.get(propKey)
            } else {
                throw new IllegalArgumentException("${propKey} is not available in current build properties.")
            }
        }
    }
}
