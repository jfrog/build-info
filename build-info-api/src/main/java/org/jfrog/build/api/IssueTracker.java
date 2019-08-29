package org.jfrog.build.api;

import java.io.Serializable;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IssueTracker)) {
            return false;
        }

        IssueTracker that = (IssueTracker) o;

        if (!Objects.equals(name, that.name)) {
            return false;
        }

        return Objects.equals(version, that.version);
    }
}
