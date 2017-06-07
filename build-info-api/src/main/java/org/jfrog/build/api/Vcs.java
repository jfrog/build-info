package org.jfrog.build.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

@XStreamAlias("vcs")
public class Vcs implements Serializable {
    String revision = "";
    String url = "";

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

    @JsonIgnore
    public boolean isEmpty() {
        return StringUtils.isEmpty(this.getRevision()) && StringUtils.isEmpty(this.getUrl());
    }
}