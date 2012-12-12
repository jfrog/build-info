package org.jfrog.build.api.dependency.pattern;

/**
 * @author Shay Yaakov
 */
public class DependencyPattern {

    protected String pattern; // "libs-release-local:com/plugins/gradle/0.1.1/*.jar"

    protected String matrixParams; // "status+=prod;QA=passed"

    protected String targetDirectory; // Directory to download dependencies to (what's after the => sign)

    protected PatternType patternType = PatternType.NORMAL;

    public DependencyPattern(String pattern, String matrixParams, String targetDirectory, PatternType patternType) {
        this.pattern = pattern;
        this.matrixParams = matrixParams;
        this.targetDirectory = targetDirectory;
        this.patternType = patternType;
    }

    public String getPattern() {
        return pattern;
    }

    public String getMatrixParams() {
        return matrixParams;
    }

    public String getTargetDirectory() {
        return targetDirectory;
    }

    public PatternType getPatternType() {
        return patternType;
    }
}
