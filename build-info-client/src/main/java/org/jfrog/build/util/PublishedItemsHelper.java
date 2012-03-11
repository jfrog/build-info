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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
                    sourcePattern = FilenameUtils.separatorsToUnix(splitPattern[0].trim());
                }
                if (splitPattern.length > 1) {
                    targetPattern = FilenameUtils.separatorsToUnix(splitPattern[1].trim());
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

    public static Multimap<String, File> buildPublishingData(File checkoutDir, String sourcePath, String targetPath)
            throws IOException {
        final Multimap<String, File> filePathsMap = HashMultimap.create();
        File file = resolvePath(checkoutDir, sourcePath);
        if (file.isFile()) {
            filePathsMap.put(targetPath, file);
        } else {
            Pattern filePattern = null;
            File patternDir = null;
            if (file.isDirectory()) {
                filePattern = Pattern.compile(".*");
                patternDir = file;
            } else if (sourcePath.indexOf('*') >= 0 || sourcePath.indexOf('?') >= 0) {
                File baseTruncationDir = getBaseTruncationDir(file);
                patternDir = baseTruncationDir != null ? baseTruncationDir : checkoutDir;
                boolean fileWithinCwd = isAncestor(checkoutDir, file);

                if (fileWithinCwd) {
                    sourcePath = sourcePath.substring(
                            patternDir.getAbsolutePath().length() - checkoutDir.getAbsolutePath().length());
                }
                /*                    if (sourcePath.startsWith(FileUtil.toSystemIndependentName(myBasePatternDir.getAbsolutePath()))) {
                    path = FileUtil.getRelativePath(myBasePatternDir, new File(path));
                    path = FileUtils
                }*/
                filePattern = Pattern.compile(convertAntToRegexp(sourcePath));
            }
            if (filePattern != null) {
                List<File> files = new ArrayList<File>();
                collectMatchedFiles(patternDir, patternDir, filePattern, files);
                filePathsMap.putAll(targetPath, files);
            }
        }

        return filePathsMap;
    }

    private static File resolvePath(File baseDir, String path) {
        File pathFile = new File(path);
        if (pathFile.isAbsolute()) {
            return pathFile;
        }

        File rawFile = new File(baseDir, path);
        if (baseDir.getPath().startsWith("\\\\")) {
            return rawFile;
        }

        return new File(rawFile.toURI().normalize().getPath());
    }

    private static File getBaseTruncationDir(File file) {
        String dirWithoutPattern = getDirWithoutPattern(file.getPath());
        if ("".equals(dirWithoutPattern)) {
            return null;
        } else {
            return new File(dirWithoutPattern);
        }
    }

    private static String getDirWithoutPattern(String pathWithWildCard) {
        String t = pathWithWildCard.replace('\\', '/');
        int firstStar = t.indexOf('*');
        int firstQuest = t.indexOf('?');
        int mark = firstStar >= 0 ? firstStar >= firstQuest && firstQuest >= 0 ? firstQuest : firstStar : firstQuest;
        int lastSlash = t.lastIndexOf('/', mark);
        return lastSlash <= 0 ? "" : pathWithWildCard.substring(0, lastSlash);
    }

    private static boolean isAncestor(File ancestor, File file)
            throws IOException {
        File parent = file;
        do {
            if (parent == null) {
                return false;
            }
            if (parent.equals(ancestor)) {
                return true;
            }
            parent = getParentFile(parent);
        } while (true);
    }

    private static File getParentFile(File file) {
        int skipCount = 0;
        File parentFile = file;
        do {
            do {
                parentFile = parentFile.getParentFile();
                if (parentFile == null) {
                    return null;
                }
            } while (".".equals(parentFile.getName()));
            if ("..".equals(parentFile.getName())) {
                skipCount++;
            } else if (skipCount > 0) {
                skipCount--;
            } else {
                return parentFile;
            }
        } while (true);
    }

    private static String convertAntToRegexp(String antPattern) {
        StringBuilder builder = new StringBuilder(antPattern.length());
        int asteriskCount = 0;
        boolean recursive = true;
        int start = !antPattern.startsWith("/") && !antPattern.startsWith("\\") ? 0 : 1;
        for (int idx = start; idx < antPattern.length(); idx++) {
            char ch = antPattern.charAt(idx);
            if (ch == '*') {
                asteriskCount++;
                continue;
            }
            boolean foundRecursivePattern = recursive && asteriskCount == 2 && (ch == '/' || ch == '\\');
            boolean asterisksFound = asteriskCount > 0;
            asteriskCount = 0;
            recursive = ch == '/' || ch == '\\';
            if (foundRecursivePattern) {
                builder.append("(?:[^/]+/)*?");
                continue;
            }
            if (asterisksFound) {
                builder.append("[^/]*?");
            }
            if (ch == '(' || ch == ')' || ch == '[' || ch == ']' || ch == '^' || ch == '$' || ch == '.' || ch == '{' || ch == '}' || ch == '+' || ch == '|') {
                builder.append('\\').append(ch);
                continue;
            }
            if (ch == '?') {
                builder.append("[^/]{1}");
                continue;
            }
            if (ch == '\\') {
                builder.append('/');
            } else {
                builder.append(ch);
            }
        }

        boolean isTrailingSlash = builder.length() > 0 && builder.charAt(builder.length() - 1) == '/';
        if (asteriskCount == 0 && isTrailingSlash || recursive && asteriskCount == 2) {
            if (isTrailingSlash) {
                builder.setLength(builder.length() - 1);
            }
            if (builder.length() == 0) {
                builder.append(".*");
            } else {
                builder.append("(?:$|/.+)");
            }
        } else if (asteriskCount > 0) {
            builder.append("[^/]*?");
        }
        return builder.toString();
    }

    private static void collectMatchedFiles(File absoluteRoot, File root, Pattern pattern, List files) {
        File dirs[] = root.listFiles();
        if (dirs == null) {
            return;
        }
        File arr[] = dirs;
        int len = arr.length;
        for (int i = 0; i < len; i++) {
            File dir = arr[i];
            if (dir.isFile()) {
                String path = getRelativePath(absoluteRoot, dir).replace("\\", "/");
                //String path = absoluteRoot.getAbsolutePath();
                if (pattern.matcher(path).matches()) {
                    files.add(dir);
                }
            } else {
                collectMatchedFiles(absoluteRoot, dir, pattern, files);
            }
        }
    }

    private static String getRelativePath(File base, File file) {
        if (base == null || file == null) {
            return null;
        }
        if (!base.isDirectory()) {
            base = base.getParentFile();
            if (base == null) {
                return null;
            }
        }
        if (base.equals(file)) {
            return ".";
        } else {
            String filePath = file.getAbsolutePath();
            String basePath = base.getAbsolutePath();
            return getRelativePath(basePath, filePath, File.separatorChar);
        }
    }

    private static String getRelativePath(String basePath, String filePath, char separator) {
        return getRelativePath(basePath, filePath, separator, false);
    }

    private static String ensureEnds(String s, char endsWith) {
        return StringUtils.endsWith(s, "/") ? s : (new StringBuilder()).append(s).append(endsWith).toString();
    }

    private static String getRelativePath(String basePath, String filePath, char separator, boolean caseSensitive) {
        basePath = ensureEnds(basePath, separator);
        int len = 0;
        int lastSeparatorIndex = 0;
        String basePathToCompare = caseSensitive ? basePath : basePath.toLowerCase();
        String filePathToCompare = caseSensitive ? filePath : filePath.toLowerCase();
        if (basePathToCompare.equals(ensureEnds(filePathToCompare, separator))) {
            return ".";
        }
        for (; len < filePath.length() && len < basePath.length() && filePathToCompare.charAt(
                len) == basePathToCompare.charAt(len); len++) {
            if (basePath.charAt(len) == separator) {
                lastSeparatorIndex = len;
            }
        }

        if (len == 0) {
            return null;
        }
        StringBuilder relativePath = new StringBuilder();
        for (int i = len; i < basePath.length(); i++) {
            if (basePath.charAt(i) == separator) {
                relativePath.append("..");
                relativePath.append(separator);
            }
        }

        relativePath.append(filePath.substring(lastSeparatorIndex + 1));
        return relativePath.toString();
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