package org.jfrog.build.extractor.clientConfiguration.util.spec;

/**
 * Created by diman on 29/03/2017.
 */
class ConstData {

    static final String[] POSITIVE_TEST_FILE_SPEC;
    static final String[] NEGATIVE_TEST_FILE_SPEC;

    static {
        POSITIVE_TEST_FILE_SPEC = getPositiveTestFileSpecs();
        NEGATIVE_TEST_FILE_SPEC = getNegativeTestFileSpecs();
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

}