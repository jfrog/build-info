package org.jfrog.build.extractor.executor;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Created by Bar Belity on 04/08/2020.
 */
public class CommandExecutorTest {

    @DataProvider
    private Object[][] getFixedWindowsPathProvider() {
        return new Object[][]{
                {"C:\\my\\first;Drive:\\my\\second", "C:\\my\\first;Drive:\\my\\second"},
                {"C:\\my\\first;Drive:\\my\\second:\\my\\third", "C:\\my\\first;Drive:\\my\\second;\\my\\third"},
                {"/Users/my/first:/Users/my/second", "/Users/my/first;/Users/my/second"},
                {"/Users/my/first:/Users/my/sec;ond", "/Users/my/first;/Users/my/sec;ond"},
                {"/Users/my/first:/Users/my/sec;ond:C:\\my\\third", "/Users/my/first;/Users/my/sec;ond:C:\\my\\third"},
        };
    }

    @Test(dataProvider = "getFixedWindowsPathProvider")
    public void getFixedWindowsPathTest(String path, String expected) {
        String actual = CommandExecutor.getFixedWindowsPath(path);
        assertEquals(actual, expected);
    }
}
