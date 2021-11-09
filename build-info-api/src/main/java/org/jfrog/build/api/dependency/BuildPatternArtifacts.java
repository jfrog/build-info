package org.jfrog.build.api.dependency;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents built artifacts, filtered by patterns in request.
 *
 * @author jbaruch
 * @see BuildPatternArtifactsRequest
 * @since 16/02/12
 */
public class BuildPatternArtifacts implements Serializable {

    private String buildName;
    private String buildNumber;
    private String started;
    private String url;

    private List<PatternResult> patternResults;

    public BuildPatternArtifacts() {
        patternResults = new ArrayList<>();
    }

    public BuildPatternArtifacts(String buildName, String buildNumber, String started, String url) {
        this.buildName = buildName;
        this.buildNumber = buildNumber;
        this.started = started;
        this.url = url;
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

    public String getStarted() {
        return started;
    }

    public void setStarted(String started) {
        this.started = started;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<PatternResult> getPatternResults() {
        return patternResults;
    }

    public void setPatternResults(List<PatternResult> patternResults) {
        this.patternResults = patternResults;
    }

    public void addPatternResult(PatternResult patternResult) {
        if (patternResults == null) {
            patternResults = new ArrayList<>();
        }
        patternResults.add(patternResult);
    }
}
