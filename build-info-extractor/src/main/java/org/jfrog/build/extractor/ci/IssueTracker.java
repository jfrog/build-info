package org.jfrog.build.extractor.ci;

import java.io.Serializable;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IssueTracker)) {
            return false;
        }

        IssueTracker that = (IssueTracker) o;
        return Objects.equals(name, that.name) && Objects.equals(version, that.version);
    }

    public org.jfrog.build.api.IssueTracker ToBuildIssueTracker() {
        return new org.jfrog.build.api.IssueTracker(name, version);
    }

    public static IssueTracker ToBuildInfoIssueTracker(org.jfrog.build.api.IssueTracker it) {
        return new IssueTracker(it.getName(), it.getVersion());
    }
}
