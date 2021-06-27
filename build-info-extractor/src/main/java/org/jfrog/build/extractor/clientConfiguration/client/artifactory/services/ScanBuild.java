package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.JFrogHttpClient;
import org.jfrog.build.client.artifactoryXrayResponse.ArtifactoryXrayResponse;
import org.jfrog.build.extractor.clientConfiguration.client.JFrogService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ScanBuild extends JFrogService<ArtifactoryXrayResponse> {
    /**
     * Expecting \r\n every 30 seconds
     */
    public static final int XRAY_SCAN_CONNECTION_TIMEOUT_SECS = 90;
    /**
     * Fatal error code from Xray
     */
    private static final String XRAY_FATAL_FAIL_STATUS = "-1";
    /**
     * Retrying to resume the scan 5 times after a stable connection
     */
    private static final int XRAY_SCAN_RETRY_CONSECUTIVE_RETRIES = 5;
    /**
     * 30 seconds sleep between retry
     */
    private static final int XRAY_SCAN_SLEEP_BETWEEN_RETRIES_MILLIS = 30000;
    private static final String SCAN_BUILD_URL = "api/xray/scanBuild";
    private final String buildName;
    private final String buildNumber;
    private final String project;
    private final String context;
    private HttpPost request;

    public ScanBuild(String buildName, String buildNumber, String project, String context, Log log) {
        super(log);
        this.buildName = buildName;
        this.buildNumber = buildNumber;
        this.project = project;
        this.context = context;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        StringEntity entity = new StringEntity(getXrayScanBody());
        entity.setContentType("application/json");
        HttpPost request = new HttpPost(SCAN_BUILD_URL);

        // The scan build operation can take a long time to finish.
        // To keep the connection open, when Xray starts scanning the build, it starts sending new-lines
        // on the open channel. This tells the client that the operation is still in progress and the
        // connection does not get timed out.
        // We need make sure the new-lines are not buffered on the nginx and are flushed
        // as soon as Xray sends them.
        request.addHeader("X-Accel-Buffering", "no");

        request.setEntity(entity);
        this.request = request;
        return request;
    }

    @Override
    protected void setResponse(InputStream stream) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String content = IOUtils.toString(stream, StandardCharsets.UTF_8.name());
        JsonNode result;
        try {
            result = mapper.readTree(content);
            if (result == null) {
                throw new NullArgumentException("Received empty content from Artifactory");
            }
        } catch (Exception ex) {
            // Throwing XrayErrorException since the retry-mechanism should not reset the retries-count in such error.
            throw new XrayErrorException(String.format("Failed processing scan response: %s\n%s", ex, content));
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
        this.result = mapper.treeToValue(result, ArtifactoryXrayResponse.class);
    }


    @Override
    public ArtifactoryXrayResponse execute(JFrogHttpClient client) throws IOException {
        int retryNum = 0;
        long lastConnectionAttemptMillis = 0;
        while (true) {
            try {
                lastConnectionAttemptMillis = System.currentTimeMillis();
                retryNum++;
                return super.execute(client);
            } catch (XrayErrorException e) {
                handleException(retryNum, e);
            } catch (IOException e) {
                if (isStableConnection(lastConnectionAttemptMillis)) {
                    // Interruption may happen when build is aborted in the CI Server.
                    if (Thread.currentThread().isInterrupted()) {
                        throw new IOException("Operation interrupted.");
                    }
                    retryNum = 0;
                    continue;
                }
                handleException(retryNum, e);
            } finally {
                request.releaseConnection();
            }
        }
    }

    /**
     * Stable connection is a connection which was connected successfully for at least @stableConnectionMillis.
     *
     */
    private boolean isStableConnection(long lastConnectionAttemptMillis) {
        final long stableConnectionMillis = (XRAY_SCAN_CONNECTION_TIMEOUT_SECS + 10) * 1000;
        return lastConnectionAttemptMillis + stableConnectionMillis < System.currentTimeMillis();
    }

    private void handleException(int retryNum, IOException e) throws IOException {
        if (XRAY_SCAN_RETRY_CONSECUTIVE_RETRIES <= retryNum) {
            throw e;
        }
        log.warn("Xray scan connection lost: " + e.getMessage() + ", attempting to reconnect...");
        // Sleeping before trying to reconnect.
        try {
            Thread.sleep(XRAY_SCAN_SLEEP_BETWEEN_RETRIES_MILLIS);
        } catch (InterruptedException interruptedException) {
            throw new IOException(interruptedException.getMessage());
        }
    }

    /**
     * Private exception class, signals that Xray-response returned from Artifactory contained an error.
     */
    private static class XrayErrorException extends IOException {
        private XrayErrorException(String message) {
            super(message);
        }
    }

    /**
     * Creates JSON body request for ScanBuild API.
     */
    private String getXrayScanBody() {
        String body = "{\"buildName\":\"" + buildName + "\",\"buildNumber\":\"" + buildNumber +
                "\",\"context\":\"" + context;
        if (StringUtils.isNotEmpty(project)) {
            body += "\",\"project\":\"" + project;
        }
        return body + "\"}";
    }
}
