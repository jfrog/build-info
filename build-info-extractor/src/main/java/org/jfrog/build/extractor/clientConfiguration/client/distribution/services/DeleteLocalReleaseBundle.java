package org.jfrog.build.extractor.clientConfiguration.client.distribution.services;

import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.JFrogHttpClient;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.response.DistributionStatusResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author yahavi
 */
public class DeleteLocalReleaseBundle extends VoidDistributionService {
    private static final String GET_STATUS_ENDPOINT = "api/v1/release_bundle";
    private final String version;
    private final String name;

    public DeleteLocalReleaseBundle(String name, String version, Log logger) {
        super(logger);
        this.name = name;
        this.version = version;
    }

    @Override
    protected void ensureRequirements(JFrogHttpClient client) throws IOException {
        if (StringUtils.isBlank(name)) {
            throw new IOException("Release bundle name is mandatory");
        }
        if (StringUtils.isBlank(version)) {
            throw new IOException("Release bundle version is mandatory");
        }
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        return new HttpDelete(buildUrlForDelete());
    }

    @Override
    protected void setResponse(InputStream stream) throws IOException {
        result = getMapper().readValue(stream, TypeFactory.defaultInstance().constructCollectionLikeType(List.class, DistributionStatusResponse.class));
    }

    private String buildUrlForDelete() {
        String url = GET_STATUS_ENDPOINT;
        if (StringUtils.isEmpty(name)) {
            return url;
        }
        url += "/" + name;
        if (StringUtils.isEmpty(version)) {
            return url;
        }
        return url + "/" + version;
    }
}
