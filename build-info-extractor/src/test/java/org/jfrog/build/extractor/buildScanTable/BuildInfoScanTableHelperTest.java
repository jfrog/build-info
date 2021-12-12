package org.jfrog.build.extractor.buildScanTable;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.artifactoryXrayResponse.ArtifactoryXrayResponse;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class BuildInfoScanTableHelperTest {
    private static final String SCAN_RESULT_PATH = "/buildScanTable/scanResult.json";
    private static final String EMPTY_RESULT_PATH = "/buildScanTable/emptyResult.json";
    private static final String INVALID_RESULT_PATH = "/buildScanTable/invalidResult.json";
    private final BuildScanTableHelper tableHelper = new BuildScanTableHelper();

    @Test
    public void testPrintTable() throws IOException, URISyntaxException {
        TestsAggregationLog log = new TestsAggregationLog();
        ArtifactoryXrayResponse result = getXrayResultResource();

        tableHelper.printTable(result, log);
        List<String> logs = log.getLogs();
        Assert.assertEquals(logs.size(), 27);
        Assert.assertEquals(logs.get(0), tableHelper.securityViolationsTable.getHeadline());
        String headersLine = logs.get(1);
        for (String header : tableHelper.securityViolationsTable.getHeaders()) {
            Assert.assertTrue(headersLine.contains(header));
        }

        Assert.assertEquals(logs.get(14), tableHelper.licenseViolationsTable.getHeadline());
        headersLine = logs.get(15);
        for (String header : tableHelper.licenseViolationsTable.getHeaders()) {
            Assert.assertTrue(headersLine.contains(header));
        }

        Assert.assertEquals(logs.get(3).length(), logs.get(4).length());
        Assert.assertEquals(logs.get(16).length(), logs.get(17).length());
    }

    @Test
    public void testPrintTableWithCorruptData() throws IOException, URISyntaxException {
        TestsAggregationLog log = new TestsAggregationLog();
        ArtifactoryXrayResponse result = getXrayResultResource();

        // Create some broken data
        result.getAlerts().get(0).getIssues().get(0).getImpactedArtifacts().get(0).setDisplayName(null);

        tableHelper.printTable(result, log);
    }

    @Test
    public void testPrintTableWithNoViolations() throws IOException, URISyntaxException {
        TestsAggregationLog log = new TestsAggregationLog();
        ArtifactoryXrayResponse result = getXrayEmptyResultResource();

        tableHelper.printTable(result, log);
        List<String> logs = log.getLogs();
        Assert.assertEquals(logs.size(), 7);
        Assert.assertEquals(logs.get(0), tableHelper.securityViolationsTable.getHeadline());
        Assert.assertEquals(logs.get(1), tableHelper.securityViolationsTable.getEmptyTableLine());
        Assert.assertEquals(logs.get(4), tableHelper.licenseViolationsTable.getHeadline());
        Assert.assertEquals(logs.get(5), tableHelper.licenseViolationsTable.getEmptyTableLine());
    }

    @Test
    public void testPrintTableWithInvalidType() throws IOException, URISyntaxException {
        TestsAggregationLog log = new TestsAggregationLog();
        ArtifactoryXrayResponse result = getXrayInvalidResultResource();
        Assert.assertThrows(IllegalArgumentException.class, () -> tableHelper.printTable(result, log));
    }

    private ArtifactoryXrayResponse getXrayResultResource() throws URISyntaxException, IOException {
        return getResource(SCAN_RESULT_PATH);
    }

    private ArtifactoryXrayResponse getXrayEmptyResultResource() throws URISyntaxException, IOException {
        return getResource(EMPTY_RESULT_PATH);
    }

    private ArtifactoryXrayResponse getXrayInvalidResultResource() throws URISyntaxException, IOException {
        return getResource(INVALID_RESULT_PATH);
    }

    private ArtifactoryXrayResponse getResource(String path) throws URISyntaxException, IOException {
        File testResourcesPath = new File(this.getClass().getResource(path).toURI()).getCanonicalFile();
        ObjectMapper mapper = new ObjectMapper(new JsonFactory());
        return mapper.readValue(testResourcesPath, ArtifactoryXrayResponse.class);
    }

    public static class TestsAggregationLog implements Log {
        List<String> logs = new ArrayList<>();

        @Override
        public void debug(String message) {
            logs.add(message);
        }

        @Override
        public void info(String message) {
            logs.add(message);
        }

        @Override
        public void warn(String message) {
            logs.add(message);
        }

        @Override
        public void error(String message) {
            logs.add(message);
        }

        @Override
        public void error(String message, Throwable e) {
            logs.add(message);
        }

        public List<String> getLogs() {
            return logs;
        }
    }
}
