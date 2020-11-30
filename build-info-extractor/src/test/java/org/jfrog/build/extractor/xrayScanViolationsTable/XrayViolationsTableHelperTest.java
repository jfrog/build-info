package org.jfrog.build.extractor.xrayScanViolationsTable;

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

public class XrayViolationsTableHelperTest {
    private static final String BASE_CONFIG_PATH = "/xrayScanViolationsTable/scanResult.json";
    private final XrayViolationsTableHelper tableHelper = new XrayViolationsTableHelper();

    @Test
    public void testPrintTable() throws IOException, URISyntaxException {
        TestsAggregationLog log = new TestsAggregationLog();
        ArtifactoryXrayResponse result = getXrayResultResource();

        tableHelper.PrintTable(result, log);
        List<String> logs = log.getLogs();
        Assert.assertEquals(logs.size(), 15);
        Assert.assertEquals(logs.get(0), tableHelper.TABLE_HEADLINE);
        String headersLine = logs.get(1);
        for (String header : tableHelper.TABLE_HEADERS) {
            Assert.assertTrue(headersLine.contains(header));
        }
        Assert.assertEquals(logs.get(3).length(), logs.get(4).length());
    }

    private ArtifactoryXrayResponse getXrayResultResource() throws URISyntaxException, IOException {
        File testResourcesPath = new File(this.getClass().getResource(BASE_CONFIG_PATH).toURI()).getCanonicalFile();
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
