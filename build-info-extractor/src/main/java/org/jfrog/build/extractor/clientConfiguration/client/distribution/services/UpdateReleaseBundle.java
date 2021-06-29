package org.jfrog.build.extractor.clientConfiguration.client.distribution.services;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.JFrogHttpClient;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.request.UpdateReleaseBundleRequest;

import java.io.IOException;

import static org.jfrog.build.extractor.clientConfiguration.util.JsonUtils.toJsonString;

/**
 * @author yahavi
 */
public class UpdateReleaseBundle extends VoidDistributionService {
    private static final String UPDATE_RELEASE_BUNDLE_ENDPOINT = "api/v1/release_bundle";
    private final UpdateReleaseBundleRequest request;
    private final String gpgPassphrase;
    private final String version;
    private final String name;

    public UpdateReleaseBundle(UpdateReleaseBundleRequest request, String name, String version, String gpgPassphrase, Log logger) {
        super(logger);
        this.request = request;
        this.name = name;
        this.version = version;
        this.gpgPassphrase = gpgPassphrase;
    }

    @Override
    protected void ensureRequirements(JFrogHttpClient client) throws IOException {
        if (request == null) {
            throw new IOException("UpdateReleaseBundleRequest parameter is mandatory");
        }
        if (StringUtils.isBlank(name)) {
            throw new IOException("Release bundle name is mandatory");
        }
        if (StringUtils.isBlank(version)) {
            throw new IOException("Release bundle version is mandatory");
        }
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        HttpPut request = new HttpPut(String.format("%s/%s/%s", UPDATE_RELEASE_BUNDLE_ENDPOINT, name, version));
        request.setHeader("Accept", " application/json");
        if (StringUtils.isNotBlank(gpgPassphrase)) {
            request.setHeader("X-GPG-PASSPHRASE", gpgPassphrase);
        }
        StringEntity stringEntity = new StringEntity(toJsonString(this.request));
        stringEntity.setContentType("application/json");
        request.setEntity(stringEntity);
        return request;
    }
}
