package org.jfrog.build.api.builder.dependency;

import org.jfrog.build.api.dependency.BuildPatternArtifactsRequest;
import org.jfrog.build.api.dependency.Pattern;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jbaruch
 * @since 16/02/12
 */
public class BuildPatternArtifactsRequestBuilder {

    private String buildName;
    private String buildNumber;
    private String project;
    private boolean transitive;
    private List<Pattern> patterns;

    public BuildPatternArtifactsRequestBuilder() {
        patterns = new ArrayList<>();
    }

    public BuildPatternArtifactsRequest build() {
        if (buildName == null) {
            throw new IllegalArgumentException("BuildPatternArtifactsRequest must have a build name.");
        }
        if (buildNumber == null) {
            throw new IllegalArgumentException("BuildPatternArtifactsRequest must have a build number.");
        }

        BuildPatternArtifactsRequest request = new BuildPatternArtifactsRequest(buildName, buildNumber, project);
        request.setTransitive(transitive);
        request.setPatterns(patterns);
        return request;
    }

    public BuildPatternArtifactsRequestBuilder buildName(String buildName) {
        this.buildName = buildName;
        return this;
    }

    public BuildPatternArtifactsRequestBuilder buildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
        return this;
    }

    public BuildPatternArtifactsRequestBuilder project(String project) {
        this.project = project;
        return this;
    }

    public BuildPatternArtifactsRequestBuilder pattern(String pattern) {
        patterns.add(new Pattern(pattern));
        return this;
    }
}
