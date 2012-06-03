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
import com.google.common.collect.Multimap;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Helper class for calculating published artifacts of a generic deployment.
 *
 * @author Noam Y. Tenne
 */
public class PublishedItemsHelper {

    /**
     * Splits a given property value to pairs of source and target strings (the splitter is '=>'
     * the source represents the Ant Pattern to search for
     * the target represents the target path to deploy the found artifacts
     * Multi values as acceptable by new lined or comma separated.
     *
     * @param publishedItemsPropertyValue the string value to split, if the splitter '=>' was not found
     *                                    then the value is treated as a source only (target will be "").
     * @return a Map containing the sources as keys and targets as values
     */
    public static Multimap<String, String> getPublishedItemsPatternPairs(String publishedItemsPropertyValue) {
        Multimap<String, String> patternPairMap = HashMultimap.create();
        if (StringUtils.isNotBlank(publishedItemsPropertyValue)) {

            List<String> patternPairs = parsePatternsFromProperty(publishedItemsPropertyValue);
            for (String patternPair : patternPairs) {

                String[] splitPattern = patternPair.split("=>");

                String sourcePattern = "";
                String targetPattern = "";

                if (splitPattern.length > 0) {
                    sourcePattern = FilenameUtils.separatorsToUnix(splitPattern[0].trim());
                }

                // We allow an empty target, in that case it will be ""
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

    /**
     * Splits the given property value by new lines or by commas.
     *
     * @param publishedItemsPropertyValue The property value to split
     * @return a List of the splinted parameter by new lines or commas.
     */
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

    /**
     * Building a multi map of target paths mapped to their files.
     *
     * @param checkoutDir the base directory of which to calculate the given source ant pattern
     * @param pattern     the Ant pattern to calculate the files from
     * @param targetPath  the target path for deployment of a file
     * @return a Multimap containing the targets as keys and the files as values
     * @throws IOException in case of any file system exception
     */
    public static Multimap<String, File> buildPublishingData(File checkoutDir, String pattern, String targetPath)
            throws IOException {
        final Multimap<String, File> filePathsMap = HashMultimap.create();
        File patternAbsolutePath = getAbsolutePath(checkoutDir, pattern);
        if (patternAbsolutePath.isFile()) {
            // The given pattern is an absolute path of just one file, let's add it to our result map
            filePathsMap.put(targetPath, patternAbsolutePath);
        } else {
            Pattern filePattern = null;
            File patternDir = null;
            if (patternAbsolutePath.isDirectory()) {
                // The given pattern is a path to a directory, we need to return all it's content
                filePattern = Pattern.compile(".*");
                patternDir = patternAbsolutePath;
            } else if (pattern.indexOf('*') >= 0 || pattern.indexOf('?') >= 0) {
                // We are dealing with complex Ant pattern, need to analyze it
                File baseTruncationDir = getBaseTruncationDir(patternAbsolutePath);
                patternDir = baseTruncationDir != null ? baseTruncationDir : checkoutDir;

                // If the checkout dir is an ancestor of the pattern path then
                // we cut the checkout dir from the pattern
                boolean isCheckoutDirAncestor = isAncestor(checkoutDir, patternAbsolutePath);
                if (isCheckoutDirAncestor) {
                    pattern = pattern.substring(
                            patternDir.getAbsolutePath().length() - checkoutDir.getAbsolutePath().length());
                }

                // If the pattern absolute path starts with the pattern directory path
                // then we need to cut the pattern to be only the relative path
                String patternAbsolutePathUrl = patternAbsolutePath.getAbsolutePath();
                if (!StringUtils.isBlank(patternAbsolutePathUrl) && patternAbsolutePathUrl.startsWith(
                        patternDir.getAbsolutePath())) {
                    pattern = getRelativePath(patternDir, patternAbsolutePath);
                }

                // All done, we can now convert and compile from Ant pattern to regular expression
                filePattern = Pattern.compile(convertAntToRegexp(pattern));
            }

            // If we successfully converted from Ant pattern to a regular expression
            // then it's time to collect all our artifacts according to this regular expression
            if (filePattern != null) {
                List<File> files = new ArrayList<File>();
                collectMatchedFiles(patternDir, patternDir, filePattern, files);
                for (File file : files) {
                    String fileTargetPath = calculateFileTargetPath(patternDir, file, targetPath);
                    filePathsMap.put(fileTargetPath, file);
                }
            }
        }

        return filePathsMap;
    }

    private static String calculateFileTargetPath(File patternDir, File file, String targetPath) {
        String relativePath = getRelativePath(patternDir, file);
        relativePath = stripFileNameFromPath(relativePath);
        if (targetPath.length() == 0) {
            return relativePath;
        }
        if (relativePath.length() == 0) {
            return targetPath;
        } else {
            return (new StringBuilder()).append(targetPath).append('/').append(relativePath).toString();
        }
    }

    private static String stripFileNameFromPath(String relativePath) {
        File file = new File(relativePath);
        return file.getPath().substring(0, file.getPath().length() - file.getName().length());
    }

    /**
     * Calculates the absolute path of the given Ant pattern relatively to the given base directory
     *
     * @param baseDir the base directory to use in case the pattern is not absolute
     * @param pattern the pattern to which calculate an absolute path
     * @return File representation of the calculated absolute path
     */
    private static File getAbsolutePath(File baseDir, String pattern) {
        File pathFile = new File(pattern);
        if (pathFile.isAbsolute()) {
            return pathFile;
        }

        File rawFile = new File(baseDir, pattern);
        if (baseDir.getPath().startsWith("\\\\")) {
            return rawFile;
        }

        return new File(rawFile.toURI().normalize().getPath());
    }

    /**
     * Gets the base directory of a given File representation of an Ant pattern
     * without the pattern itself
     *
     * @param antPatternDir the Ant pattern directory to calculate the base directory from
     * @return the base directory of the given Ant pattern, null if there isn't any
     */
    private static File getBaseTruncationDir(File antPatternDir) {
        String dirWithoutPattern = getDirWithoutPattern(antPatternDir.getPath());
        if ("".equals(dirWithoutPattern)) {
            return null;
        } else {
            return new File(dirWithoutPattern);
        }
    }

    /**
     * Returns the directory path of the given Ant pattern without the pattern itself.
     *
     * @param pathWithWildCard an Ant pattern containing wildcards
     * @return the path to the directory without the Ant pattern
     */
    private static String getDirWithoutPattern(String pathWithWildCard) {
        String t = pathWithWildCard.replace('\\', '/');
        int firstStar = t.indexOf('*');
        int firstQuestion = t.indexOf('?');
        int mark;
        if (firstStar >= 0) {
            if (firstStar >= firstQuestion && firstQuestion >= 0) {

                mark = firstQuestion;
            } else {
                mark = firstStar;
            }
        } else {
            mark = firstQuestion;
        }

        int lastSlash = t.lastIndexOf('/', mark);
        return lastSlash <= 0 ? "" : pathWithWildCard.substring(0, lastSlash);
    }

    /**
     * Checks if a given directory is an ansector (file system speaking)
     * of a certain directory
     *
     * @param ancestor The directory to check if it's an ancestor of
     * @param child    The child directory
     * @return true if ancestor if an ancestor of child, false otherwise
     * @throws IOException
     */
    private static boolean isAncestor(File ancestor, File child) throws IOException {
        File parent = child;
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

    /**
     * Gets the relative path of a given file to the base
     *
     * @param base the base path to calculate the relative path
     * @param file the file itself
     * @return the calculated relative path
     */
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
        basePath = ensureEnds(basePath, separator);
        int len = 0;
        int lastSeparatorIndex = 0;
        String basePathToCompare = basePath.toLowerCase();
        String filePathToCompare = filePath.toLowerCase();
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

    private static String ensureEnds(String s, char endsWith) {
        return StringUtils.endsWith(s, "/") ? s : (new StringBuilder()).append(s).append(endsWith).toString();
    }

    /**
     * Calculates the target deployment path of an artifact by it's name
     *
     * @param targetPattern an Ant pattern of the target path
     * @param artifactFile  the artifact file to calculate target deployment path for
     * @param rootDir       the root dir to calculate relativity to
     * @return the calculated target path
     */
    public static String calculateTargetPath(String targetPattern, File artifactFile) {
        String relativePath = calculateTargetRelativePath(artifactFile);
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
            return targetPattern + "/" + artifactFile.getName();
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

    private static String calculateTargetRelativePath(File artifactFile) {
        String relativePath = artifactFile.getAbsolutePath();
        String parentDir = artifactFile.getParent();
        if (!StringUtils.isBlank(parentDir)) {
            relativePath = StringUtils.removeStart(artifactFile.getAbsolutePath(), parentDir);
        }
        relativePath = FilenameUtils.separatorsToUnix(relativePath);
        relativePath = StringUtils.removeStart(relativePath, "/");
        return relativePath;
    }
}