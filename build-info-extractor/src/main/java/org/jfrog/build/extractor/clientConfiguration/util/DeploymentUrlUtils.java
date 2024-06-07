package org.jfrog.build.extractor.clientConfiguration.util;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.CommonUtils;
import org.jfrog.build.extractor.UrlUtils;
import org.jfrog.build.extractor.clientConfiguration.ClientProperties;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author Tomer C.
 */
public abstract class DeploymentUrlUtils {

    /**
     * Calculate the full Artifactory deployment URL which includes the matrix params appended to it. see {@link
     * org.jfrog.build.extractor.clientConfiguration.ClientProperties#PROP_DEPLOY_PARAM_PROP_PREFIX} for the property prefix that this method takes into account.
     *
     * @param artifactoryUrl The Artifactory upload URL.
     * @param properties     The properties to append to the Artifactory URL.
     * @return The generated Artifactory URL with the matrix params appended to it.
     */
    public static String getDeploymentUrl(String artifactoryUrl, Properties properties)
            throws UnsupportedEncodingException {
        Map<Object, Object> filteredProperties = CommonUtils.filterMapKeys(properties, input -> ((String) input).startsWith(ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX));
        StringBuilder deploymentUrl = new StringBuilder(artifactoryUrl);
        Set<Map.Entry<Object, Object>> propertyEntries = filteredProperties.entrySet();
        for (Map.Entry<Object, Object> propertyEntry : propertyEntries) {
            String key = StringUtils
                    .removeStart((String) propertyEntry.getKey(),
                            ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX);
            deploymentUrl.append(";").append(URLEncoder.encode(key, "UTF-8")).append("=").
                    append(URLEncoder.encode(((String) propertyEntry.getValue()), "UTF-8"));
        }
        return deploymentUrl.toString();
    }

    public static String encodePath(String unescaped) {
        int index = unescaped.indexOf(";");

        String path = unescaped;
        String matrixParams = null;
        if (index != -1) {
            path = unescaped.substring(0, index);
            if (index == unescaped.length() - 1) {
                matrixParams = "";
            } else {
                matrixParams = unescaped.substring(index + 1);
            }
        }

        String[] split = StringUtils.split(path, "/");
        for (int i = 0; i < split.length; i++) {
            split[i] = UrlUtils.encodeUrlPathPart(split[i]);
        }
        String escaped = StringUtils.join(split, "/");
        if (StringUtils.isNotBlank(matrixParams)) {
            escaped += ";" + matrixParams;
        }
        return escaped;
    }


    public static String buildMatrixParamsString(MultiValuedMap<String, String> matrixParams, boolean encodeProperties)
            throws UnsupportedEncodingException {
        StringBuilder matrix = new StringBuilder();
        if (matrixParams != null && !matrixParams.isEmpty()) {
            for (String propertyKey : matrixParams.keySet()) {
                for (String multiPropertyValue : matrixParams.get(propertyKey)) {
                    if (multiPropertyValue == null) {
                        continue;
                    }
                    // Due to known bug in Artifactory, in order to support multi values properties
                    // we add a statement to each value separately.
                    for (String propertyValue : multiPropertyValue.split(",")) {
                        propertyValue = propertyValue.trim();
                        matrix.append(";").append(encodeProperties ? encode(propertyKey) : propertyKey)
                                .append("=").append(encodeProperties ? encode(propertyValue) : propertyValue);
                    }
                }
            }
        }
        return matrix.toString();
    }

    private static String encode(String s) throws UnsupportedEncodingException {
        if (s != null) {
            return URLEncoder.encode(s, "UTF-8");
        }
        return null;
    }
}
