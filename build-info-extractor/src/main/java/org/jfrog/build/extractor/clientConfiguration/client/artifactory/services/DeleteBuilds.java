package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.VoidJFrogService;
import org.jfrog.build.extractor.clientConfiguration.client.request.DeleteBuildsRequest;

import java.io.IOException;

import static org.jfrog.build.extractor.clientConfiguration.util.JsonUtils.toJsonString;

public class DeleteBuilds extends VoidJFrogService {
    public static final String DELETE_BUILDS_ENDPOINT = "api/build/delete";

    private final DeleteBuildsRequest deleteBuildsRequest;

    /**
     * Delete All build numbers of a certain build.
     */
    public DeleteBuilds(String buildName, String project, boolean deleteArtifacts, Log logger) {
        super(logger);
        deleteBuildsRequest = new DeleteBuildsRequest(buildName, project, deleteArtifacts);
    }

    /**
     * Delete multiple build numbers of a certain build.
     */
    public DeleteBuilds(String buildName, String project, String[] buildNumbers, boolean deleteArtifacts, Log logger) {
        super(logger);
        deleteBuildsRequest = new DeleteBuildsRequest(buildName, buildNumbers, project, deleteArtifacts);
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        HttpPost request = new HttpPost(DELETE_BUILDS_ENDPOINT);
        StringEntity stringEntity = new StringEntity(toJsonString(deleteBuildsRequest));
        stringEntity.setContentType("application/json");
        request.setEntity(stringEntity);
        return request;
    }
}
