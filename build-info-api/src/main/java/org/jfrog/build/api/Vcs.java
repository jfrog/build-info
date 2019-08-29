package org.jfrog.build.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.Objects;

@XStreamAlias(BuildInfoFields.VCS)
public class Vcs implements Serializable {
    private String revision = "";
    private String url = "";

    public Vcs() {
    }

    public Vcs(String vcsUrl, String vcsRevision) {
        this.setUrl(vcsUrl);
        this.setRevision(vcsRevision);
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "revision= '" + revision + '\'' +
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

        if (!Objects.equals(revision, that.revision)) {
            return false;
        }

        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        int result = (revision != null ? revision.hashCode() : 0);
        result = 31 * result + (url != null ? url.hashCode() : 0);
        return result;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return StringUtils.isEmpty(this.getRevision()) && StringUtils.isEmpty(this.getUrl());
    }
}