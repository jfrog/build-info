package org.jfrog.build.api.dependency;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents request for built artifacts, filtered by patterns.
 *
 * @author jbaruch
 * @see BuildPatternArtifacts
 * @since 16/02/12
 */
public class BuildPatternArtifactsRequest implements Serializable {

    private String buildName;
    private String buildNumber;
    private String project;
    private boolean transitive;
    private List<Pattern> patterns;

    public BuildPatternArtifactsRequest() {
        patterns = new ArrayList<>();
    }

    public BuildPatternArtifactsRequest(String buildName, String buildNumber, String project) {
        this.buildNumber = buildNumber;
        this.buildName = buildName;
        this.project = project;
    }

    public String getBuildName() {
        return buildName;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public boolean isTransitive() {
        return transitive;
    }

    public void setTransitive(boolean transitive) {
        this.transitive = transitive;
    }

    public List<Pattern> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<Pattern> patterns) {
        this.patterns = patterns;
    }

    public void addPattern(Pattern pattern) {
        if (patterns == null) {
            patterns = new ArrayList<>();
        }
        patterns.add(pattern);
    }
}
