package org.jfrog.build.extractor.clientConfiguration.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.PreemptiveHttpClient;

import java.io.IOException;

/**
 * Created by romang on 1/1/17.
 */
public class ArtifactoryXrayClient extends ArtifactoryBaseClient {
    private static final String SCAN_BUILD_URL = "/api/xray/scanBuild";
    private static final int XRAY_SCAN_RETRY_CONSECUTIVE_RETRIES = 10; // Retrying to resume the scan 10 times after a stable connection
    private static final int XRAY_SCAN_CONNECTION_TIMEOUT_SECS = 90;  // Expecting \r\n every 30 seconds
    private static final int XRAY_SCAN_SLEEP_BETWEEN_RETRIES_MILLIS = 15000; // 15 seconds sleep between retry
    private static final String XRAY_FATAL_FAIL_STATUS = "-1"; //Fatal error code from Xray

    public ArtifactoryXrayClient(String artifactoryUrl, String username, String password, Log logger) {
        super(artifactoryUrl, username, password, logger);
    }

    public JsonNode xrayScanBuild(String buildName, String buildNumber, String context) throws IOException, InterruptedException {
        StringEntity entity = new StringEntity("{\"buildName\":\"" + buildName + "\",\"buildNumber\":\"" + buildNumber +
                "\",\"context\":\"" + context + "\"}");
        entity.setContentType("application/json");

        String scanUrl = artifactoryUrl + SCAN_BUILD_URL;
        HttpPost httpPost = new HttpPost(scanUrl);
        httpPost.setEntity(entity);
        return execute(httpPost);
    }

    /**
     * Stable connection is a connection which was connected successfully for at least @stableConnectionMillis.
     *
     * @param lastConnectionAttemptMillis
     * @return
     */
    private boolean isStableConnection(long lastConnectionAttemptMillis) {
        final long stableConnectionMillis = (XRAY_SCAN_CONNECTION_TIMEOUT_SECS + 10) * 1000;
        return lastConnectionAttemptMillis + stableConnectionMillis < System.currentTimeMillis();
    }

    private JsonNode parseXrayScanResponse(HttpResponse response) throws IOException {
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            response.getEntity().getContent().close();
            throw new IOException("Artifactory response: " + response.getStatusLine().getReasonPhrase());
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JsonNode result = mapper.readTree(response.getEntity().getContent());
        if (result.get("errors") != null) {
            String resultStr = result.get("errors").toString();
            for (JsonNode error : result.get("errors")) {
                if (error.get("status").toString().equals(XRAY_FATAL_FAIL_STATUS)) {
                    throw new RuntimeException("Artifactory response: " + resultStr);
                }
            }
            throw new IOException("Artifactory response: " + resultStr);
        }
        return result;
    }

    private JsonNode execute(HttpRequestBase httpRequest) throws InterruptedException, IOException {
        PreemptiveHttpClient client = httpClient.getHttpClient(XRAY_SCAN_CONNECTION_TIMEOUT_SECS);
        int retryNum = 0;
        long lastConnectionAttemptMillis = 0;
        while (true) {
            try {
                lastConnectionAttemptMillis = System.currentTimeMillis();
                retryNum++;
                HttpResponse response = client.execute(httpRequest);
                return parseXrayScanResponse(response);
            } catch (IOException e) {
                if (isStableConnection(lastConnectionAttemptMillis)) {
                    retryNum = 0;
                    continue;
                }
                if (XRAY_SCAN_RETRY_CONSECUTIVE_RETRIES <= retryNum) {
                    throw e;
                }
                log.warn("Xray scan connection lost: " + e.getMessage() + ", attempting to reconnect...");
                // Sleeping before trying to reconnect.
                Thread.sleep(XRAY_SCAN_SLEEP_BETWEEN_RETRIES_MILLIS);
            } finally {
                httpRequest.releaseConnection();
            }
        }
    }
}

