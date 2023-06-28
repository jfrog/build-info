package org.jfrog.build.extractor.ci;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Objects;

public class Vcs implements Serializable {
    private String revision = "";
    private String message = "";
    private String branch = "";
    private String url = "";

    public Vcs() {
    }

    public Vcs(String vcsUrl, String vcsRevision) {
        this.setUrl(vcsUrl);
        this.setRevision(vcsRevision);
    }

    public Vcs(String vcsUrl, String vcsRevision, String branch, String message) {
        this(vcsUrl, vcsRevision);
        this.branch = branch;
        this.message = message;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "revision='" + revision + '\'' +
                ", commit message=" + message + '\'' +
                ", branch='" + branch + '\'' +
                ", url='" + url + '\'';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Vcs)) {
            return false;
        }

        Vcs that = (Vcs) o;
        return Objects.equals(revision, that.revision) &&
                Objects.equals(branch, that.branch) &&
                Objects.equals(message, that.message) &&
                Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(revision, branch, message, url);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return StringUtils.isEmpty(getRevision()) && StringUtils.isEmpty(getBranch()) &&
                StringUtils.isEmpty(getMessage()) && StringUtils.isEmpty(getUrl());
    }

    public org.jfrog.build.api.Vcs ToBuildVcs() {
        return new org.jfrog.build.api.Vcs(url, revision, branch, message);
    }

    public static Vcs ToBuildInfoVcs(org.jfrog.build.api.Vcs vcs) {
        return new Vcs(vcs.getUrl(), vcs.getRevision(), vcs.getBranch(), vcs.getMessage());
    }
}