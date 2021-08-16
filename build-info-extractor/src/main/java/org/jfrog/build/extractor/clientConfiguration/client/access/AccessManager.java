package org.jfrog.build.extractor.clientConfiguration.client.access;

import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ManagerBase;
import org.jfrog.build.extractor.clientConfiguration.client.access.services.CreateProject;
import org.jfrog.build.extractor.clientConfiguration.client.access.services.DeleteProject;
import org.jfrog.build.extractor.clientConfiguration.client.access.services.GetProject;
import org.jfrog.build.extractor.clientConfiguration.client.access.services.UpdateProject;

import java.io.IOException;

public class AccessManager extends ManagerBase {

    public AccessManager(String AccessUrl, String accessToken, Log log) {
        super(AccessUrl, StringUtils.EMPTY, StringUtils.EMPTY, accessToken, log);
    }

    // Access has no version API.
    public org.jfrog.build.client.Version getVersion() throws IOException {
        return null;
    }

    public void createProject(String projectJsonConfig) throws IOException {
        CreateProject createProject = new CreateProject(projectJsonConfig, log);
        createProject.execute(jfrogHttpClient);
    }

    public void updateProject(String projectKey, String projectJsonConfig) throws IOException {
        UpdateProject updateProject = new UpdateProject(projectKey, projectJsonConfig, log);
        updateProject.execute(jfrogHttpClient);
    }

    public void deleteProject(String projectKey) throws IOException {
        DeleteProject deleteProject = new DeleteProject(projectKey, log);
        deleteProject.execute(jfrogHttpClient);
    }

    public String getProject(String projectKey) throws IOException {
        GetProject getProject = new GetProject(projectKey, log);
        return getProject.execute(jfrogHttpClient);
    }

}
