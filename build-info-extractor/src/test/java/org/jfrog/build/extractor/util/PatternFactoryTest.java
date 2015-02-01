package org.jfrog.build.extractor.util;

import org.jfrog.build.api.dependency.pattern.BuildDependencyPattern;
import org.jfrog.build.api.dependency.pattern.DependencyPattern;
import org.jfrog.build.api.dependency.pattern.PatternType;
import org.jfrog.build.extractor.clientConfiguration.util.PatternFactory;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * @author Shay Yaakov
 */
@Test
public class PatternFactoryTest {

    public void testDependencies() throws Exception {
        DependencyPattern dependencyPattern = PatternFactory.create("win-demo:*/*/*.dll;type+=32;OS+=win-7=>property");
        assertDependency(dependencyPattern, "win-demo:*/*/*.dll", "type+=32;OS+=win-7", "property", PatternType.NORMAL);

        dependencyPattern = PatternFactory.create("win-demo:**/*;type+=64;OS+=win-7=>starstar");
        assertDependency(dependencyPattern, "win-demo:**/*", "type+=64;OS+=win-7", "starstar", PatternType.NORMAL);

        dependencyPattern = PatternFactory.create("repo:win-7/**/*;OS+=win-7;type+=64=>path/deep");
        assertDependency(dependencyPattern, "repo:win-7/**/*", "OS+=win-7;type+=64", "path/deep", PatternType.NORMAL);

        dependencyPattern = PatternFactory.create("win-demo:*/*/*.dll=>all");
        assertDependency(dependencyPattern, "win-demo:*/*/*.dll", "", "all", PatternType.NORMAL);

        dependencyPattern = PatternFactory.create("repo:*/*/*.*;type=32;type=64=>all-withproperty");
        assertDependency(dependencyPattern, "repo:*/*/*.*", "type=32;type=64", "all-withproperty", PatternType.NORMAL);

        dependencyPattern = PatternFactory.create("win-demo:*/*/*.*;type=32;OS=linux");
        assertDependency(dependencyPattern, "win-demo:*/*/*.*", "type=32;OS=linux", "", PatternType.NORMAL);

        dependencyPattern = PatternFactory.create("win-demo:*/*/*.*;type=32;OS=linux=!>test\\sep");
        assertDependency(dependencyPattern, "win-demo:*/*/*.*", "type=32;OS=linux", "test/sep", PatternType.DELETE);

        dependencyPattern = PatternFactory.create("win-demo:**/*.*=!>");
        assertDependency(dependencyPattern, "win-demo:**/*.*", "", "", PatternType.DELETE);
    }

    public void testBuildDependencies() throws Exception {
        DependencyPattern pattern = PatternFactory.create("repo:**/*;a=1;b+=2@build#13=>relative\\path");
        assertBuildDependency(pattern, "repo:**/*", "a=1;b+=2", "relative/path", PatternType.NORMAL, "build", "13");

        pattern = PatternFactory.create("repo:**/*;c+=4;@build#13=>");
        assertBuildDependency(pattern, "repo:**/*", "c+=4;", "", PatternType.NORMAL, "build", "13");

        pattern = PatternFactory.create("repo:*/*/*/*.jar@build-name#13=>path");
        assertBuildDependency(pattern, "repo:*/*/*/*.jar", "", "path", PatternType.NORMAL, "build-name", "13");

        pattern = PatternFactory.create("repo:*/*.jar@build-name#10=!>path");
        assertBuildDependency(pattern, "repo:*/*.jar", "", "path", PatternType.DELETE, "build-name", "10");

        pattern = PatternFactory.create("repo:*/*.jar;a+=a;b=b@build#11=!>");
        assertBuildDependency(pattern, "repo:*/*.jar", "a+=a;b=b", "", PatternType.DELETE, "build", "11");

        pattern = PatternFactory.create("repo:*/*.jar@build#11=!>");
        assertBuildDependency(pattern, "repo:*/*.jar", "", "", PatternType.DELETE, "build", "11");
    }

    private void assertDependency(DependencyPattern result, String pattern, String matrixParams, String targetDir,
            PatternType patternType) {
        assertFalse(result instanceof BuildDependencyPattern, "Expected a dependency instance");
        assertCommonsFields(result, pattern, matrixParams, targetDir, patternType);
    }

    private void assertBuildDependency(DependencyPattern result, String pattern, String matrixParams,
            String targetDir, PatternType patternType, String buildName, String buildNumber) {
        assertTrue(result instanceof BuildDependencyPattern, "Expected a build dependency instance");
        assertCommonsFields(result, pattern, matrixParams, targetDir, patternType);
        assertEquals(((BuildDependencyPattern) result).getBuildName(), buildName, "Unexpected build name");
        assertEquals(((BuildDependencyPattern) result).getBuildNumber(), buildNumber, "Unexpected number");
    }

    private void assertCommonsFields(DependencyPattern result, String pattern, String matrixParams, String targetDir,
            PatternType patternType) {
        assertEquals(result.getPattern(), pattern, "Unexpected pattern");
        assertEquals(result.getMatrixParams(), matrixParams, "Unexpected matrix params");
        assertEquals(result.getTargetDirectory(), targetDir, "Unexpected target directory");
        assertEquals(result.getPatternType(), patternType, "Unexpected pattern type");
    }
}
