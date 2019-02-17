package org.jfrog.build.extractor.clientConfiguration.util.spec;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.clientConfiguration.util.spec.validator.SearchBasedSpecValidator;
import org.jfrog.build.extractor.clientConfiguration.util.spec.validator.SpecsValidator;
import org.jfrog.build.extractor.clientConfiguration.util.spec.validator.UploadSpecValidator;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static org.testng.Assert.assertEquals;

/**
 * Unit tests for the SpecHelper.
 */
public class SpecsHelperTest {
    private static final String[] POSITIVE_FILE_SPEC;
    private static final String[] NEGATIVE_FILE_SPEC;
    private static final String[] POSITIVE_DOWNLOAD_FILE_SPEC;
    private static final String[] POSITIVE_UPLOAD_FILE_SPEC;
    private static final String[] NEGATIVE_DOWNLOAD_FILE_SPEC;
    private static final String[] NEGATIVE_UPLOAD_FILE_SPEC;
    private static SpecsValidator uploadSpecValidator = new UploadSpecValidator();
    private static SpecsValidator downloadSpecValidator = new SearchBasedSpecValidator();

    static {
        POSITIVE_FILE_SPEC = getPositiveTestFileSpecs();
        NEGATIVE_FILE_SPEC = getNegativeTestFileSpecs();
        POSITIVE_DOWNLOAD_FILE_SPEC = getPositiveDownloadTestFileSpecs();
        POSITIVE_UPLOAD_FILE_SPEC = getPositiveUploadTestFileSpecs();
        NEGATIVE_DOWNLOAD_FILE_SPEC = getNegativeDownloadTestFileSpecs();
        NEGATIVE_UPLOAD_FILE_SPEC = getNegativeUploadTestFileSpecs();
    }

    // Positive Tests
    /**
     * Tests positive scenarios of parsing a String to download FileSpec object.
     */
    @Test(dataProvider = "positiveDownloadSpecProvider")
    public void testPositiveDownloadSpecFromString(String specFileName, String aql, String pattern, String target,
                                                                 String props, String recursive, String flat, String regexp,
                                                                 String build) throws IOException {
        SpecsHelper specsHelper = new SpecsHelper(new NullLog());
        String specString = FileUtils.readFileToString(getFileFromResources("positiveTestSpecs/" + specFileName));
        FileSpec spec = specsHelper.getSpecFromString(specString, downloadSpecValidator).getFiles()[0];
        assertSpecParams(aql, pattern, target, props, recursive, flat, regexp, build, spec);
    }

    /**
     * Tests positive scenarios of parsing a String to upload FileSpec object.
     */
    @Test(dataProvider = "positiveUploadSpecProvider")
    public void testPositiveUploadSpecFromString(String specFileName, String aql, String pattern, String target,
                                                   String props, String recursive, String flat, String regexp,
                                                   String build) throws IOException {
        SpecsHelper specsHelper = new SpecsHelper(new NullLog());
        String specString = FileUtils.readFileToString(getFileFromResources("positiveTestSpecs/" + specFileName));
        FileSpec spec = specsHelper.getSpecFromString(specString, uploadSpecValidator).getFiles()[0];
        assertSpecParams(aql, pattern, target, props, recursive, flat, regexp, build, spec);
    }

    /**
     * Tests positive scenarios of parsing a File to download FileSpec object.
     */
    @Test(dataProvider = "positiveDownloadSpecProvider")
    public void testPositiveDownloadSpecFromFile (String specFileName, String aql, String pattern, String target,
                                                               String props, String recursive, String flat, String regexp,
                                                               String build) throws IOException {
        SpecsHelper specsHelper = new SpecsHelper(new NullLog());
        File fileSpec = getFileFromResources("positiveTestSpecs/" + specFileName);
        FileSpec spec = specsHelper.getSpecFromFile(fileSpec, downloadSpecValidator).getFiles()[0];
        assertSpecParams(aql, pattern, target, props, recursive, flat, regexp, build, spec);
    }

    /**
     * Tests positive scenarios of parsing a File to upload FileSpec object.
     */
    @Test(dataProvider = "positiveUploadSpecProvider")
    public void testPositiveUploadSpecFromFile (String specFileName, String aql, String pattern, String target,
                                                  String props, String recursive, String flat, String regexp,
                                                  String build) throws IOException {
        SpecsHelper specsHelper = new SpecsHelper(new NullLog());
        File fileSpec = getFileFromResources("positiveTestSpecs/" + specFileName);
        FileSpec spec = specsHelper.getSpecFromFile(fileSpec, uploadSpecValidator).getFiles()[0];
        assertSpecParams(aql, pattern, target, props, recursive, flat, regexp, build, spec);
    }

    // Negative Tests
    /**
     * Tests negative scenarios of parsing a String to download FileSpec object.
     */
    @Test(dataProvider = "negativeDownloadStringSpecProvider", expectedExceptions = {JsonMappingException.class, IllegalArgumentException.class})
    public void testNegativeDownloadSpecFromString (String spec) throws IOException {
        SpecsHelper specsHelper = new SpecsHelper(new NullLog());
        specsHelper.getSpecFromString(spec, downloadSpecValidator);
    }

    /**
     * Tests negative scenarios of parsing a String to upload FileSpec object.
     */
    @Test(dataProvider = "negativeUploadStringSpecProvider", expectedExceptions = {JsonMappingException.class, IllegalArgumentException.class})
    public void testNegativeUploadSpecFromString (String spec) throws IOException {
        SpecsHelper specsHelper = new SpecsHelper(new NullLog());
        specsHelper.getSpecFromString(spec, uploadSpecValidator);
    }

    /**
     * Tests negative scenarios of parsing a File to download FileSpec object.
     */
    @Test(dataProvider = "negativeDownloadFileSpecProvider", expectedExceptions = {JsonMappingException.class, IllegalArgumentException.class})
    public void testNegativeDownloadSpecFromFile(String specFileName) throws IOException {
        SpecsHelper specsHelper = new SpecsHelper(new NullLog());
        File fileSpec = getFileFromResources("negativeTestSpecs/" + specFileName);
        specsHelper.getSpecFromFile(fileSpec, downloadSpecValidator);
    }

    /**
     * Tests negative scenarios of parsing a File to upload FileSpec object.
     */
    @Test(dataProvider = "negativeUploadFileSpecProvider", expectedExceptions = {JsonMappingException.class, IllegalArgumentException.class})
    public void testNegativeUploadSpecFromFile(String specFileName) throws IOException {
        SpecsHelper specsHelper = new SpecsHelper(new NullLog());
        File fileSpec = getFileFromResources("negativeTestSpecs/" + specFileName);
        specsHelper.getSpecFromFile(fileSpec, uploadSpecValidator);
    }

    // Data Providers
    @DataProvider
    private static Object[][] positiveDownloadSpecProvider() {
        return (Object[][]) ArrayUtils.addAll(getPositiveDownloadUploadSpecsWithValues(), getPositiveDownloadSpecsWithValues());
    }

    @DataProvider
    private static Object[][] positiveUploadSpecProvider() {
        return (Object[][]) ArrayUtils.addAll(getPositiveDownloadUploadSpecsWithValues(), getPositiveUploadSpecsWithValues());
    }

    @DataProvider
    private static Object[][] negativeDownloadStringSpecProvider() {
        return (Object[][]) ArrayUtils.addAll(getNegativeDownloadUploadStringSpecs(), getNegativeDownloadStringSpecs());
    }

    @DataProvider
    private static Object[][] negativeDownloadFileSpecProvider() {
        return (Object[][]) ArrayUtils.addAll(getNegativeDownloadUploadSpecs(), getNegativeDownloadSpecs());
    }

    @DataProvider
    private static Object[][] negativeUploadStringSpecProvider() {
        return (Object[][]) ArrayUtils.addAll(getNegativeDownloadUploadStringSpecs(), getNegativeUploadStringSpecs());
    }

    @DataProvider
    private static Object[][] negativeUploadFileSpecProvider() {
        return (Object[][]) ArrayUtils.addAll(getNegativeDownloadUploadSpecs(), getNegativeUploadSpecs());
    }

    // Help Methods
    private static Object[][] getPositiveDownloadUploadSpecsWithValues() {
        return new Object[][]{
                // { Spec file name, aql, pattern, target, properties, recursive, flat, regexp, build }
                {POSITIVE_FILE_SPEC[0], null, "simple_pattern", "simple_target", null, null, null, null, null},
                {POSITIVE_FILE_SPEC[1], null, "simple_pattern", "simple_target", "props_val", "recursive_val", "flat_val", "regexp_val", "build_val"},
                {POSITIVE_FILE_SPEC[2], null, "simple_pattern", "c:/some/windows/path", null, null, null, null, null},
                {POSITIVE_FILE_SPEC[3], null, "c:/some/windows/path", "simple_target", null, null, null, null, null}
        };
    }

    private static Object[][] getPositiveDownloadSpecsWithValues() {
        return new Object[][]{
                // { Spec file name, aql, pattern, target, properties, recursive, flat, regexp, build }
                {POSITIVE_DOWNLOAD_FILE_SPEC[0], "{\"foo\":\"bar\",\"key\":\"val\"}", null, "simple_target", null, null, null, null, null},
                {POSITIVE_DOWNLOAD_FILE_SPEC[1], "{\"foo\":\"bar\",\"key\":\"val\"}", null, "simple_target", "props_val", "recursive_val", "flat_val", "regexp_val", "build_val"},
                {POSITIVE_DOWNLOAD_FILE_SPEC[2], "{\"foo\":\"bar\",\"key\":\"val\"}", null, "c:/some/windows/path", null, null, null, null, null},
                {POSITIVE_DOWNLOAD_FILE_SPEC[3], "{\"foo\":\"bar\",\"key\":\"val\"}", null, null, null, null, null, null, null}
        };
    }

    private static Object[][] getPositiveUploadSpecsWithValues() {
        return new Object[][]{
                // { Spec file name, aql, pattern, target, properties, recursive, flat, regexp, build }
                {POSITIVE_UPLOAD_FILE_SPEC[0], null, "c:/some/windows/path", "simple_target", null, null, null, "True", null}
        };
    }

    private static Object[][] getNegativeDownloadUploadSpecs() {
        return new Object[][]{
                {NEGATIVE_FILE_SPEC[0]},
                {NEGATIVE_FILE_SPEC[1]},
                {NEGATIVE_FILE_SPEC[2]},
                {NEGATIVE_FILE_SPEC[3]},
                {NEGATIVE_FILE_SPEC[4]}
        };
    }

    private static Object[][] getNegativeDownloadSpecs() {
        return new Object[][]{
                {NEGATIVE_DOWNLOAD_FILE_SPEC[0]}
        };
    }

    private static Object[][] getNegativeUploadSpecs() {
        return new Object[][]{
                {NEGATIVE_UPLOAD_FILE_SPEC[0]},
                {NEGATIVE_UPLOAD_FILE_SPEC[1]}
        };
    }

    private static Object[][] getNegativeDownloadUploadStringSpecs() {
        return new Object[][]{
                {""},
                {"{}"},
                {"{ \"files\": []}"},
                {"{ \"files\": [{\"target\": \"foo\", \"pattern\": \"  \"}]}"},
                {"{ \"files\": [{\"target\": \"foo\", \"pattern\": \"bar\", \"aql\": \"bar\"}]}"},
        };
    }

    private static Object[][] getNegativeDownloadStringSpecs() {
        return new Object[][]{
                {"{ \"files\": [{\"target\": \"foo\", \"aql\": \"  \"}]}"}
        };
    }

    private static Object[][] getNegativeUploadStringSpecs() {
        return new Object[][]{
                {"{ \"files\": [{\"pattern\": \"foo\", \"target\": \"  \"}]}"},
                {"{ \"files\": [{\"aql\": \"foo\", \"target\": \"  \"}]}"}
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

    // Static Resources
    private static String[] getPositiveDownloadTestFileSpecs() {
        return new String[]{
                "download/positiveDownloadSpecTest1.json",
                "download/positiveDownloadSpecTest2.json",
                "download/positiveDownloadSpecTest3.json",
                "download/positiveDownloadSpecTest4.json"
        };
    }

    private static String[] getPositiveUploadTestFileSpecs() {
        return new String[]{
                "upload/positiveUploadSpecTest1.json"
        };
    }

    private static String[] getNegativeDownloadTestFileSpecs() {
        return new String[]{
                "download/negativeDownloadSpecTest1.json"
        };
    }

    private static String[] getNegativeUploadTestFileSpecs() {
        return new String[]{
                "upload/negativeUploadSpecTest1.json",
                "upload/negativeUploadSpecTest2.json"
        };
    }

    private static String[] getPositiveTestFileSpecs() {
        return new String[]{
                "positiveDownloadUploadSpecTest1.json",
                "positiveDownloadUploadSpecTest2.json",
                "positiveDownloadUploadSpecTest3.json",
                "positiveDownloadUploadSpecTest4.json",
                "positiveDownloadUploadSpecTest5.json"
        };
    }

    private static String[] getNegativeTestFileSpecs() {
        return new String[]{
                "negativeDownloadUploadSpecTest1.json",
                "negativeDownloadUploadSpecTest2.json",
                "negativeDownloadUploadSpecTest3.json",
                "negativeDownloadUploadSpecTest4.json",
                "negativeDownloadUploadSpecTest5.json"
        };
    }
}