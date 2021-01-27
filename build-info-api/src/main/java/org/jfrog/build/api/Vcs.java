package org.jfrog.build.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.Objects;

@XStreamAlias(BuildInfoFields.VCS)
public class Vcs implements Serializable {
    private String revision = "";
    private String branch = "";
    private String url = "";

    public Vcs() {
    }

    public Vcs(String vcsUrl, String vcsRevision) {
        this.setUrl(vcsUrl);
        this.setRevision(vcsRevision);
    }

    public Vcs(String vcsUrl, String vcsRevision, String branch) {
        this(vcsUrl, vcsRevision);
        this.branch = branch;
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

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "revision='" + revision + '\'' +
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
                Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(revision, branch, url);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return StringUtils.isEmpty(getRevision()) && StringUtils.isEmpty(getBranch()) && StringUtils.isEmpty(getUrl());
    }
}