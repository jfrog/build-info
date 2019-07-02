package org.jfrog.build.extractor.clientConfiguration.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NullArgumentException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.PreemptiveHttpClient;
import org.jfrog.build.client.artifactoryXrayResponse.ArtifactoryXrayResponse;

import java.io.IOException;

/**
 * Created by romang on 1/1/17.
 */
public class ArtifactoryXrayClient extends ArtifactoryBaseClient {
    private static final String SCAN_BUILD_URL = "/api/xray/scanBuild";
    /**
     * Retrying to resume the scan 5 times after a stable connection
     */
    private static final int XRAY_SCAN_RETRY_CONSECUTIVE_RETRIES = 5;
    /**
     * Expecting \r\n every 30 seconds
     */
    private static final int XRAY_SCAN_CONNECTION_TIMEOUT_SECS = 300;
    /**
     * 30 seconds sleep between retry
     */
    private static final int XRAY_SCAN_SLEEP_BETWEEN_RETRIES_MILLIS = 30000;
    /**
     * Fatal error code from Xray
     */
    private static final String XRAY_FATAL_FAIL_STATUS = "-1";
    /**
     * ArtifactoryXrayClient manages retry mechanism, http-client's mechanism is not required
     */
    private static final int HTTP_CLIENT_RETRIES = 0;

    public ArtifactoryXrayClient(String artifactoryUrl, String username, String password, Log logger) {
        super(artifactoryUrl, username, password, logger);
        setConnectionRetries(HTTP_CLIENT_RETRIES);
    }

    public ArtifactoryXrayResponse xrayScanBuild(String buildName, String buildNumber, String context) throws IOException, InterruptedException {
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

    private ArtifactoryXrayResponse parseXrayScanResponse(HttpResponse response) throws IOException {
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new IOException("Artifactory response: " + response.getStatusLine().getReasonPhrase());
        }

        ObjectMapper mapper = new ObjectMapper();
        String content = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
        JsonNode result;
        try {
            result = mapper.readTree(content);
            if (result == null) {
                throw new NullArgumentException("Received empty content from Artifactory");
            }
        } catch (Exception ex) {
            // Throwing XrayErrorException since the retry-mechanism should not reset the retries-count in such error.
            throw new XrayErrorException(String.format("Failed processing scan response: %s\n%s", ex.toString(), content));
        }
        if (result.get("errors") != null) {
            String resultStr = result.get("errors").toString();
            for (JsonNode error : result.get("errors")) {
                if (error.get("status").toString().equals(XRAY_FATAL_FAIL_STATUS)) {
                    throw new RuntimeException("Artifactory response: " + resultStr);
                }
            }
            throw new XrayErrorException("Artifactory response: " + resultStr);
        }
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.treeToValue(result, ArtifactoryXrayResponse.class);
    }

    private ArtifactoryXrayResponse execute(HttpRequestBase httpRequest) throws InterruptedException, IOException {
        PreemptiveHttpClient client = httpClient.getHttpClient(XRAY_SCAN_CONNECTION_TIMEOUT_SECS);
        int retryNum = 0;
        long lastConnectionAttemptMillis = 0;
        HttpResponse response = null;
        while (true) {
            try {
                lastConnectionAttemptMillis = System.currentTimeMillis();
                retryNum++;
                response = client.execute(httpRequest);
                return parseXrayScanResponse(response);
            } catch (XrayErrorException e) {
                handleException(retryNum, e);
            } catch (IOException e) {
                if (isStableConnection(lastConnectionAttemptMillis)) {
                    // Interruption may happen when build is aborted in the CI Server.
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException("Operation interrupted.");
                    }
                    retryNum = 0;
                    continue;
                }
                handleException(retryNum, e);
            } finally {
                releaseResponse(httpRequest, response);
            }
        }
    }

    private void releaseResponse(HttpRequestBase httpRequest, HttpResponse response) {
        if (response != null) {
            try {
                EntityUtils.consume(response.getEntity());
            } catch (IOException e) {
                // Ignore
            }
        }
        httpRequest.releaseConnection();
    }

    private void handleException(int retryNum, IOException e) throws InterruptedException, IOException {
        if (XRAY_SCAN_RETRY_CONSECUTIVE_RETRIES <= retryNum) {
            throw e;
        }
        log.warn("Xray scan connection lost: " + e.getMessage() + ", attempting to reconnect...");
        // Sleeping before trying to reconnect.
        Thread.sleep(XRAY_SCAN_SLEEP_BETWEEN_RETRIES_MILLIS);
    }

    /**
     * Private exception class, signals that Xray-response returned from Artifactory contained an error.
     */
    private class XrayErrorException extends IOException {
        private XrayErrorException(String message) {
            super(message);
        }
    }
}

