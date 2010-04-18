package org.jfrog.build.api.util;

import org.jfrog.build.api.BaseBuildFileBean;
import org.jfrog.build.api.BuildFileBean;

import java.io.File;

/**
 * Contains data about a build file (an artifact or a dependency) with its physical file information
 *
 * @author Noam Y. Tenne
 */
public class DeployableFile extends BaseBuildFileBean {

    /**
     * The build file details
     */
    private BuildFileBean buildFile;

    /**
     * The file to deploy
     */
    File file;

    private String groupId;
    private String artifactId;
    private String version;
    private String classifier;

    public BuildFileBean getBuildFile() {
        return buildFile;
    }

    public void setBuildFile(BuildFileBean buildFile) {
        this.buildFile = buildFile;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }
}