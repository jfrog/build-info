package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.ci.BuildInfo;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.VoidJFrogService;

import java.io.IOException;

import static org.jfrog.build.extractor.clientConfiguration.client.artifactory.services.PublishBuildInfo.getProjectQueryParam;
import static org.jfrog.build.extractor.clientConfiguration.util.JsonUtils.toJsonString;

public class SendModuleInfo extends VoidJFrogService {
    public static final String APPLICATION_VND_ORG_JFROG_ARTIFACTORY_JSON = "application/vnd.org.jfrog.artifactory+json";
    private static final String SEND_MODULE_INFO_ENDPOINT = "/api/build/append/";
    private final Build build;

    public SendModuleInfo(BuildInfo buildInfo, Log logger) {
        super(logger);
        this.build = buildInfo.ToBuild();
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        String url = SEND_MODULE_INFO_ENDPOINT + encodeUrl(build.getName()) + "/" +
                encodeUrl(build.getNumber()) + getProjectQueryParam(build.getProject());
        HttpPost request = new HttpPost(url);
        String modulesAsJsonString = toJsonString(build.getModules());
        StringEntity stringEntity = new StringEntity(modulesAsJsonString, "UTF-8");
        stringEntity.setContentType(APPLICATION_VND_ORG_JFROG_ARTIFACTORY_JSON);
        request.setEntity(stringEntity);
        log.info("Deploying buildInfo descriptor to: " + request.getURI().toString());
        return request;
    }

    @Override
    protected void handleUnsuccessfulResponse(HttpEntity entity) throws IOException {
        log.error("Could not publish build-info modules");
        throwException(entity, getStatusCode());
    }
}
