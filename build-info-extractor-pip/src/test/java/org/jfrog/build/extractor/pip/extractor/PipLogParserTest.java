package org.jfrog.build.extractor.pip.extractor;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.util.TestingLog;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import static org.jfrog.build.extractor.pip.extractor.PipLogParser.*;
import static org.testng.Assert.assertEquals;

/**
 * Created by Bar Belity on 21/07/2020.
 */
public class PipLogParserTest {

    static final Log log = new TestingLog();

    @Test
    public void pipLogParserTest() {
        // Build result map.
        Map<String, String> expectedResult = new HashMap<String, String>() {{
            put("", "");
            put("", "");
        }};

        // Read pip log.
        String pipLog = this.getClass().getResourceAsStream("/pipLogParser/pipLog.txt").toString();


        // Parse.
        Map<String, String> actualMap = PipLogParser.parse(pipLog, log);

        // Validate.
        assertEquals(actualMap.keySet(), expectedResult.keySet());
        assertEquals(actualMap.values(), expectedResult.values());
    }

    @DataProvider
    private Object[][] extractPackageNameProvider() {
        return new Object[][]{
                {"Collecting PyYAML>3.11", "", false, "PyYAML", true, new HashMap<String, String>()},
                {"Collecting joblib", "PyYAML", true, "joblib", true, Collections.singletonMap("pyyaml", "")},
                {"Collecting j-obl.ib", "", false, "j-obl.ib", true, new HashMap<String, String>()},
        };
    }

    @Test(dataProvider = "extractPackageNameProvider")
    public void extractPackageNameTest(String line, String packageName, boolean isExpectingFilePath, String expectedPackageName, boolean expectedExpectingFilePath, Map<String, String> expectedDepMap) {
        Map<String, String> downloadedDependencies = new HashMap<>();
        Matcher matcher = collectingPackagePattern.matcher(line);
        matcher.find();
        MutableBoolean expectingFilePath = new MutableBoolean(isExpectingFilePath);
        // Run.
        String actualPackageName = PipLogParser.extractPackageName(downloadedDependencies, matcher, packageName, expectingFilePath, log);
        // Validate.
        assertEquals(actualPackageName, expectedPackageName);
        assertEquals(expectingFilePath.booleanValue(), expectedExpectingFilePath);
        assertEquals(downloadedDependencies.keySet(), expectedDepMap.keySet());
        assertEquals(downloadedDependencies.values(), expectedDepMap.values());
    }

    @DataProvider
    private Object[][] extractDownloadedFileNameProvider() {
        return new Object[][]{
                {"  Downloading http://someserver/pypi/packages/more/path/PyYAML-5.3.1.tar.gz", "PyYAML", true, false, new HashMap<String, String>(){{put("pyyaml", "PyYAML-5.3.1.tar.gz");}}},
                {"  Downloading http://another-server/pypi/packages/more/path/nltk-3.5.zip", "nltk", false, false, new HashMap<String, String>()},
                {"  Downloading http://another-s$erver/pypi/packages/p@ypi/more/pa?th/nltk-3.5.zip", "nltk", true, false, Collections.singletonMap("nltk", "nltk-3.5.zip")},
        };
    }

    @Test(dataProvider = "extractDownloadedFileNameProvider")
    public void extractDownloadedFileNameTest(String line, String packageName, boolean isExpectingFilePath, boolean expectedExpectingFilePath, Map<String, String> expectedDepMap) {
        Map<String, String> downloadedDependencies = new HashMap<>();
        Matcher matcher = downloadedFilePattern.matcher(line);
        matcher.find();
        MutableBoolean expectingFilePath = new MutableBoolean(isExpectingFilePath);
        // Run.
        PipLogParser.extractDownloadedFileName(downloadedDependencies, matcher, packageName, expectingFilePath, log);
        // Validate.
        assertEquals(expectingFilePath.booleanValue(), expectedExpectingFilePath);
        assertEquals(downloadedDependencies.keySet(), expectedDepMap.keySet());
        assertEquals(downloadedDependencies.values(), expectedDepMap.values());
    }

    @DataProvider
    private Object[][] extractAlreadyInstalledPackageProvider() {
        return new Object[][]{
                {"Requirement already satisfied: PyYAML>3.11 /some/path (from -r requirements.txt (line 1))", new HashMap<String, String>(){{put("pyyaml", "");}}},
                {"Requirement already satisfied: joblib /so@me/pa?th", Collections.singletonMap("joblib", "")},
                {"Requirement already satisfied: my-pkg.weird.Na-me", Collections.singletonMap("my-pkg.weird.na-me", "")},
        };
    }

    @Test(dataProvider = "extractAlreadyInstalledPackageProvider")
    public void extractAlreadyInstalledPackageTest(String line,  Map<String, String> expectedDepMap) {
        Map<String, String> downloadedDependencies = new HashMap<>();
        Matcher matcher = installedPackagePattern.matcher(line);
        matcher.find();
        // Run.
        PipLogParser.extractAlreadyInstalledPackage(downloadedDependencies, matcher, log);
        // Validate.
        assertEquals(downloadedDependencies.keySet(), expectedDepMap.keySet());
        assertEquals(downloadedDependencies.values(), expectedDepMap.values());
    }
}
