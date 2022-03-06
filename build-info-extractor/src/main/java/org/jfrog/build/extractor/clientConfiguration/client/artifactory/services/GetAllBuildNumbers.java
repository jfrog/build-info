package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.JFrogService;
import org.jfrog.build.extractor.clientConfiguration.client.response.GetAllBuildNumbersResponse;

import java.io.IOException;
import java.io.InputStream;

import static org.jfrog.build.extractor.UrlUtils.encodeUrlPathPart;
import static org.jfrog.build.extractor.UrlUtils.getProjectQueryParam;

public class GetAllBuildNumbers extends JFrogService<GetAllBuildNumbersResponse> {

    private final String buildName;
    private final String project;

    public GetAllBuildNumbers(String buildName, String project, Log logger) {
        super(logger);
        this.buildName = buildName;
        this.project = project;
    }

    @Override
    public HttpRequestBase createRequest() {
        String apiEndPoint = String.format("%s/%s%s", "api/build", encodeUrlPathPart(buildName), getProjectQueryParam(project));
        return new HttpGet(apiEndPoint);
    }

    @Override
    protected void setResponse(InputStream stream) throws IOException {
        result = getMapper().readValue(stream, GetAllBuildNumbersResponse.class);
    }

    @Override
    protected void handleUnsuccessfulResponse(HttpEntity entity) throws IOException {
        if (statusCode == HttpStatus.SC_NOT_FOUND) {
            result = new GetAllBuildNumbersResponse();
        } else {
            throwException(entity, getStatusCode());
        }
    }
}
