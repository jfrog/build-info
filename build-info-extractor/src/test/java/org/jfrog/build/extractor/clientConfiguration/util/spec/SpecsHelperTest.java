package org.jfrog.build.extractor.clientConfiguration.util.spec;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.apache.commons.io.FileUtils;
import org.jfrog.build.api.util.NullLog;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static org.testng.Assert.assertEquals;

/**
 * Unit tests for the SpecHelper.
 */
public class SpecsHelperTest {

    /**
     * Tests positive scenarios of parsing a String to FileSpec object.
     */
    @Test(dataProvider = "fileSpecWithValues")
    public void testGetUploadDownloadSpecFromStringPositiveCases(String specFileName, String aql, String pattern, String target,
                                                                 String props, String recursive, String flat, String regexp,
                                                                 String build) throws IOException {
        SpecsHelper specsHelper = new SpecsHelper(new NullLog());
        String specString = FileUtils.readFileToString(getFileFromResources("positiveTestSpecs/" + specFileName));
        FileSpec spec = specsHelper.getDownloadUploadSpec(specString).getFiles()[0];
        assertSpecParams(aql, pattern, target, props, recursive, flat, regexp, build, spec);
    }

    /**
     * Tests negative scenarios of parsing a String to FileSpec object.
     */
    @Test(dataProvider = "corruptedStringSpecs", expectedExceptions = {JsonMappingException.class, IllegalArgumentException.class})
    public void testGetUploadDownloadSpecFromStringNegativeCases(String spec) throws IOException {
        SpecsHelper specsHelper = new SpecsHelper(new NullLog());
        specsHelper.getDownloadUploadSpec(spec);
    }

    @DataProvider
    private static Object[][] corruptedStringSpecs() {
        return new Object[][]{
                {""},
                {"{}"},
                {"{ \"files\": []}"},
                {"{ \"files\": [{\"pattern\": \"foo\", \"target\": \"  \"}]}"},
                {"{ \"files\": [{\"aql\": \"foo\", \"target\": \"  \"}]}"},
                {"{ \"files\": [{\"target\": \"foo\", \"aql\": \"  \"}]}"},
                {"{ \"files\": [{\"target\": \"foo\", \"pattern\": \"  \"}]}"},
                {"{ \"files\": [{\"target\": \"foo\", \"pattern\": \"bar\", \"aql\": \"bar\"}]}"},
        };
    }

    /**
     * Tests positive scenarios of parsing a File to FileSpec object.
     */
    @Test(dataProvider = "fileSpecWithValues")
    public void testGetUploadDownloadSpecFromFilePositiveCases(String specFileName, String aql, String pattern, String target,
                                                               String props, String recursive, String flat, String regexp,
                                                               String build) throws IOException {
        SpecsHelper specsHelper = new SpecsHelper(new NullLog());
        File fileSpec = getFileFromResources("positiveTestSpecs/" + specFileName);
        FileSpec spec = specsHelper.getDownloadUploadSpec(fileSpec).getFiles()[0];
        assertSpecParams(aql, pattern, target, props, recursive, flat, regexp, build, spec);
    }

    @DataProvider
    private static Object[][] fileSpecWithValues() {
        return new Object[][]{
                // { Spec file name, aql, pattern, target, properties, recursive, flat, regexp, build }
                {ConstData.POSITIVE_TEST_FILE_SPEC[0], "{\"foo\":\"bar\",\"key\":\"val\"}", null, "simple_target", null, null, null, null, null},
                {ConstData.POSITIVE_TEST_FILE_SPEC[1], "{\"foo\":\"bar\",\"key\":\"val\"}", null, "simple_target", "props_val", "recursive_val", "flat_val", "regexp_val", "build_val"},
                {ConstData.POSITIVE_TEST_FILE_SPEC[2], "{\"foo\":\"bar\",\"key\":\"val\"}", null, "c:/some/windows/path", null, null, null, null, null},
                {ConstData.POSITIVE_TEST_FILE_SPEC[3], null, "simple_pattern", "simple_target", null, null, null, null, null},
                {ConstData.POSITIVE_TEST_FILE_SPEC[4], null, "simple_pattern", "simple_target", "props_val", "recursive_val", "flat_val", "regexp_val", "build_val"},
                {ConstData.POSITIVE_TEST_FILE_SPEC[5], null, "simple_pattern", "c:/some/windows/path", null, null, null, null, null},
                {ConstData.POSITIVE_TEST_FILE_SPEC[6], null, "c:/some/windows/path", "simple_target", null, null, null, null, null},
                {ConstData.POSITIVE_TEST_FILE_SPEC[7], null, "c:/some/windows/path", "simple_target", null, null, null, "True", null}
        };
    }

    /**
     * Tests negative scenarios of parsing a File to FileSpec object.
     */
    @Test(dataProvider = "corruptedFileSpecs", expectedExceptions = {JsonMappingException.class, IllegalArgumentException.class})
    public void testGetUploadDownloadSpecFromFileNegativeCases(String specFileName) throws IOException {
        SpecsHelper specsHelper = new SpecsHelper(new NullLog());
        File fileSpec = getFileFromResources("negativeTestSpecs/" + specFileName);
        specsHelper.getDownloadUploadSpec(fileSpec);
    }

    @DataProvider
    private static Object[][] corruptedFileSpecs() {
        return new Object[][]{
                {ConstData.NEGATIVE_TEST_FILE_SPEC[0]},
                {ConstData.NEGATIVE_TEST_FILE_SPEC[1]},
                {ConstData.NEGATIVE_TEST_FILE_SPEC[2]},
                {ConstData.NEGATIVE_TEST_FILE_SPEC[3]},
                {ConstData.NEGATIVE_TEST_FILE_SPEC[4]},
                {ConstData.NEGATIVE_TEST_FILE_SPEC[5]},
                {ConstData.NEGATIVE_TEST_FILE_SPEC[6]},
                {ConstData.NEGATIVE_TEST_FILE_SPEC[7]}
        };
    }

    private void assertSpecParams(String aql, String pattern, String target, String props, String recursive, String flat, String regexp, String build, FileSpec spec) throws IOException {
        assertEquals(spec.getAql(), aql);
        assertEquals(spec.getPattern(), pattern);
        assertEquals(spec.getTarget(), target);
        assertEquals(spec.getProps(), props);
        assertEquals(spec.getRecursive(), recursive);
        assertEquals(spec.getFlat(), flat);
        assertEquals(spec.getRegexp(), regexp);
        assertEquals(spec.getRecursive(), recursive);
        assertEquals(spec.getBuild(), build);
    }

    private File getFileFromResources(String relativePath) {
        return new File(this.getClass().getResource("/specs/unitTestSpecs/" + relativePath).getPath());
    }
}