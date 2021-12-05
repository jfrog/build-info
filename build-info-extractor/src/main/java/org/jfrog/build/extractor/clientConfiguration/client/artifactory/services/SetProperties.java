package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import com.google.common.collect.ArrayListMultimap;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.JFrogHttpClient;
import org.jfrog.build.extractor.clientConfiguration.client.VoidJFrogService;
import org.jfrog.build.extractor.clientConfiguration.util.DeploymentUrlUtils;

import java.io.IOException;

public class SetProperties extends VoidJFrogService {
    public static final String SET_PROPERTIES_ENDPOINT = "api/storage/";
    private ArrayListMultimap<String, String> propertiesMap;
    private final boolean encodeProperties;
    private final String relativePath;
    private String propertiesString;

    private SetProperties(String relativePath, String propertiesString,ArrayListMultimap<String, String> propertiesMap, boolean encodeProperties, Log log) {
        super(log);
        this.relativePath = relativePath;
        this.propertiesMap = propertiesMap;
        this.propertiesString = propertiesString;
        this.encodeProperties = encodeProperties;
    }

    public SetProperties(String relativePath, String propertiesString, boolean encodeProperties, Log log) {
        this(relativePath, propertiesString,null, encodeProperties, log);
    }

    public SetProperties(String relativePath, ArrayListMultimap<String, String> propertiesMap, boolean encodeProperties, Log log) {
        this(relativePath, null,propertiesMap, encodeProperties, log);
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        String requestUrl = SET_PROPERTIES_ENDPOINT + encodeUrl(StringUtils.stripEnd(relativePath, "/") + "?properties=");
        if (StringUtils.isNotEmpty(propertiesString)) {
            requestUrl += encodeProperties ? DeploymentUrlUtils.buildMatrixParamsString(mapPropsString(propertiesString), true) : propertiesString;
        } else {
            requestUrl += DeploymentUrlUtils.buildMatrixParamsString(propertiesMap, encodeProperties);
        }
        return new HttpPut(requestUrl);
    }

    @Override
    protected void handleUnsuccessfulResponse(HttpEntity entity) throws IOException {
        log.error("Failed to set properties to  '" + relativePath + "'");
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

    private ArrayListMultimap<String, String> mapPropsString(String props) {
        ArrayListMultimap<String, String> propsMap = ArrayListMultimap.create();
        String[] propsList = props.split(";");
        for (String prop : propsList) {
            if (StringUtils.isNotEmpty(prop)) {
                String[] propParts = prop.split("=");
                propsMap.put(propParts[0], propParts[1]);
            }
        }
        return propsMap;
    }
}
