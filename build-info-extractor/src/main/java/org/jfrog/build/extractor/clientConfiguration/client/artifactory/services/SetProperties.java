package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.commons.collections4.MultiMapUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.JFrogHttpClient;
import org.jfrog.build.extractor.clientConfiguration.client.VoidJFrogService;
import org.jfrog.build.extractor.clientConfiguration.util.DeploymentUrlUtils;

import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.stripEnd;
import static org.jfrog.build.extractor.UrlUtils.encodeUrl;

public class SetProperties extends VoidJFrogService {
    public static final String SET_PROPERTIES_ENDPOINT = "api/storage/";
    private final MultiValuedMap<String, String> propertiesMap;
    private final boolean encodeProperties;
    private final String relativePath;
    private final String propertiesString;

    private SetProperties(String relativePath, String propertiesString, MultiValuedMap<String, String> propertiesMap, boolean encodeProperties, Log log) {
        super(log);
        this.relativePath = relativePath;
        this.propertiesMap = propertiesMap;
        this.propertiesString = propertiesString;
        this.encodeProperties = encodeProperties;
    }

    public SetProperties(String relativePath, String propertiesString, boolean encodeProperties, Log log) {
        this(relativePath, propertiesString, null, encodeProperties, log);
    }

    public SetProperties(String relativePath, MultiValuedMap<String, String> propertiesMap, boolean encodeProperties, Log log) {
        this(relativePath, null, propertiesMap, encodeProperties, log);
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        String requestUrl = SET_PROPERTIES_ENDPOINT + encodeUrl(stripEnd(relativePath, "/")) + "?properties=";
        if (isNotEmpty(propertiesString)) {
            requestUrl += encodeProperties ? DeploymentUrlUtils.buildMatrixParamsString(mapPropsString(propertiesString), true) : propertiesString;
        } else {
            requestUrl += DeploymentUrlUtils.buildMatrixParamsString(propertiesMap, encodeProperties);
        }
        return new HttpPut(requestUrl);
    }

    @Override
    protected void handleUnsuccessfulResponse(HttpEntity entity) throws IOException {
        log.error("Failed to set properties to '" + relativePath + "'");
        throwException(entity, getStatusCode());
    }

    @Override
    protected void ensureRequirements(JFrogHttpClient client) throws IOException {
        if (StringUtils.isEmpty(propertiesString)) {
            return;
        }
        for (String prop : propertiesString.trim().split(";")) {
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

    private MultiValuedMap<String, String> mapPropsString(String props) {
        MultiValuedMap<String, String> propsMap = MultiMapUtils.newListValuedHashMap();
        String[] propsList = props.split(";");
        for (String prop : propsList) {
            if (isNotEmpty(prop)) {
                String[] propParts = prop.split("=");
                propsMap.put(propParts[0], propParts[1]);
            }
        }
        return propsMap;
    }
}
