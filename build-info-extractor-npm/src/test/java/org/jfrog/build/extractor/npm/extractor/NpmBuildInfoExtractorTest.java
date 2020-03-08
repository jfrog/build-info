package org.jfrog.build.extractor.npm.extractor;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.jfrog.build.extractor.npm.extractor.NpmBuildInfoExtractor.isJsonOutputRequired;
import static org.testng.Assert.assertEquals;

@Test
public class NpmBuildInfoExtractorTest {

    @DataProvider
    private Object[][] isJsonOutputRequiredProvider() {
        return new Object[][]{
                //  Check "--json" possible positions
                {Arrays.asList("--json"), true},
                {Arrays.asList("--json", "arg2"), true},
                {Arrays.asList( "arg1", "--json"), true},
                {Arrays.asList("arg1", "--json", "arg3"), true},
                //  Check "--json=true" possible positions
                {Arrays.asList("--json=true"), true},
                {Arrays.asList("--json=true", "arg2"), true},
                {Arrays.asList( "arg1", "--json=true"), true},
                {Arrays.asList("arg1", "--json=true", "arg3"), true},
                //  Check "--json=false" possible positions
                {Arrays.asList("--json=false"), false},
                {Arrays.asList("--json=false", "arg2"), false},
                {Arrays.asList( "arg1", "--json=false"), false},
                {Arrays.asList("arg1", "--json=false", "arg3"), false},
                //  Check "--json true" possible positions
                {Arrays.asList("--json", "true"), true},
                {Arrays.asList("--json", "true", "arg3"), true},
                {Arrays.asList( "arg1", "--json", "true"), true},
                {Arrays.asList("arg1", "--json", "true", "arg4"), true},
                //  Check "--json false" possible positions
                {Arrays.asList("--json", "false"), false},
                {Arrays.asList("--json","false", "arg3"), false},
                {Arrays.asList( "arg1", "--json", "false"), false},
                {Arrays.asList("arg1", "--json", "false", "arg4"), false},
        };
    }

    @Test(dataProvider = "isJsonOutputRequiredProvider")
    public void isJsonOutputRequiredTest(List<String> installationArgs, boolean expectedResult) {
        assertEquals(isJsonOutputRequired(installationArgs), expectedResult);
    }
}
