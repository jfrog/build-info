package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.BuildRetention;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.VoidJFrogService;

import java.io.IOException;

import static org.jfrog.build.extractor.clientConfiguration.util.JsonUtils.toJsonString;

public class SendBuildRetention extends VoidJFrogService {
    private static final String RETENTION_REST_URL = "api/build/retention/";
    private final BuildRetention buildRetention;
    private final String buildName;
    private final boolean async;

    public SendBuildRetention(BuildRetention buildRetention, String buildName, boolean async, Log logger) {
        super(logger);
        this.buildRetention = buildRetention;
        this.buildName = encodeUrl(buildName);
        this.async = async;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        log.info(createBuildRetentionLogMsg(buildRetention, async));
        log.debug(toJsonString(buildRetention));
        String url = RETENTION_REST_URL + buildName + "?async=" + async;
        return new HttpPost(url);
    }

    private String createBuildRetentionLogMsg(BuildRetention buildRetention, boolean async) {
        StringBuilder strBuilder = new StringBuilder().append("Sending");

        if (async) {
            strBuilder.append(" async");
        }

        strBuilder.append(" request for build retention");

        if (buildRetention.isDeleteBuildArtifacts()) {
            strBuilder.append(", deleting build artifacts");
        }

        if (buildRetention.getCount() != -1) {
            strBuilder.append(", max number of builds to store: ").append(buildRetention.getCount());
        }

        if (buildRetention.getMinimumBuildDate() != null) {
            strBuilder.append(", min build date: ").append(buildRetention.getMinimumBuildDate());
        }

        if (!buildRetention.getBuildNumbersNotToBeDiscarded().isEmpty()) {
            strBuilder.append(", build numbers not to be discarded: ").append(buildRetention.getBuildNumbersNotToBeDiscarded());
        }
        strBuilder.append(".");

        return strBuilder.toString();
    }
}
