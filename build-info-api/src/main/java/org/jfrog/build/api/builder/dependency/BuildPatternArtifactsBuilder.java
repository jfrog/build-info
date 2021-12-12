package org.jfrog.build.api.builder.dependency;

import org.jfrog.build.api.Build;
import org.jfrog.build.api.dependency.BuildPatternArtifacts;
import org.jfrog.build.api.dependency.PatternResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author jbaruch
 * @since 16/02/12
 */
public class BuildPatternArtifactsBuilder {
    private String buildName;
    private String buildNumber;
    private String started;
    private String url;
    private List<PatternResult> patternResults;

    public BuildPatternArtifactsBuilder() {
        patternResults = new ArrayList<>();
    }

    public BuildPatternArtifacts build() {
        if (buildName == null) {
            throw new IllegalArgumentException("BuildPatternArtifacts must have a name.");
        }
        if (buildNumber == null) {
            throw new IllegalArgumentException("BuildPatternArtifacts must have a number.");
        }
        if (started == null) {
            throw new IllegalArgumentException("BuildPatternArtifacts must have a started.");
        }
        if (url == null) {
            throw new IllegalArgumentException("BuildPatternArtifacts must have a url.");
        }
        BuildPatternArtifacts buildPatternArtifacts = new BuildPatternArtifacts(buildName, buildNumber, started, url);
        buildPatternArtifacts.setPatternResults(patternResults);
        return buildPatternArtifacts;
    }

    public BuildPatternArtifactsBuilder buildName(String buildName) {
        this.buildName = buildName;
        return this;
    }

    public BuildPatternArtifactsBuilder buildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
        return this;
    }

    public BuildPatternArtifactsBuilder patternResult(PatternResult patternResult) {
        patternResults.add(patternResult);
        return this;
    }

    public BuildPatternArtifactsBuilder started(String started) {
        this.started = started;
        return this;
    }

    public BuildPatternArtifactsBuilder startedDate(Date started) {
        if (started == null) {
            throw new IllegalArgumentException("Cannot format a null date.");
        }
        this.started = new SimpleDateFormat(Build.STARTED_FORMAT).format(started);
        return this;
    }


    public BuildPatternArtifactsBuilder url(String url) {
        this.url = url;
        return this;
    }
}
