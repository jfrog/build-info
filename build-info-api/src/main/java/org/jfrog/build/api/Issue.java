package org.jfrog.build.api;

import java.io.Serializable;

/**
 * @author Noam Y. Tenne
 */
public class Issue implements Serializable {

    private String key;
    private String url;
    private String summary;

    public Issue() {
    }

    public Issue(String key, String url, String summary) {
        this.key = key;
        this.url = url;
        this.summary = summary;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
