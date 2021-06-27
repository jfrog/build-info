package org.jfrog.build.extractor.clientConfiguration.client.distribution.services;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.JFrogHttpClient;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.request.CreateReleaseBundleRequest;

import java.io.IOException;

import static org.jfrog.build.extractor.clientConfiguration.util.JsonUtils.toJsonString;

/**
 * @author yahavi
 */
public class CreateReleaseBundle extends VoidDistributionService {
    private static final String CREAT_RELEASE_BUNDLE_ENDPOINT = "api/v1/release_bundle";
    private final CreateReleaseBundleRequest request;
    private final String gpgPassphrase;

    public CreateReleaseBundle(CreateReleaseBundleRequest request, String gpgPassphrase, Log logger) {
        super(logger);
        this.request = request;
        this.gpgPassphrase = gpgPassphrase;
    }

    @Override
    protected void ensureRequirements(JFrogHttpClient client) throws IOException {
        if (request == null) {
            throw new IOException("CreateReleaseBundleRequest parameter is mandatory");
        }
        if (StringUtils.isBlank(request.getName())) {
            throw new IOException("Release bundle name in CreateReleaseBundleRequest is mandatory");
        }
        if (StringUtils.isBlank(request.getVersion())) {
            throw new IOException("Release bundle version in CreateReleaseBundleRequest is mandatory");
        }
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        HttpPost request = new HttpPost(CREAT_RELEASE_BUNDLE_ENDPOINT);
        request.setHeader("Accept", "application/json");
        if (StringUtils.isNotBlank(gpgPassphrase)) {
            request.setHeader("X-GPG-PASSPHRASE", gpgPassphrase);
        }
        StringEntity stringEntity = new StringEntity(toJsonString(this.request));
        stringEntity.setContentType("application/json");
        request.setEntity(stringEntity);
        return request;
    }
}
