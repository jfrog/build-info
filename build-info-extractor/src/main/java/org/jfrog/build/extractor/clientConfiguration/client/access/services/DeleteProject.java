package org.jfrog.build.extractor.clientConfiguration.client.access.services;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.VoidJFrogService;

import java.io.IOException;

import static org.jfrog.build.extractor.clientConfiguration.client.access.services.Utils.PROJECTS_ENDPOINT;

public class DeleteProject extends VoidJFrogService {
    private final String projectKey;

    public DeleteProject(String projectKey, Log logger) {
        super(logger);
        this.projectKey = projectKey;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        return new HttpDelete(PROJECTS_ENDPOINT + "/" + encodeUrl(projectKey));
    }
}
