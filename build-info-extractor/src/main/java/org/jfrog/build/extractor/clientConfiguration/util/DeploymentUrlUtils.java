/*
 * Copyright (C) 2011 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.build.extractor.clientConfiguration.util;

import com.google.common.collect.ArrayListMultimap;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.util.CommonUtils;
import org.jfrog.build.extractor.clientConfiguration.ClientProperties;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.apache.commons.codec.binary.StringUtils.getBytesUtf8;
import static org.apache.commons.codec.binary.StringUtils.newStringUsAscii;

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
            if (index == unescaped.length()-1) {
                matrixParams = "";
            } else {
                matrixParams = unescaped.substring(index+1);
            }
        }

        URLCodec codec = new URLCodec();
        String[] split = StringUtils.split(path, "/");
        for (int i = 0; i < split.length; i++) {
            split[i] = newStringUsAscii(codec.encode(getBytesUtf8(split[i])));
            // codec.encode replaces spaces with '+', but we want to escape them to %20.
            split[i] = split[i].replaceAll("\\+", "%20");
        }
        String escaped = StringUtils.join(split, "/");
        if (StringUtils.isNotBlank(matrixParams)) {
            escaped += ";" + matrixParams;
        }
        return escaped;
    }

    public static String buildMatrixParamsString(ArrayListMultimap<String, String> matrixParams)
            throws UnsupportedEncodingException {
        StringBuilder matrix = new StringBuilder();
        if (matrixParams != null && !matrixParams.isEmpty()) {
            for (String propertyKey : matrixParams.keySet()) {
                for (String propertyValue : matrixParams.get(propertyKey)) {
                    matrix.append(";").append(encode(propertyKey))
                        .append("=").append(encode(propertyValue));
                }
            }
        }
        return matrix.toString();
    }

    private static String encode(String s) throws UnsupportedEncodingException {
        if (s != null) {
            return URLEncoder.encode(s, "UTF-8").replace("%2F","/");
        }
        return null;
    }
}
