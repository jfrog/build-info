package org.jfrog.build.api.dependency;


import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.List;


/**
 * Build dependency, as converted from user input like
 * "libs-release-local:com/goldin/plugins/gradle/0.1.1/*.jar;status+=prod@gradle-plugins :: Build :: Gradle#LATEST => many-jars-build"
 */
public class UserBuildDependency {
    private final String buildName;                        // "gradle-plugins :: Build :: Gradle"
    private final String buildNumberRequest;               // "LATEST"
    private final List<Pattern> patterns = Lists.newLinkedList(); // "libs-release-local:com/plugins/gradle/0.1.1/*.jar;status+=prod"
    private String buildNumberResponse;              // "5"
    private String buildStarted;                     //
    private String buildUrl;                         //

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UserBuildDependency that = (UserBuildDependency) o;

        return !(buildName != null ? !buildName.equals(that.buildName) : that.buildName != null) && !(
                buildNumberResponse != null ? !buildNumberResponse.equals(that.buildNumberResponse) :
                        that.buildNumberResponse != null) && !(buildStarted != null ?
                !buildStarted.equals(that.buildStarted) : that.buildStarted != null);

    }

    @Override
    public int hashCode() {
        int result = buildName != null ? buildName.hashCode() : 0;
        result = 31 * result + (buildNumberResponse != null ? buildNumberResponse.hashCode() : 0);
        result = 31 * result + (buildStarted != null ? buildStarted.hashCode() : 0);
        return result;
    }

    public static class Pattern {

        private final String artifactoryPattern; // "libs-release-local:com/plugins/gradle/0.1.1/*.jar"
        private final String matrixParameters;   // "status+=prod"
        private final String targetDirectory;    // Directory to download dependencies to
        private PatternResult patternResult;      // Pattern result as received from Artifactory

        public Pattern(String artifactoryPattern, String matrixParameters, String targetDirectory) {

            if (StringUtils.isBlank(artifactoryPattern)) {
                throw new IllegalArgumentException("Artifactory pattern is blank");
            }
            if (matrixParameters == null) {
                throw new NullPointerException("Matrix parameters is null");
            } // Can be empty.
            if (targetDirectory == null) {
                throw new NullPointerException("Target directory is null");
            } // Can be empty.

            this.artifactoryPattern = artifactoryPattern;
            this.matrixParameters = matrixParameters;
            this.targetDirectory = targetDirectory;
        }

        public String getArtifactoryPattern() {
            return this.artifactoryPattern;
        }

        public String getMatrixParameters() {
            return this.matrixParameters;
        }

        public String getTargetDirectory() {
            return this.targetDirectory;
        }

        public PatternResult getPatternResult() {
            return this.patternResult;
        }

        public void setPatternResult(PatternResult patternResult) {
            this.patternResult = patternResult;
        }
    }


    public UserBuildDependency(String buildName, String buildNumberRequest, String pattern, String targetDirectory) {

        if (StringUtils.isBlank(buildName)) {
            throw new IllegalArgumentException("Build name is blank");
        }
        if (StringUtils.isBlank(buildNumberRequest)) {
            throw new IllegalArgumentException("Build number is blank");
        }

        this.buildName = buildName;
        this.buildNumberRequest = buildNumberRequest;
        addPattern(pattern, targetDirectory);
    }


    public String getBuildName() {
        return this.buildName;
    }

    public String getBuildNumberRequest() {
        return this.buildNumberRequest;
    }

    public List<Pattern> getPatterns() {
        return Collections.unmodifiableList(this.patterns);
    }

    public String getBuildNumberResponse() {
        return this.buildNumberResponse;
    }

    public void setBuildNumberResponse(String buildNumberResponse) {
        this.buildNumberResponse = buildNumberResponse;
    }

    public String getBuildStarted() {
        return this.buildStarted;
    }

    public void setBuildStarted(String buildStarted) {
        this.buildStarted = buildStarted;
    }

    public String getBuildUrl() {
        return this.buildUrl;
    }

    public void setBuildUrl(String buildUrl) {
        this.buildUrl = buildUrl;
    }


    public void addPattern(String pattern, String targetDirectory) {
        if (StringUtils.isBlank(pattern)) {
            throw new IllegalArgumentException("Pattern can not be blank!");
        }

        int j = pattern.lastIndexOf(';');
        Pattern p = new Pattern((j > 0) ? pattern.substring(0, j) : pattern,
                (j > 0) ? pattern.substring(j + 1) : "",
                targetDirectory);
        this.patterns.add(p);
    }
}
