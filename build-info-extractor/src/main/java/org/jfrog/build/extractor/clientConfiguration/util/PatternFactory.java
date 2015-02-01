package org.jfrog.build.extractor.clientConfiguration.util;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.dependency.pattern.BuildDependencyPattern;
import org.jfrog.build.api.dependency.pattern.DependencyPattern;
import org.jfrog.build.api.dependency.pattern.PatternType;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * @author Shay Yaakov
 */
public class PatternFactory {

    public static DependencyPattern create(String patternLine) {
        String[] splitPattern = patternLine.split("=>|=!>");
        String sourcePattern = "";
        String targetPattern = "";

        if (splitPattern.length > 0) {
            sourcePattern = FilenameUtils.separatorsToUnix(splitPattern[0].trim());
        }

        // We allow an empty target, in that case it will be ""
        if (splitPattern.length > 1) {
            targetPattern = FilenameUtils.separatorsToUnix(splitPattern[1].trim());
        }

        String pattern = extractPatternFromSource(sourcePattern);
        String matrixParams = extractMatrixParamsFromSource(sourcePattern);

        int index1 = sourcePattern.lastIndexOf('@');
        int index2 = sourcePattern.lastIndexOf('#');
        boolean lineIsBuildDependency = (index1 > 0) && (index2 > index1) && (index2 < (sourcePattern.length() - 1));
        if (lineIsBuildDependency) {
            String buildDependencyPattern = StringUtils.substring(sourcePattern, 0, index1);
            String buildName = StringUtils.substring(sourcePattern, index1 + 1, index2);
            String buildNumber = StringUtils.substring(sourcePattern, index2 + 1);
            if (StringUtils.isNotBlank(buildName) || StringUtils.isNotBlank(buildNumber)
                    || StringUtils.isNotBlank(buildDependencyPattern)) {
                matrixParams = StringUtils.substring(buildDependencyPattern, pattern.length() + 1, index1);
                if (StringUtils.isNotBlank(matrixParams)) {
                    buildDependencyPattern = StringUtils.substring(buildDependencyPattern, 0,
                            index1 - matrixParams.length() - 1);
                }
                return new BuildDependencyPattern(buildDependencyPattern, matrixParams, targetPattern,
                        getPatternType(patternLine), buildName, buildNumber);
            }
        } else {
            return new DependencyPattern(pattern, matrixParams, targetPattern, getPatternType(patternLine));
        }

        return null;
    }

    private static PatternType getPatternType(String linePattern) {
        if (StringUtils.contains(linePattern, "=!>")) {
            return PatternType.DELETE;
        }

        return PatternType.NORMAL;
    }

    private static String extractPatternFromSource(String sourcePattern) {
        int indexOfSemiColon = sourcePattern.indexOf(';');
        if (indexOfSemiColon == -1) {
            return sourcePattern;
        }

        return StringUtils.substring(sourcePattern, 0, indexOfSemiColon);
    }

    private static String extractMatrixParamsFromSource(String sourcePattern) {
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
                    key = StringUtils.substring(key, 0, key.length() - 1);
                }
                if (i > 1) {
                    matrixParamBuilder.append(";");
                }
                try {
                    matrixParamBuilder.append(URLEncoder.encode(key, "utf-8"));
                    if (mandatory) {
                        matrixParamBuilder.append("+");
                    }
                    if (matrixParamFragments.length > 1) {
                        matrixParamBuilder.append("=").append(URLEncoder.encode(matrixParamFragments[1], "utf-8"));
                    }
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalArgumentException(e.getMessage());
                }
            }
        }

        return matrixParamBuilder.toString();
    }
}
