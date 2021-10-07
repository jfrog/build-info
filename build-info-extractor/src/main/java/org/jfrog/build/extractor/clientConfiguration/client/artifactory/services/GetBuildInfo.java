package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.ci.BuildInfo;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.JFrogService;
import org.jfrog.build.extractor.clientConfiguration.client.response.GetBuildInfoResponse;

import java.io.IOException;
import java.io.InputStream;

import static org.jfrog.build.extractor.clientConfiguration.client.artifactory.services.PublishBuildInfo.getProjectQueryParam;

public class GetBuildInfo extends JFrogService<BuildInfo> {

    private final String buildName;
    private final String buildNumber;
    private final String project;

    public GetBuildInfo(String buildName, String buildNumber, String project, Log logger) {
        super(logger);

        this.buildName = buildName;
        this.buildNumber = buildNumber;
        this.project = project;
    }

    @Override
    public HttpRequestBase createRequest() {
        String apiEndPoint = String.format("%s/%s/%s%s", "api/build", encodeUrl(buildName), encodeUrl(buildNumber), getProjectQueryParam(project));
        return new HttpGet(apiEndPoint);
    }

    @Override
    protected void setResponse(InputStream stream) throws IOException {
        Build build = getMapper().readValue(stream, GetBuildInfoResponse.class).getBuildInfo();
        result = build == null ? null : build.ToBuildInfo();
    }
}
