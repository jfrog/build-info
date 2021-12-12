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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeployableFile)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        DeployableFile that = (DeployableFile) o;

        if (artifactId != null ? !artifactId.equals(that.artifactId) : that.artifactId != null) {
            return false;
        }
        if (buildFile != null ? !buildFile.equals(that.buildFile) : that.buildFile != null) {
            return false;
        }
        if (classifier != null ? !classifier.equals(that.classifier) : that.classifier != null) {
            return false;
        }
        if (file != null ? !file.equals(that.file) : that.file != null) {
            return false;
        }
        if (groupId != null ? !groupId.equals(that.groupId) : that.groupId != null) {
            return false;
        }
        if (version != null ? !version.equals(that.version) : that.version != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (buildFile != null ? buildFile.hashCode() : 0);
        result = 31 * result + (file != null ? file.hashCode() : 0);
        result = 31 * result + (groupId != null ? groupId.hashCode() : 0);
        result = 31 * result + (artifactId != null ? artifactId.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (classifier != null ? classifier.hashCode() : 0);
        return result;
    }
}