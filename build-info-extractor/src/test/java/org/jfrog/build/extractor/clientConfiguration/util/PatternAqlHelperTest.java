package org.jfrog.build.extractor.clientConfiguration.util;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Unit tests for PatternAqlHelper.
 */
public class PatternAqlHelperTest {

    @Test(dataProvider = "buildAqlSearchQueryProvider")
    public void testBuildAqlSearchQuery(String pattern, boolean recursive, String expectedAql) throws IOException {
        String result = PatternAqlHelper.buildAqlSearchQuery(pattern, null, null, recursive, null);
        assertEquals(result, expectedAql);
    }

    @Test(dataProvider = "createPathFilePairsProvider")
    public void testCreatePathFilePairs(String pattern, boolean recursive, List<PatternAqlHelper.RepoPathFile> expected) throws IOException {
        List<PatternAqlHelper.RepoPathFile> actual = PatternAqlHelper.createPathFilePairs("r", pattern, recursive);
        validateRepoPathFile(actual, expected, pattern);
    }

    @Test(dataProvider = "createRepoPathFileTriplesProvider")
    public void testCreateRepoPathFileTriples(String pattern, boolean recursive, List<PatternAqlHelper.RepoPathFile> expected) throws IOException {
        List<PatternAqlHelper.RepoPathFile> actual = PatternAqlHelper.createRepoPathFileTriples(pattern, recursive);
        validateRepoPathFile(actual, expected, pattern);
    }

    // Data Providers.

    @DataProvider
    private static Object[][] createRepoPathFileTriplesProvider() {
        return new Object[][]{
                {"a/*", true, new ArrayList<>(Collections.singletonList(
                        repoPathFile("a", "*", "*")))},
                {"a/a*b", true, new ArrayList<>(Arrays.asList(
                        repoPathFile("a", "a*", "*b"),
                        repoPathFile("a", ".", "a*b")))},
                {"a/a*b*", true, new ArrayList<>(Arrays.asList(
                        repoPathFile("a", "a*b*", "*"),
                        repoPathFile("a", "a*", "*b*"),
                        repoPathFile("a", ".", "a*b*")))},
                {"a/a*b*/a/b", true, new ArrayList<>(Collections.singletonList(
                        repoPathFile("a", "a*b*/a", "b")))},
                {"*a/b*/*c*d*", true, new ArrayList<>(Arrays.asList(
                        repoPathFile("*", "*a/b*/*c*d*", "*"),
                        repoPathFile("*", "*a/b*/*c*", "*d*"),
                        repoPathFile("*", "*a/b*/*", "*c*d*"),
                        repoPathFile("*", "*a/b*", "*c*d*"),
                        repoPathFile("*a", "b*", "*c*d*"),
                        repoPathFile("*a", "b*/*c*", "*d*"),
                        repoPathFile("*a", "b*/*", "*c*d*"),
                        repoPathFile("*a", "b*/*c*d*", "*")))},
                {"*aa/b*/*c*d*", true, new ArrayList<>(Arrays.asList(
                        repoPathFile("*", "*aa/b*/*c*d*", "*"),
                        repoPathFile("*", "*aa/b*/*c*", "*d*"),
                        repoPathFile("*", "*aa/b*/*", "*c*d*"),
                        repoPathFile("*", "*aa/b*", "*c*d*"),
                        repoPathFile("*aa", "b*", "*c*d*"),
                        repoPathFile("*aa", "b*/*c*", "*d*"),
                        repoPathFile("*aa", "b*/*", "*c*d*"),
                        repoPathFile("*aa", "b*/*c*d*", "*")))},
                {"*/a*/*b*a*", true, new ArrayList<>(Arrays.asList(
                        repoPathFile("*", "*a*/*b*a*", "*"),
                        repoPathFile("*", "*a*", "*b*a*"),
                        repoPathFile("*", "*a*/*b*", "*a*"),
                        repoPathFile("*", "*a*/*", "*b*a*")))},
                {"*", true, new ArrayList<>(Collections.singletonList(
                        repoPathFile("*", "*", "*")))},
                {"*/*", true, new ArrayList<>(Collections.singletonList(
                        repoPathFile("*", "*", "*")))},
                {"*/a.z", true, new ArrayList<>(Collections.singletonList(
                        repoPathFile("*", "*", "a.z")))},
                {"a/b", true, new ArrayList<>(Collections.singletonList(
                        repoPathFile("a", ".", "b")))},
                {"a/b", false, new ArrayList<>(Collections.singletonList(
                        repoPathFile("a", ".", "b")))},
                {"a//*", false, new ArrayList<>(Collections.singletonList(
                        repoPathFile("a", "", "*")))},
                {"r//a*b", false, new ArrayList<>(Collections.singletonList(
                        repoPathFile("r", "", "a*b")))},
                {"a*b", true, new ArrayList<>(Arrays.asList(
                        repoPathFile("a*", "*", "*b"),
                        repoPathFile("a*b", "*", "*")))},
                {"a*b*", true, new ArrayList<>(Arrays.asList(
                        repoPathFile("a*", "*b*", "*"),
                        repoPathFile("a*", "*", "*b*"),
                        repoPathFile("a*b*", "*", "*")))},
        };
    }

    @DataProvider
    private static Object[][] createPathFilePairsProvider() {
        return new Object[][]{
                {"a", true, new ArrayList<>(Collections.singletonList(
                        repoPathFile("r", ".", "a")))},
                {"a/*", true, new ArrayList<>(Arrays.asList(
                        repoPathFile("r", "a", "*"),
                        repoPathFile("r", "a/*", "*")))},
                {"a/a*b", true, new ArrayList<>(Arrays.asList(
                        repoPathFile("r", "a", "a*b"),
                        repoPathFile("r", "a/a*", "*b")))},
                {"a/a*b*", true, new ArrayList<>(Arrays.asList(
                        repoPathFile("r", "a/a*", "*b*"),
                        repoPathFile("r", "a/a*", "*b*"),
                        repoPathFile("r", "a/a*b*", "*")))},
                {"a/a*b*/a/b", true, new ArrayList<>(Collections.singletonList(
                        repoPathFile("r", "a/a*b*/a", "b")))},
                {"*/a*/*b*a*", true, new ArrayList<>(Arrays.asList(
                        repoPathFile("r", "*/a*", "*b*a*"),
                        repoPathFile("r", "*/a*/*", "*b*a*"),
                        repoPathFile("r", "*/a*/*b*", "*a*"),
                        repoPathFile("r", "*/a*/*b*a*", "*")))},
                {"*", true, new ArrayList<>(Collections.singletonList(
                        repoPathFile("r", "*", "*")))},
                {"*/*", true, new ArrayList<>(Arrays.asList(
                        repoPathFile("r", "*", "*"),
                        repoPathFile("r", "*/*", "*")))},
                {"*/a.z", true, new ArrayList<>(Collections.singletonList(
                        repoPathFile("r", "*", "a.z")))},
                {"a", false, new ArrayList<>(Collections.singletonList(
                        repoPathFile("r", ".", "a")))},
                {"/*", false, new ArrayList<>(Collections.singletonList(
                        repoPathFile("r", "", "*")))},
                {"/a*b", false, new ArrayList<>(Collections.singletonList(
                        repoPathFile("r", "", "a*b")))},
                {"a*b*", true, new ArrayList<>(Arrays.asList(
                        repoPathFile("r", "a*", "*b*"),
                        repoPathFile("r", "a*b*", "*"),
                        repoPathFile("r", ".", "a*b*")))},
                {"*b*", true, new ArrayList<>(Arrays.asList(
                        repoPathFile("r", "*b*", "*"),
                        repoPathFile("r", "*", "*b*")))}
        };
    }

    @DataProvider
    private static Object[][] buildAqlSearchQueryProvider() {
        return new Object[][]{
                {"repo-local", true, "{\"$or\":[{\"$and\":[{\"repo\":\"repo-local\",\"path\":{\"$match\":\"*\"},\"name\":{\"$match\":\"*\"}}]}]}"},
                {"repo-w*ldcard", true, "{\"$or\":[{\"$and\":[{\"repo\":{\"$match\":\"repo-w*\"},\"path\":{\"$match\":\"*\"},\"name\":{\"$match\":\"*ldcard\"}}]},{\"$and\":[{\"repo\":{\"$match\":\"repo-w*ldcard\"},\"path\":{\"$match\":\"*\"},\"name\":{\"$match\":\"*\"}}]}]}"},
                {"repo-local2/a*b*c/dd/", true, "{\"path\":{\"$ne\":\".\"},\"$or\":[{\"$and\":[{\"repo\":\"repo-local2\",\"path\":{\"$match\":\"a*b*c/dd\"},\"name\":{\"$match\":\"*\"}}]},{\"$and\":[{\"repo\":\"repo-local2\",\"path\":{\"$match\":\"a*b*c/dd/*\"},\"name\":{\"$match\":\"*\"}}]}]}"},
                {"repo-local*/a*b*c/dd/", true, "{\"path\":{\"$ne\":\".\"},\"$or\":[{\"$and\":[{\"repo\":{\"$match\":\"repo-local*\"},\"path\":{\"$match\":\"*a*b*c/dd\"},\"name\":{\"$match\":\"*\"}}]},{\"$and\":[{\"repo\":{\"$match\":\"repo-local*\"},\"path\":{\"$match\":\"*a*b*c/dd/*\"},\"name\":{\"$match\":\"*\"}}]}]}"},
                {"repo-local", false, "{\"$or\":[{\"$and\":[{\"repo\":\"repo-local\",\"path\":\".\",\"name\":{\"$match\":\"*\"}}]}]}"},
                {"*repo-local", false, "{\"$or\":[{\"$and\":[{\"repo\":{\"$match\":\"*\"},\"path\":\".\",\"name\":{\"$match\":\"*repo-local\"}}]},{\"$and\":[{\"repo\":{\"$match\":\"*repo-local\"},\"path\":\".\",\"name\":{\"$match\":\"*\"}}]}]}"},
                {"repo-local2/a*b*c/dd/", false, "{\"path\":{\"$ne\":\".\"},\"$or\":[{\"$and\":[{\"repo\":\"repo-local2\",\"path\":{\"$match\":\"a*b*c/dd\"},\"name\":{\"$match\":\"*\"}}]}]}"},
                {"*/a*b*c/dd/", false, "{\"path\":{\"$ne\":\".\"},\"$or\":[{\"$and\":[{\"repo\":{\"$match\":\"*\"},\"path\":{\"$match\":\"*a*b*c/dd\"},\"name\":{\"$match\":\"*\"}}]}]}"}
        };
    }

    // Utils.

    private void validateRepoPathFile(List<PatternAqlHelper.RepoPathFile> actual,
                                      List<PatternAqlHelper.RepoPathFile> expected, String pattern) {
        // Validate length.
        assertEquals(actual.size(), expected.size(),
                String.format("Wrong triple.\nPattern: %s\nExcpected: %s\nActual: %s", pattern, expected, actual));

        for (PatternAqlHelper.RepoPathFile triple : expected) {
            boolean found = false;
            for (PatternAqlHelper.RepoPathFile actualTriple : actual) {
                if (triple.equals(actualTriple)) {
                    found = true;
                }
            }
            assertTrue(found, String.format("Wrong triple for pattern: '%s'. Missing %s in %s", pattern, triple, actual));
        }
    }

    private static PatternAqlHelper.RepoPathFile repoPathFile(String repo, String path, String file) {
        return new PatternAqlHelper.RepoPathFile(repo, path, file);
    }
}
