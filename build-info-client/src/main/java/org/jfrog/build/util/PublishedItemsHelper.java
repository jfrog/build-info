/*
 * Copyright (C) 2010 JFrog Ltd.
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

package org.jfrog.build.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * @author Noam Y. Tenne
 */
public class PublishedItemsHelper {

    public static Map<String, String> getPublishedItemsPatternPairs(String publishedItemsPropertyValue) {
        Map<String, String> patternPairMap = Maps.newHashMap();
        if (StringUtils.isNotBlank(publishedItemsPropertyValue)) {

            List<String> patternPairs = parsePatternsFromProperty(publishedItemsPropertyValue);
            for (String patternPair : patternPairs) {

                String[] splitPattern = patternPair.split("=>");

                String sourcePattern = "";
                String targetPattern = "";

                if (splitPattern.length > 0) {
                    sourcePattern = removeDoubleDotsFromPattern(FilenameUtils.separatorsToUnix(splitPattern[0].trim()));
                }
                if (splitPattern.length > 1) {
                    targetPattern = removeDoubleDotsFromPattern(FilenameUtils.separatorsToUnix(splitPattern[1].trim()));
                }

                if (StringUtils.isNotBlank(sourcePattern)) {
                    patternPairMap.put(sourcePattern, targetPattern);
                }
            }
        }
        return patternPairMap;
    }

    public static List<String> parsePatternsFromProperty(String publishedItemsPropertyValue) {

        if (publishedItemsPropertyValue == null) {
            throw new IllegalArgumentException("Cannot parse null pattern.");
        }

        List<String> patterns = Lists.newArrayList();

        if (StringUtils.isEmpty(publishedItemsPropertyValue)) {
            return patterns;
        }

        String[] newLineTokens = publishedItemsPropertyValue.split("\n");
        for (String lineToken : newLineTokens) {

            if (StringUtils.isNotBlank(lineToken)) {
                String[] commaTokens = lineToken.trim().split(",");

                for (String commaToken : commaTokens) {

                    if (StringUtils.isNotBlank(commaToken)) {
                        patterns.add(commaToken.trim());
                    }
                }
            }
        }

        return patterns;
    }

    public static String removeDoubleDotsFromPattern(String pattern) {

        if (pattern == null) {
            throw new IllegalArgumentException("Cannot remove double dots from a null pattern.");
        }

        if (!pattern.contains("..")) {
            return pattern;
        }

        String[] splitPattern = pattern.split("/");

        StringBuilder patternBuilder = new StringBuilder();
        if (pattern.startsWith("/")) {
            patternBuilder.append("/");
        }
        for (int i = 0; i < splitPattern.length; i++) {

            if (!"..".equals(splitPattern[i])) {
                patternBuilder.append(splitPattern[i]);
                if (i != (splitPattern.length - 1)) {
                    patternBuilder.append("/");
                }
            }
        }

        return StringUtils.removeEnd(patternBuilder.toString(), "/");
    }

    public static String calculateTargetPath(String relativePath, String targetPattern, String fileName) {
        if (relativePath == null) {
            throw new IllegalArgumentException("Cannot calculate a target path given a null relative path.");
        }

        if (StringUtils.isBlank(targetPattern)) {
            return relativePath;
        }

        relativePath = FilenameUtils.separatorsToUnix(relativePath);
        targetPattern = FilenameUtils.separatorsToUnix(targetPattern);

        // take care of absolute path
        if (StringUtils.startsWith(targetPattern, "/")) {
            return targetPattern + "/" + fileName;
        }

        // take care of relative paths with patterns.
        StringBuilder itemPathBuilder = new StringBuilder();

        String[] targetTokens = targetPattern.split("/");

        boolean addedRelativeParent = false;
        for (int i = 0; i < targetTokens.length; i++) {

            boolean lastToken = (i == (targetTokens.length - 1));

            String targetToken = targetTokens[i];

            if ("**".equals(targetToken)) {
                if (!lastToken) {
                    String relativeParentPath = FilenameUtils.getPathNoEndSeparator(relativePath);
                    itemPathBuilder.append(relativeParentPath);
                    addedRelativeParent = true;
                } else {
                    itemPathBuilder.append(relativePath);
                }
            } else if (targetToken.startsWith("*.")) {
                String newFileName = FilenameUtils.removeExtension(FilenameUtils.getName(relativePath)) +
                        targetToken.substring(1);
                itemPathBuilder.append(newFileName);
            } else if ("*".equals(targetToken)) {
                itemPathBuilder.append(FilenameUtils.getName(relativePath));
            } else {
                if (StringUtils.isNotBlank(targetToken)) {
                    itemPathBuilder.append(targetToken);
                }
                if (lastToken) {
                    if (itemPathBuilder.length() > 0) {
                        itemPathBuilder.append("/");
                    }
                    if (addedRelativeParent) {
                        itemPathBuilder.append(FilenameUtils.getName(relativePath));
                    } else {
                        itemPathBuilder.append(relativePath);
                    }
                }
            }

            if (!lastToken) {
                itemPathBuilder.append("/");
            }
        }
        return itemPathBuilder.toString();
    }
}