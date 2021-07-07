package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.VoidJFrogService;

import java.io.IOException;

/**
 * Delete multiple build numbers of a certain build.
 */
public class DeleteBuilds extends VoidJFrogService {
    public static final String DELETE_BUILDS_ENDPOINT = "api/build/";
    private final String buildName;
    private final boolean deleteArtifact;
    private final String project;
    private String buildNumber;

    public DeleteBuilds(String buildName, String project, boolean deleteArtifact, Log logger) {
        super(logger);
        this.buildName = buildName;
        this.project = project;
        this.deleteArtifact = deleteArtifact;
    }

    public DeleteBuilds(String buildName, String project, String buildNumber, boolean deleteArtifact, Log logger) {
        super(logger);
        this.buildName = buildName;
        this.project = project;
        this.buildNumber = buildNumber;
        this.deleteArtifact = deleteArtifact;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        String requestUrl = DELETE_BUILDS_ENDPOINT + encodeUrl(buildName);
        if (buildName == null) {
            requestUrl += "?deleteAll=1";
        } else {
            requestUrl += "?buildNumbers=" + buildNumber;
        }
        requestUrl += "&artifacts=" + (deleteArtifact ? "1" : "0");
        if (StringUtils.isNotEmpty(project)) {
            requestUrl += "&project=" + project;
        }
        return new HttpDelete(requestUrl);
    }
}
