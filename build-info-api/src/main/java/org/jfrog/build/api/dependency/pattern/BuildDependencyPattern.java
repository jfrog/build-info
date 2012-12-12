package org.jfrog.build.api.dependency.pattern;


/**
 * Build dependency, as converted from user input like
 * "libs-release-local:com/goldin/plugins/gradle/0.1.1/*.jar;status+=prod@gradle-plugins :: Build :: Gradle#LATEST => many-jars-build"
 */
public class BuildDependencyPattern extends DependencyPattern {

    private String buildName;                 // "gradle-plugins :: Build :: Gradle"
    private String buildNumber;               // "LATEST"

    public BuildDependencyPattern(String pattern, String matrixParams, String targetPattern, PatternType patternType,
            String buildName, String buildNumber) {
        super(pattern, matrixParams, targetPattern, patternType);
        this.buildName = buildName;
        this.buildNumber = buildNumber;
    }

    public String getBuildName() {
        return buildName;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BuildDependencyPattern)) {
            return false;
        }

        BuildDependencyPattern that = (BuildDependencyPattern) o;

        if (!buildName.equals(that.buildName)) {
            return false;
        }
        if (!buildNumber.equals(that.buildNumber)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = buildName.hashCode();
        result = 31 * result + buildNumber.hashCode();
        return result;
    }
}
