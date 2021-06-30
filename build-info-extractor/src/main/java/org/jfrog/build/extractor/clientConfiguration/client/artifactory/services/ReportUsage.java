package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.client.JFrogHttpClient;
import org.jfrog.build.extractor.clientConfiguration.client.VoidJFrogService;
import org.jfrog.build.extractor.clientConfiguration.util.JsonSerializer;
import org.jfrog.build.extractor.usageReport.UsageReporter;

import java.io.IOException;

public class ReportUsage extends VoidJFrogService {
    private static final ArtifactoryVersion USAGE_ARTIFACTORY_MIN_VERSION = new ArtifactoryVersion("6.9.0");
    private static final String USAGE_API = "api/system/usage";

    private final UsageReporter usageReporter;

    public ReportUsage(UsageReporter usageReporter, Log log) {
        super(log);
        this.usageReporter = usageReporter;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        String requestBody = new JsonSerializer<UsageReporter>().toJSON(usageReporter);
        StringEntity entity = new StringEntity(requestBody, "UTF-8");
        entity.setContentType("application/json");
        HttpPost request = new HttpPost(USAGE_API);
        request.setEntity(entity);
        return request;
    }

    @Override
    public Void execute(JFrogHttpClient client) throws IOException {
        Version versionService = new Version(log);
        ArtifactoryVersion version = versionService.execute(client);
        if (version.isNotFound()) {
            throw new IOException("Could not get Artifactory version.");
        }
        if (!version.isAtLeast(USAGE_ARTIFACTORY_MIN_VERSION)) {
            log.debug("Usage report is not supported on targeted Artifactory server.")
            return null;
        }
        return super.execute(client);
    }
}
