package org.jfrog.build.extractor.npm.extractor;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.util.List;

import static org.jfrog.build.extractor.npm.extractor.NpmBuildInfoExtractor.isJsonOutputRequired;
import static org.testng.Assert.assertEquals;

@Test
public class NpmBuildInfoExtractorTest {

    @DataProvider
    private Object[][] isJsonOutputRequiredProvider() {
        return new Object[][]{
                //  Check "--json" possible positions
                {true, "--json"},
                {true, "--json", "arg2"},
                {true, "arg1", "--json"},
                {true, "arg1", "--json", "arg3"},
                //  Check "--json=true" possible positions
                {true, "--json=true"},
                {true, "--json=true", "arg2"},
                {true, "arg1", "--json=true"},
                {true, "arg1", "--json=true", "arg3"},
                //  Check "--json=false" possible positions
                {false, "--json=false"},
                {false, "--json=false", "arg2"},
                {false, "arg1", "--json=false"},
                {false, "arg1", "--json=false", "arg3"},
                //  Check "--json true" possible positions
                {true, "--json", "true"},
                {true, "--json", "true", "arg3"},
                {true, "arg1", "--json", "true"},
                {true, "arg1", "--json", "true", "arg4"},
                //  Check "--json false" possible positions
                {false, "--json", "false"},
                {false, "--json","false", "arg3"},
                {false, "arg1", "--json", "false"},
                {false, "arg1", "--json", "false", "arg4"},
        };
    }

    @Test(dataProvider = "isJsonOutputRequiredProvider")
    public void isJsonOutputRequiredTest(boolean expectedResult, String[] installationArgs) {
        assertEquals(isJsonOutputRequired(Lists.newArrayList(installationArgs)), expectedResult);
    }
}
