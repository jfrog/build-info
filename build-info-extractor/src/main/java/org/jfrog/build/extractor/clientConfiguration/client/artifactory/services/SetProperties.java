package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import com.google.common.collect.ArrayListMultimap;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.JFrogHttpClient;
import org.jfrog.build.extractor.clientConfiguration.client.VoidJFrogService;
import org.jfrog.build.extractor.clientConfiguration.util.DeploymentUrlUtils;

import java.io.IOException;

public class SetProperties extends VoidJFrogService {
    public static final String SET_PROPERTIES_ENDPOINT = "api/storage/";

    private final String relativePath;
    private final String properties;
    private final boolean encodeProperties;

    public SetProperties(String relativePath, String properties, boolean encodeProperties, Log log) {
        super(log);
        this.relativePath = relativePath;
        this.properties = properties;
        this.encodeProperties = encodeProperties;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        String requestUrl = SET_PROPERTIES_ENDPOINT + encodeUrl(StringUtils.stripEnd(relativePath, "/") + "?properties=");
        if (encodeProperties) {
            requestUrl += DeploymentUrlUtils.buildMatrixParamsString(mapPropsString(properties), true);
        } else {
            requestUrl += properties;
        }
        return new HttpPut(requestUrl);
    }

    @Override
    protected void handleUnsuccessfulResponse(CloseableHttpResponse response) throws IOException {
        log.error("Failed to set properties to  '" + relativePath + "'");
        throwException(response);
    }

    @Override
    protected void ensureRequirements(JFrogHttpClient client) throws IOException {
        for (String prop : properties.trim().split(";")) {
            if (prop.isEmpty()) {
                continue;
            }

            String key = StringUtils.substringBefore(prop, "=");
            if (key.isEmpty()) {
                throw new IOException("Setting properties: Every property must have a key.");
            }

            String values = StringUtils.substringAfter(prop, "=");
            // Verify values aren't empty nor commas only
            if (values.isEmpty() || StringUtils.countMatches(values, ",") == values.length()) {
                throw new IOException("Setting properties: Every property must have at least one value.");
            }
        }
    }

    private ArrayListMultimap<String, String> mapPropsString(String props) {
        ArrayListMultimap<String, String> propsMap = ArrayListMultimap.create();
        String[] propsList = props.split(";");
        for (String prop : propsList) {
            String[] propParts = prop.split("=");
            propsMap.put(propParts[0], propParts[1]);
        }
        return propsMap;
    }
}
