package org.jfrog.build.api;

import java.io.Serializable;

/**
 * @author Noam Y. Tenne
 */
public class IssueTracker implements Serializable {

    private String name;
    private String version;

    public IssueTracker() {
    }

    public IssueTracker(String name) {
        this(name, null);
    }

    public IssueTracker(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
