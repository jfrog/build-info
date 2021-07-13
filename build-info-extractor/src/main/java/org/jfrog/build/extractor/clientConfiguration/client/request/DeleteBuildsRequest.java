package org.jfrog.build.extractor.clientConfiguration.client.request;

public class DeleteBuildsRequest {
    public String buildName;
    public String project;
    public String[] buildNumbers;
    public boolean deleteArtifacts;
    public boolean deleteAll;

    public DeleteBuildsRequest() {

    }

    public DeleteBuildsRequest(String buildName, String project, boolean deleteArtifacts) {
        this.buildName = buildName;
        this.project = project;
        this.deleteArtifacts = deleteArtifacts;
    }

    public DeleteBuildsRequest(String buildName, String[] buildNumber, String project, boolean deleteArtifacts) {
        this(buildName, project, deleteArtifacts);
        this.buildNumbers = buildNumber;
    }

    public String getBuildName() {
        return buildName;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public String[] getBuildNumbers() {
        return buildNumbers;
    }

    public void setBuildNumbers(String[] buildNumbers) {
        this.buildNumbers = buildNumbers;
    }

    public boolean isDeleteArtifacts() {
        return deleteArtifacts;
    }

    public void setDeleteArtifacts(boolean deleteArtifacts) {
        this.deleteArtifacts = deleteArtifacts;
    }

    public boolean isDeleteAll() {
        return deleteAll;
    }

    public void setDeleteAll(boolean deleteAll) {
        this.deleteAll = deleteAll;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

}
