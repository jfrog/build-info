package org.jfrog.build.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Base implementation of the build file bean interface
 *
 * @author Noam Y. Tenne
 */
public abstract class BaseBuildFileBean extends BaseBuildBean implements BuildFileBean {

    protected String type;
    protected String sha1;
    protected String sha256;
    protected String md5;

    @JsonProperty("path")
    protected String remotePath;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseBuildFileBean)) {
            return false;
        }
        BaseBuildFileBean that = (BaseBuildFileBean) o;
        if (!Objects.equals(md5, that.md5)) {
            return false;
        }
        if (!Objects.equals(sha1, that.sha1)) {
            return false;
        }
        if (!Objects.equals(sha256, that.sha256)) {
            return false;
        }
        return Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (sha1 != null ? sha1.hashCode() : 0);
        result = 31 * result + (sha256 != null ? sha256.hashCode() : 0);
        result = 31 * result + (md5 != null ? md5.hashCode() : 0);
        result = 31 * result + (remotePath != null ? remotePath.hashCode() : 0);
        return result;
    }
}