package org.jfrog.build.extractor.clientConfiguration.util.spec;

/**
 * Created by diman on 29/03/2017.
 */
class ConstData {

    static final String[] UPLOAD_RESULTS;
    static final String[] UPLOAD_PARENTHESIS_RESULTS;
    static final String[] UPLOAD_REPO_ROOT_RESULTS;
    static final String[] UPLOAD_EXCLUDE_RESULTS;
    static final String[] UPLOAD_EXCLUDE_FROM_PATH_RESULTS;
    static final String[] DOWNLOAD_RESULTS;
    static final String[] DOWNLOAD_EXPLODE_RESULTS;
    static final String[] DOWNLOAD_EXCLUDE_RESULTS;
    static final String[] DOWNLOAD_EXCLUDE_FROM_PATH;
    static final String[] POSITIVE_TEST_FILE_SPEC;
    static final String[] NEGATIVE_TEST_FILE_SPEC;

    static {
        UPLOAD_RESULTS = getExpectedUploadResults();
        UPLOAD_PARENTHESIS_RESULTS = getUploadParenthesisResults();
        UPLOAD_REPO_ROOT_RESULTS = getUploadRepoRootResults();
        UPLOAD_EXCLUDE_RESULTS = getExpectedUploadExcludeResults();
        UPLOAD_EXCLUDE_FROM_PATH_RESULTS = getExpectedUploadExcludeFromPathResults();
        DOWNLOAD_RESULTS = getExpectedDownloadResults();
        DOWNLOAD_EXPLODE_RESULTS = getExpectedExplodeDownloadResults();
        DOWNLOAD_EXCLUDE_RESULTS = getExpectedExcludeDownloadResults();
        DOWNLOAD_EXCLUDE_FROM_PATH = getExpectedExcludeFromPathDownloadResults();
        POSITIVE_TEST_FILE_SPEC = getPositiveTestFileSpecs();
        NEGATIVE_TEST_FILE_SPEC = getNegativeTestFileSpecs();
    }

    private static String[] getExpectedUploadResults() {
        return new String[]{
                "1_flat_recursive_wildcard/a1.in",
                "1_flat_recursive_wildcard/a2.in",
                "1_flat_recursive_wildcard/a3.in",
                "1_flat_recursive_wildcard/b1.in",
                "1_flat_recursive_wildcard/b2.in",
                "1_flat_recursive_wildcard/b3.in",
                "1_flat_recursive_wildcard/c1.in",
                "1_flat_recursive_wildcard/c2.in",
                "1_flat_recursive_wildcard/c3.in",
                "1_flat_recursive_wildcard/ant-antlr-1.6.5.zip",

                "2_flat_recursive_regexp/a1.in",
                "2_flat_recursive_regexp/a2.in",
                "2_flat_recursive_regexp/a3.in",
                "2_flat_recursive_regexp/b1.in",
                "2_flat_recursive_regexp/b2.in",
                "2_flat_recursive_regexp/b3.in",
                "2_flat_recursive_regexp/c1.in",
                "2_flat_recursive_regexp/c2.in",
                "2_flat_recursive_regexp/c3.in",
                "2_flat_recursive_regexp/ant-antlr-1.6.5.zip",

                "3_defaults_flat_recursive/a1.in",
                "3_defaults_flat_recursive/a2.in",
                "3_defaults_flat_recursive/a3.in",
                "3_defaults_flat_recursive/b1.in",
                "3_defaults_flat_recursive/b2.in",
                "3_defaults_flat_recursive/b3.in",
                "3_defaults_flat_recursive/c1.in",
                "3_defaults_flat_recursive/c2.in",
                "3_defaults_flat_recursive/c3.in",
                "3_defaults_flat_recursive/ant-antlr-1.6.5.zip",

                "4_3-only_wildcard_recursive/a3.in",
                "4_3-only_wildcard_recursive/b3.in",
                "4_3-only_wildcard_recursive/c3.in",

                "5_3-only_regexp_recursive/a3.in",
                "5_3-only_regexp_recursive/b3.in",
                "5_3-only_regexp_recursive/c3.in",

                "6_flat_nonrecursive_wildcard/a1.in",
                "6_flat_nonrecursive_wildcard/a2.in",
                "6_flat_nonrecursive_wildcard/a3.in",

                "7_flat_nonrecursive_regexp/a1.in",
                "7_flat_nonrecursive_regexp/a2.in",
                "7_flat_nonrecursive_regexp/a3.in",

                "8_nonflat_recursive_wildcard/files/a/a1.in",
                "8_nonflat_recursive_wildcard/files/a/a2.in",
                "8_nonflat_recursive_wildcard/files/a/a3.in",
                "8_nonflat_recursive_wildcard/files/a/b/b1.in",
                "8_nonflat_recursive_wildcard/files/a/b/b2.in",
                "8_nonflat_recursive_wildcard/files/a/b/b3.in",
                "8_nonflat_recursive_wildcard/files/a/b/c/c1.in",
                "8_nonflat_recursive_wildcard/files/a/b/c/c2.in",
                "8_nonflat_recursive_wildcard/files/a/b/c/c3.in",
                "8_nonflat_recursive_wildcard/files/a/b/c/ant-antlr-1.6.5.zip",

                "9_nonflat_recursive_regexp/files/a/a1.in",
                "9_nonflat_recursive_regexp/files/a/a2.in",
                "9_nonflat_recursive_regexp/files/a/a3.in",
                "9_nonflat_recursive_regexp/files/a/b/b1.in",
                "9_nonflat_recursive_regexp/files/a/b/b2.in",
                "9_nonflat_recursive_regexp/files/a/b/b3.in",
                "9_nonflat_recursive_regexp/files/a/b/c/c1.in",
                "9_nonflat_recursive_regexp/files/a/b/c/c2.in",
                "9_nonflat_recursive_regexp/files/a/b/c/c3.in",
                "9_nonflat_recursive_regexp/files/a/b/c/ant-antlr-1.6.5.zip",

                "10_nonflat_nonrecursive_wildcard/files/a/a1.in",
                "10_nonflat_nonrecursive_wildcard/files/a/a2.in",
                "10_nonflat_nonrecursive_wildcard/files/a/a3.in",

                "11_nonflat_nonrecursive_regexp/files/a/a1.in",
                "11_nonflat_nonrecursive_regexp/files/a/a2.in",
                "11_nonflat_nonrecursive_regexp/files/a/a3.in",

                "12_simple_flat/a1.in",

                "13_simple_nonflat/files/a/a1.in",

                "14_rename_wildcard_flat/a1.out",

                "15_rename_regexp_nonflat/a1.out",

                "16_pattern_placeholder_flat_nonrecursive/a/a1.in",
                "16_pattern_placeholder_flat_nonrecursive/a/a2.in",
                "16_pattern_placeholder_flat_nonrecursive/a/a3.in",

                "17_pattern_placeholder_nonflat_nonrecursive_wildcard/a/a1.in",
                "17_pattern_placeholder_nonflat_nonrecursive_wildcard/a/a2.in",
                "17_pattern_placeholder_nonflat_nonrecursive_wildcard/a/a3.in",

                "18_props_wildcard/a2.in",
                "18_props_wildcard/b2.in",
                "18_props_wildcard/c2.in",

                "19_props_regexp/a2.in",
                "19_props_regexp/b2.in",
                "19_props_regexp/c2.in",

                "20_defaults_recursive_flat_regexp/a1.in",
                "20_defaults_recursive_flat_regexp/a2.in",
                "20_defaults_recursive_flat_regexp/a3.in",
                "20_defaults_recursive_flat_regexp/b1.in",
                "20_defaults_recursive_flat_regexp/b2.in",
                "20_defaults_recursive_flat_regexp/b3.in",
                "20_defaults_recursive_flat_regexp/c1.in",
                "20_defaults_recursive_flat_regexp/c2.in",
                "20_defaults_recursive_flat_regexp/c3.in",
                "20_defaults_recursive_flat_regexp/ant-antlr-1.6.5.zip",

                "21_upload-to-existing/a1.in",
                "21_upload-to-existing/a2.in",
                "21_upload-to-existing/a3.in",
                "21_upload-to-existing/b1.in",
                "21_upload-to-existing/b2.in",
                "21_upload-to-existing/b3.in",
                "21_upload-to-existing/c1.in",
                "21_upload-to-existing/c2.in",
                "21_upload-to-existing/c3.in",
                "21_upload-to-existing/ant-antlr-1.6.5.zip",

                "22_regexp-a1-only/a1.in",

                "23_nonflat-regex-all-1-recursive/files/a/a1.in",
                "23_nonflat-regex-all-1-recursive/files/a/b/b1.in",
                "23_nonflat-regex-all-1-recursive/files/a/b/c/c1.in",
                "23_nonflat-regex-all-1-recursive/files/a/b/c/ant-antlr-1.6.5.zip",

                "24_regex_all-1_nonrecursive_full-path/a1.in",

                "25_wildcard_all-1_nonrecursive_full-path/a1.in",

                "26_regexp_nonflat_negativeLookAhaed_not-c/files/a/a1.in",
                "26_regexp_nonflat_negativeLookAhaed_not-c/files/a/a2.in",
                "26_regexp_nonflat_negativeLookAhaed_not-c/files/a/a3.in",
                "26_regexp_nonflat_negativeLookAhaed_not-c/files/a/b/b1.in",
                "26_regexp_nonflat_negativeLookAhaed_not-c/files/a/b/b2.in",
                "26_regexp_nonflat_negativeLookAhaed_not-c/files/a/b/b3.in",

                "27_pattern_placeholder_nonflat_nonrecursive_regexp/a/a1.in",
                "27_pattern_placeholder_nonflat_nonrecursive_regexp/a/a2.in",
                "27_pattern_placeholder_nonflat_nonrecursive_regexp/a/a3.in",

                "28_simple_flat_full-path/a1.in",

                "29_pattern_bigPlaceholder_flat_nonrecursive_regexp/files/a/a1.in",
                "29_pattern_bigPlaceholder_flat_nonrecursive_regexp/files/a/a2.in",
                "29_pattern_bigPlaceholder_flat_nonrecursive_regexp/files/a/a3.in",

                "30_pattern_bigPlaceholder_flat_nonrecursive_wildcard/files/a/a1.in",
                "30_pattern_bigPlaceholder_flat_nonrecursive_wildcard/files/a/a2.in",
                "30_pattern_bigPlaceholder_flat_nonrecursive_wildcard/files/a/a3.in",

                "31_nonflat_nonrecursive_wildcard_full-path_placeholder/a1.in",
                "31_nonflat_nonrecursive_wildcard_full-path_placeholder/a2.in",
                "31_nonflat_nonrecursive_wildcard_full-path_placeholder/a3.in",

                "32_flat_nonrecursive_regexp_full-path_placeholder/a1.in",
                "32_flat_nonrecursive_regexp_full-path_placeholder/a2.in",
                "32_flat_nonrecursive_regexp_full-path_placeholder/a3.in"
        };
    }

    private static String[] getExpectedUploadExcludeResults() {
        return new String[]{
                "4_pattern_exclude_abc/a1.in",
                "4_pattern_exclude_abc/a2.in",
                "4_pattern_exclude_abc/a3.in",
                "4_pattern_exclude_abc/b2.in",
                "4_pattern_exclude_abc/b3.in",
                "4_pattern_exclude_abc/c2.in",
                "4_pattern_exclude_abc/c3.in",
        };
    }

    private static String[] getExpectedUploadExcludeFromPathResults() {
        return new String[]{
                "4_pattern_exclude_abc/b1.in",
                "4_pattern_exclude_abc/c1.in",
        };
    }

    private static String[] getExpectedDownloadResults() {
        return new String[]{
                "/tmp/bi_test_space/download-test/1_pattern_flat_recursive/a1.in",
                "/tmp/bi_test_space/download-test/1_pattern_flat_recursive/a2.in",
                "/tmp/bi_test_space/download-test/1_pattern_flat_recursive/a3.in",
                "/tmp/bi_test_space/download-test/1_pattern_flat_recursive/ant-antlr-1.6.5.zip",
                "/tmp/bi_test_space/download-test/1_pattern_flat_recursive/b1.in",
                "/tmp/bi_test_space/download-test/1_pattern_flat_recursive/b2.in",
                "/tmp/bi_test_space/download-test/1_pattern_flat_recursive/b3.in",
                "/tmp/bi_test_space/download-test/1_pattern_flat_recursive/c1.in",
                "/tmp/bi_test_space/download-test/1_pattern_flat_recursive/c2.in",
                "/tmp/bi_test_space/download-test/1_pattern_flat_recursive/c3.in",

                "/tmp/bi_test_space/download-test/2_pattern_3_only_flat_recursive/a3.in",
                "/tmp/bi_test_space/download-test/2_pattern_3_only_flat_recursive/c3.in",
                "/tmp/bi_test_space/download-test/2_pattern_3_only_flat_recursive/b3.in",

                "/tmp/bi_test_space/download-test/3_pattern_flat_nonrecursive/a1.in",
                "/tmp/bi_test_space/download-test/3_pattern_flat_nonrecursive/a2.in",
                "/tmp/bi_test_space/download-test/3_pattern_flat_nonrecursive/a3.in",

                "/tmp/bi_test_space/download-test/4_pattern_nonflat_recursive/bi_test_space/download-test/a/a1.in",
                "/tmp/bi_test_space/download-test/4_pattern_nonflat_recursive/bi_test_space/download-test/a/a2.in",
                "/tmp/bi_test_space/download-test/4_pattern_nonflat_recursive/bi_test_space/download-test/a/a3.in",
                "/tmp/bi_test_space/download-test/4_pattern_nonflat_recursive/bi_test_space/download-test/a/b/b1.in",
                "/tmp/bi_test_space/download-test/4_pattern_nonflat_recursive/bi_test_space/download-test/a/b/b2.in",
                "/tmp/bi_test_space/download-test/4_pattern_nonflat_recursive/bi_test_space/download-test/a/b/b3.in",
                "/tmp/bi_test_space/download-test/4_pattern_nonflat_recursive/bi_test_space/download-test/a/b/c/ant-antlr-1.6.5.zip",
                "/tmp/bi_test_space/download-test/4_pattern_nonflat_recursive/bi_test_space/download-test/a/b/c/c1.in",
                "/tmp/bi_test_space/download-test/4_pattern_nonflat_recursive/bi_test_space/download-test/a/b/c/c2.in",
                "/tmp/bi_test_space/download-test/4_pattern_nonflat_recursive/bi_test_space/download-test/a/b/c/c3.in",

                "/tmp/bi_test_space/download-test/5_pattern_nonflat_nonrecursive/bi_test_space/download-test/a/a1.in",
                "/tmp/bi_test_space/download-test/5_pattern_nonflat_nonrecursive/bi_test_space/download-test/a/a2.in",
                "/tmp/bi_test_space/download-test/5_pattern_nonflat_nonrecursive/bi_test_space/download-test/a/a3.in",

                "/tmp/bi_test_space/download-test/6_pattern_flat_nonrecursive/a1.in",
                "/tmp/bi_test_space/download-test/6_pattern_flat_nonrecursive/a2.in",
                "/tmp/bi_test_space/download-test/6_pattern_flat_nonrecursive/a3.in",

                "/tmp/bi_test_space/download-test/7_pattern_simple_flat_nonrecursive/a1.in",

                "/tmp/bi_test_space/download-test/8_pattern_simple_nonflat_nonrecursive/bi_test_space/download-test/a/a1.in",

                "/tmp/bi_test_space/download-test/9_pattern_simple_nonflat_recursive/bi_test_space/download-test/a/a1.in",

                "/tmp/bi_test_space/download-test/10_pattern_rename_flat_nonrecursive/a1.out",

                "/tmp/bi_test_space/download-test/11_pattern_rename_nonflat_nonrecursive/bi_test_space/download-test/a/a1.out",

                "/tmp/bi_test_space/download-test/12_pattern_rename_nonflat_recursive/bi_test_space/download-test/a/a1.out",

                "/tmp/bi_test_space/download-test/13_pattern_simple_placeholder_flat_nonrecursive/a/a1.in",

                "/tmp/bi_test_space/download-test/14_pattern_placeholder_flat_recursive/a/a1.in",
                "/tmp/bi_test_space/download-test/14_pattern_placeholder_flat_recursive/a/a2.in",
                "/tmp/bi_test_space/download-test/14_pattern_placeholder_flat_recursive/a/a3.in",
                "/tmp/bi_test_space/download-test/14_pattern_placeholder_flat_recursive/a/b/b1.in",
                "/tmp/bi_test_space/download-test/14_pattern_placeholder_flat_recursive/a/b/b2.in",
                "/tmp/bi_test_space/download-test/14_pattern_placeholder_flat_recursive/a/b/b3.in",
                "/tmp/bi_test_space/download-test/14_pattern_placeholder_flat_recursive/a/b/c/c1.in",
                "/tmp/bi_test_space/download-test/14_pattern_placeholder_flat_recursive/a/b/c/c2.in",
                "/tmp/bi_test_space/download-test/14_pattern_placeholder_flat_recursive/a/b/c/c3.in",

                "/tmp/bi_test_space/download-test/15_pattern_placeholder_nonflat_recursive/a/b/bi_test_space/download-test/a/b/b1.in",
                "/tmp/bi_test_space/download-test/15_pattern_placeholder_nonflat_recursive/a/b/bi_test_space/download-test/a/b/b2.in",
                "/tmp/bi_test_space/download-test/15_pattern_placeholder_nonflat_recursive/a/b/bi_test_space/download-test/a/b/b3.in",
                "/tmp/bi_test_space/download-test/15_pattern_placeholder_nonflat_recursive/a/b/c/bi_test_space/download-test/a/b/c/c1.in",
                "/tmp/bi_test_space/download-test/15_pattern_placeholder_nonflat_recursive/a/b/c/bi_test_space/download-test/a/b/c/c2.in",
                "/tmp/bi_test_space/download-test/15_pattern_placeholder_nonflat_recursive/a/b/c/bi_test_space/download-test/a/b/c/c3.in",
                "/tmp/bi_test_space/download-test/15_pattern_placeholder_nonflat_recursive/a/bi_test_space/download-test/a/a1.in",
                "/tmp/bi_test_space/download-test/15_pattern_placeholder_nonflat_recursive/a/bi_test_space/download-test/a/a2.in",
                "/tmp/bi_test_space/download-test/15_pattern_placeholder_nonflat_recursive/a/bi_test_space/download-test/a/a3.in",

                "/tmp/bi_test_space/download-test/16_pattern_properties/bi_test_space/download-test/a/a2.in",
                "/tmp/bi_test_space/download-test/16_pattern_properties/bi_test_space/download-test/a/b/b2.in",
                "/tmp/bi_test_space/download-test/16_pattern_properties/bi_test_space/download-test/a/b/c/c2.in",

                "/tmp/bi_test_space/download-test/17_pattern_defaults_recursive_nonFlat/bi_test_space/download-test/a/a1.in",
                "/tmp/bi_test_space/download-test/17_pattern_defaults_recursive_nonFlat/bi_test_space/download-test/a/a2.in",
                "/tmp/bi_test_space/download-test/17_pattern_defaults_recursive_nonFlat/bi_test_space/download-test/a/a3.in",
                "/tmp/bi_test_space/download-test/17_pattern_defaults_recursive_nonFlat/bi_test_space/download-test/a/b/b1.in",
                "/tmp/bi_test_space/download-test/17_pattern_defaults_recursive_nonFlat/bi_test_space/download-test/a/b/b2.in",
                "/tmp/bi_test_space/download-test/17_pattern_defaults_recursive_nonFlat/bi_test_space/download-test/a/b/b3.in",
                "/tmp/bi_test_space/download-test/17_pattern_defaults_recursive_nonFlat/bi_test_space/download-test/a/b/c/ant-antlr-1.6.5.zip",
                "/tmp/bi_test_space/download-test/17_pattern_defaults_recursive_nonFlat/bi_test_space/download-test/a/b/c/c1.in",
                "/tmp/bi_test_space/download-test/17_pattern_defaults_recursive_nonFlat/bi_test_space/download-test/a/b/c/c2.in",
                "/tmp/bi_test_space/download-test/17_pattern_defaults_recursive_nonFlat/bi_test_space/download-test/a/b/c/c3.in",

                "/tmp/bi_test_space/download-test/18_aql_flat_recursive/a1.in",
                "/tmp/bi_test_space/download-test/18_aql_flat_recursive/a2.in",
                "/tmp/bi_test_space/download-test/18_aql_flat_recursive/a3.in",
                "/tmp/bi_test_space/download-test/18_aql_flat_recursive/ant-antlr-1.6.5.zip",
                "/tmp/bi_test_space/download-test/18_aql_flat_recursive/b1.in",
                "/tmp/bi_test_space/download-test/18_aql_flat_recursive/b2.in",
                "/tmp/bi_test_space/download-test/18_aql_flat_recursive/b3.in",
                "/tmp/bi_test_space/download-test/18_aql_flat_recursive/c1.in",
                "/tmp/bi_test_space/download-test/18_aql_flat_recursive/c2.in",
                "/tmp/bi_test_space/download-test/18_aql_flat_recursive/c3.in",

                "/tmp/bi_test_space/download-test/19_aql_nonflat_recursive/bi_test_space/download-test/a/a1.in",
                "/tmp/bi_test_space/download-test/19_aql_nonflat_recursive/bi_test_space/download-test/a/a2.in",
                "/tmp/bi_test_space/download-test/19_aql_nonflat_recursive/bi_test_space/download-test/a/a3.in",
                "/tmp/bi_test_space/download-test/19_aql_nonflat_recursive/bi_test_space/download-test/a/b/b1.in",
                "/tmp/bi_test_space/download-test/19_aql_nonflat_recursive/bi_test_space/download-test/a/b/b2.in",
                "/tmp/bi_test_space/download-test/19_aql_nonflat_recursive/bi_test_space/download-test/a/b/b3.in",
                "/tmp/bi_test_space/download-test/19_aql_nonflat_recursive/bi_test_space/download-test/a/b/c/ant-antlr-1.6.5.zip",
                "/tmp/bi_test_space/download-test/19_aql_nonflat_recursive/bi_test_space/download-test/a/b/c/c1.in",
                "/tmp/bi_test_space/download-test/19_aql_nonflat_recursive/bi_test_space/download-test/a/b/c/c2.in",
                "/tmp/bi_test_space/download-test/19_aql_nonflat_recursive/bi_test_space/download-test/a/b/c/c3.in",

                "/tmp/bi_test_space/download-test/20_aql_flat_nonrecursive/a1.in",
                "/tmp/bi_test_space/download-test/20_aql_flat_nonrecursive/a2.in",
                "/tmp/bi_test_space/download-test/20_aql_flat_nonrecursive/a3.in",
                "/tmp/bi_test_space/download-test/20_aql_flat_nonrecursive/ant-antlr-1.6.5.zip",
                "/tmp/bi_test_space/download-test/20_aql_flat_nonrecursive/b1.in",
                "/tmp/bi_test_space/download-test/20_aql_flat_nonrecursive/b2.in",
                "/tmp/bi_test_space/download-test/20_aql_flat_nonrecursive/b3.in",
                "/tmp/bi_test_space/download-test/20_aql_flat_nonrecursive/c1.in",
                "/tmp/bi_test_space/download-test/20_aql_flat_nonrecursive/c2.in",
                "/tmp/bi_test_space/download-test/20_aql_flat_nonrecursive/c3.in",

                "/tmp/bi_test_space/download-test/21_aql_nonflat_nonrecursive/bi_test_space/download-test/a/a1.in",
                "/tmp/bi_test_space/download-test/21_aql_nonflat_nonrecursive/bi_test_space/download-test/a/a2.in",
                "/tmp/bi_test_space/download-test/21_aql_nonflat_nonrecursive/bi_test_space/download-test/a/a3.in",
                "/tmp/bi_test_space/download-test/21_aql_nonflat_nonrecursive/bi_test_space/download-test/a/b/b1.in",
                "/tmp/bi_test_space/download-test/21_aql_nonflat_nonrecursive/bi_test_space/download-test/a/b/b2.in",
                "/tmp/bi_test_space/download-test/21_aql_nonflat_nonrecursive/bi_test_space/download-test/a/b/b3.in",
                "/tmp/bi_test_space/download-test/21_aql_nonflat_nonrecursive/bi_test_space/download-test/a/b/c/ant-antlr-1.6.5.zip",
                "/tmp/bi_test_space/download-test/21_aql_nonflat_nonrecursive/bi_test_space/download-test/a/b/c/c1.in",
                "/tmp/bi_test_space/download-test/21_aql_nonflat_nonrecursive/bi_test_space/download-test/a/b/c/c2.in",
                "/tmp/bi_test_space/download-test/21_aql_nonflat_nonrecursive/bi_test_space/download-test/a/b/c/c3.in",

                "/tmp/bi_test_space/download-test/22_aql_properties_nonflat_nonrecursive/bi_test_space/download-test/a/a1.in", // TODO Bug https://www.jfrog.com/jira/browse/BI-404
                "/tmp/bi_test_space/download-test/22_aql_properties_nonflat_nonrecursive/bi_test_space/download-test/a/a2.in",
                "/tmp/bi_test_space/download-test/22_aql_properties_nonflat_nonrecursive/bi_test_space/download-test/a/a3.in",
                "/tmp/bi_test_space/download-test/22_aql_properties_nonflat_nonrecursive/bi_test_space/download-test/a/b/b1.in",
                "/tmp/bi_test_space/download-test/22_aql_properties_nonflat_nonrecursive/bi_test_space/download-test/a/b/b2.in",
                "/tmp/bi_test_space/download-test/22_aql_properties_nonflat_nonrecursive/bi_test_space/download-test/a/b/b3.in",
                "/tmp/bi_test_space/download-test/22_aql_properties_nonflat_nonrecursive/bi_test_space/download-test/a/b/c/ant-antlr-1.6.5.zip",
                "/tmp/bi_test_space/download-test/22_aql_properties_nonflat_nonrecursive/bi_test_space/download-test/a/b/c/c1.in",
                "/tmp/bi_test_space/download-test/22_aql_properties_nonflat_nonrecursive/bi_test_space/download-test/a/b/c/c2.in",
                "/tmp/bi_test_space/download-test/22_aql_properties_nonflat_nonrecursive/bi_test_space/download-test/a/b/c/c3.in"
        };
    }

    private static String[] getExpectedExplodeDownloadResults() {
        return new String[]{
                "1_flat_recursive_wildcard/ant-antlr-1.6.5.jar",
                "20_defaults_recursive_flat_regexp/ant-antlr-1.6.5.jar",
                "21_upload-to-existing/ant-antlr-1.6.5.jar",
                "23_nonflat-regex-all-1-recursive/files/a/b/c/ant-antlr-1.6.5.jar",
                "2_flat_recursive_regexp/ant-antlr-1.6.5.jar",
                "3_defaults_flat_recursive/ant-antlr-1.6.5.jar",
                "8_nonflat_recursive_wildcard/files/a/b/c/ant-antlr-1.6.5.jar",
                "9_nonflat_recursive_regexp/files/a/b/c/ant-antlr-1.6.5.jar"
        };
    }

    private static String[] getExpectedExcludeDownloadResults() {
        return new String[]{
                "1_pattern_exclude_a/a2.in",
                "1_pattern_exclude_a/a3.in",
                "3_pattern_exclude_c/c3.in"
        };
    }

    private static String[] getExpectedExcludeFromPathDownloadResults() {
        return new String[]{
                "1_pattern_exclude_a/a2.in",
                "1_pattern_exclude_a/a3.in",
                "1_pattern_exclude_a/b2.in",
                "1_pattern_exclude_a/b3.in",
                "1_pattern_exclude_a/c1.in",
                "1_pattern_exclude_a/c2.in",
                "1_pattern_exclude_a/c3.in"
        };
    }

    private static String[] getPositiveTestFileSpecs() {
        return new String[]{
                "downloadUploadSpecTest1.json",
                "downloadUploadSpecTest2.json",
                "downloadUploadSpecTest3.json",
                "downloadUploadSpecTest4.json",
                "downloadUploadSpecTest5.json",
                "downloadUploadSpecTest6.json",
                "downloadUploadSpecTest7.json",
                "downloadUploadSpecTest8.json"
        };
    }

    private static String[] getNegativeTestFileSpecs() {
        return new String[]{
                "downloadUploadSpecTest1.json",
                "downloadUploadSpecTest2.json",
                "downloadUploadSpecTest3.json",
                "downloadUploadSpecTest4.json",
                "downloadUploadSpecTest5.json",
                "downloadUploadSpecTest6.json",
                "downloadUploadSpecTest7.json",
                "downloadUploadSpecTest8.json"
        };
    }

    private static String[] getUploadParenthesisResults() {
        return new String[]{
                "33_flat_recursive_wildcard_parenthesis/a1.in",
                "33_flat_recursive_wildcard_parenthesis/a2.in",
                "33_flat_recursive_wildcard_parenthesis/a3.in",
                "33_flat_recursive_wildcard_parenthesis/ant-antlr-1.6.5.zip",
                "33_flat_recursive_wildcard_parenthesis/b1.in",
                "33_flat_recursive_wildcard_parenthesis/b2.in",
                "33_flat_recursive_wildcard_parenthesis/b3.in",
                "33_flat_recursive_wildcard_parenthesis/c1.in",
                "33_flat_recursive_wildcard_parenthesis/c2.in",
                "33_flat_recursive_wildcard_parenthesis/c3.in",

                "34_flat_recursive_regexp_parenthesis/a1.in",
                "34_flat_recursive_regexp_parenthesis/a2.in",
                "34_flat_recursive_regexp_parenthesis/a3.in",
                "34_flat_recursive_regexp_parenthesis/ant-antlr-1.6.5.zip",
                "34_flat_recursive_regexp_parenthesis/b1.in",
                "34_flat_recursive_regexp_parenthesis/b2.in",
                "34_flat_recursive_regexp_parenthesis/b3.in",
                "34_flat_recursive_regexp_parenthesis/c1.in",
                "34_flat_recursive_regexp_parenthesis/c2.in",
                "34_flat_recursive_regexp_parenthesis/c3.in",

                "35_flat_recursive_regexp_parenthesisFullPath/files/a/a1.in",
                "35_flat_recursive_regexp_parenthesisFullPath/files/a/a2.in",
                "35_flat_recursive_regexp_parenthesisFullPath/files/a/a3.in",
                "35_flat_recursive_regexp_parenthesisFullPath/files/a/b/b1.in",
                "35_flat_recursive_regexp_parenthesisFullPath/files/a/b/b2.in",
                "35_flat_recursive_regexp_parenthesisFullPath/files/a/b/b3.in",
                "35_flat_recursive_regexp_parenthesisFullPath/files/a/b/c/ant-antlr-1.6.5.zip",
                "35_flat_recursive_regexp_parenthesisFullPath/files/a/b/c/c1.in",
                "35_flat_recursive_regexp_parenthesisFullPath/files/a/b/c/c2.in",
                "35_flat_recursive_regexp_parenthesisFullPath/files/a/b/c/c3.in",

                "36_flat_recursive_regexp_pattern_parenthesis/a1.in",
                "36_flat_recursive_regexp_pattern_parenthesis/a2.in",
                "36_flat_recursive_regexp_pattern_parenthesis/a3.in",
                "36_flat_recursive_regexp_pattern_parenthesis/ant-antlr-1.6.5.zip",
                "36_flat_recursive_regexp_pattern_parenthesis/b1.in",
                "36_flat_recursive_regexp_pattern_parenthesis/b2.in",
                "36_flat_recursive_regexp_pattern_parenthesis/b3.in",
                "36_flat_recursive_regexp_pattern_parenthesis/c1.in",
                "36_flat_recursive_regexp_pattern_parenthesis/c2.in",
                "36_flat_recursive_regexp_pattern_parenthesis/c3.in"
        };
    }

    private static String[] getUploadRepoRootResults() {
        return new String[]{
                "files/a/a1.in",
                "a1.in"
        };
    }
}