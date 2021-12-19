package org.jfrog.build.extractor.go.extractor;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class GoZipBallStreamerTest {
    @Test(dataProvider = "testIsSubModuleProvider")
    public void testIsSubModule(String subModuleName, String entryName, boolean expectedResult) {
        GoZipBallStreamer goZipBallStreamer = new GoZipBallStreamer(null, "ignore", "ignore", null);
        goZipBallStreamer.setSubModuleName(subModuleName);
        boolean res = goZipBallStreamer.isSubModule(entryName);
        Assert.assertEquals(res, expectedResult);
    }

    @DataProvider
    private Object[][] testIsSubModuleProvider() {
        return new Object[][]{
                {"", "root/go.mod", false},
                {"", "go.mod", false},
                {"", "root/v2/go.mod", true},
                {"v2", "root/go.mod", true},
                {"v2", "root/v2/go.mod", false}
        };
    }

}