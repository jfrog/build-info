package org.jfrog.build.extractor.clientConfiguration.client.access;

import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ManagerBase;
import org.jfrog.build.extractor.clientConfiguration.client.access.services.*;
import org.jfrog.build.extractor.clientConfiguration.client.response.CreateAccessTokenResponse;

import java.io.IOException;

public class AccessManager extends ManagerBase {

    public AccessManager(String AccessUrl, String accessToken, Log log) {
        super(AccessUrl, StringUtils.EMPTY, StringUtils.EMPTY, accessToken, log);
        // The Access service should not accept the "anonymous" user
        jfrogHttpClient.setNoAnonymousUser();
    }

    // Access has no version API.
    @Override
    public org.jfrog.build.client.Version getVersion() throws IOException {
        return null;
    }

    public void createProject(String projectJsonConfig) throws IOException {
        new CreateProject(projectJsonConfig, log).execute(jfrogHttpClient);
    }

    public void updateProject(String projectKey, String projectJsonConfig) throws IOException {
        new UpdateProject(projectKey, projectJsonConfig, log).execute(jfrogHttpClient);
    }

    public void deleteProject(String projectKey) throws IOException {
        new DeleteProject(projectKey, log).execute(jfrogHttpClient);
    }

    public String getProject(String projectKey) throws IOException {
        return new GetProject(projectKey, log).execute(jfrogHttpClient);
    }

    public void sendBrowserLoginRequest(String uuid) throws IOException {
        new SendBrowserLoginRequest(uuid, log).execute(jfrogHttpClient);
    }

    public CreateAccessTokenResponse getBrowserLoginRequestToken(String uuid) throws IOException {
        return new GetBrowserLoginRequestToken(uuid, log).execute(jfrogHttpClient);
    }

}
