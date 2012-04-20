package org.jfrog.build.util;

import org.apache.commons.lang.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Helper class for parsing custom resolved dependencies
 *
 * @author Shay Yaakov
 */
public abstract class DependenciesHelper {

    public static String extractPatternFromSource(String sourcePattern) {
        int indexOfSemiColon = sourcePattern.indexOf(';');
        if (indexOfSemiColon == -1) {
            return sourcePattern;
        }

        return sourcePattern.substring(0, indexOfSemiColon);
    }

    public static String extractMatrixParamsFromSource(String sourcePattern) throws UnsupportedEncodingException {
        StringBuilder matrixParamBuilder = new StringBuilder();

        //Split pattern to fragments in case there are any matrix params
        String[] patternFragments = StringUtils.split(sourcePattern, ';');

        //Iterate and add matrix params if there are any
        if (patternFragments.length > 1) {
            for (int i = 1; i < patternFragments.length; i++) {
                String matrixParam = patternFragments[i];
                String[] matrixParamFragments = StringUtils.split(matrixParam, '=');

                if (matrixParamFragments.length == 0) {
                    continue;
                }
                //If the key is mandatory, separate the + before encoding
                String key = matrixParamFragments[0];
                boolean mandatory = false;
                if (key.endsWith("+")) {
                    mandatory = true;
                    key = key.substring(0, key.length() - 1);
                }
                matrixParamBuilder.append(URLEncoder.encode(key, "utf-8"));
                if (mandatory) {
                    matrixParamBuilder.append("+");
                }
                if (matrixParamFragments.length > 1) {
                    matrixParamBuilder.append("=").append(URLEncoder.encode(matrixParamFragments[1], "utf-8"));
                }
            }
        }

        return matrixParamBuilder.toString();
    }
}
