package org.jfrog.build.extractor.clientConfiguration.util;

import org.jfrog.build.extractor.UrlUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Created by Bar Belity on 19/07/2020.
 */
public class UrlUtilsTest {

    @Test(dataProvider = "testMaskUrlsProvider")
    public void testMaskCredentialsInUrl(String testName, String originalUrl, String expectedUrl) {
        Assert.assertEquals(UrlUtils.removeCredentialsFromUrl(originalUrl), expectedUrl, "Failed masking credentials on test name: " + testName);
    }

    @DataProvider
    private Object[][] testMaskUrlsProvider() {
        return new String[][]{
                {"http", "This is an example line http://user:password@127.0.0.1:8081/artifactory/path/to/repo", "This is an example line http://127.0.0.1:8081/artifactory/path/to/repo"},
                {"https", "This is an example line https://user:password@127.0.0.1:8081/artifactory/path/to/repo", "This is an example line https://127.0.0.1:8081/artifactory/path/to/repo"},
                {"git", "This is an example line git://user:password@127.0.0.1:8081/artifactory/path/to/repo", "This is an example line git://127.0.0.1:8081/artifactory/path/to/repo"},
                {"http with token", "This is an example line http://123456@127.0.0.1:8081/artifactory/path/to/repo", "This is an example line http://127.0.0.1:8081/artifactory/path/to/repo"},
                {"https with token", "This is an example line https://123456@127.0.0.1:8081/artifactory/path/to/repo", "This is an example line https://127.0.0.1:8081/artifactory/path/to/repo"},
                {"git with token", "This is an example line git://123456@127.0.0.1:8081/artifactory/path/to/repo", "This is an example line git://127.0.0.1:8081/artifactory/path/to/repo"},
                {"No credentials", "This is an example line https://127.0.0.1:8081/artifactory/path/to/repo", "This is an example line https://127.0.0.1:8081/artifactory/path/to/repo"},
                {"No http", "This is an example line", "This is an example line"},
                {"Multiple @", "This is an example line https://[user]:passw@ord@127.0.0.1:8081/artifactory/path/to/repo", "This is an example line https://127.0.0.1:8081/artifactory/path/to/repo"},
        };
    }
}
