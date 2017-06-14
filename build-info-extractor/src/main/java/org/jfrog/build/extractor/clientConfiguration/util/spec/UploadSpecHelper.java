package org.jfrog.build.extractor.clientConfiguration.util.spec;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.extractor.clientConfiguration.util.FileCollectionUtil;
import org.jfrog.build.extractor.clientConfiguration.util.PathsUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.jfrog.build.extractor.clientConfiguration.util.PathsUtils.removeUnescapedChar;

/**
 * Created by diman on 15/05/2017.
 */
public class UploadSpecHelper {

    /**
     * Calculates the target deployment path of an artifact by it's name
     *
     * @param targetPattern a wildcard pattern of the target path
     * @param artifactFile  the artifact file to calculate target deployment path for
     * @return the calculated target path (supports file renaming).
     */
    public static String wildcardCalculateTargetPath(String targetPattern, File artifactFile) {
        if (targetPattern.endsWith("/") || targetPattern.equals("")) {
            return targetPattern + calculateTargetRelativePath(artifactFile);
        }
        return targetPattern;
    }

    public static String calculateTargetRelativePath(File artifactFile) {
        String relativePath = artifactFile.getAbsolutePath();
        String parentDir = artifactFile.getParent();
        if (!StringUtils.isBlank(parentDir)) {
            relativePath = StringUtils.removeStart(artifactFile.getAbsolutePath(), parentDir);
        }
        relativePath = FilenameUtils.separatorsToUnix(relativePath);
        relativePath = StringUtils.removeStart(relativePath, "/");
        return relativePath;
    }

    /**
     * Building a multi map of target paths mapped to their files using wildcard pattern.
     * The pattern should contain slashes instead of backslashes in case of windows.
     *
     * @param checkoutDir the base directory of which to calculate the given source ant pattern
     * @param pattern     the Ant pattern to calculate the files from
     * @param targetPath  the target path for deployment of a file
     * @return a Multimap containing the targets as keys and the files as values
     * @throws IOException in case of any file system exception
     */
    public static Multimap<String, File> buildPublishingData(
            File checkoutDir, String pattern, String targetPath, boolean flat, boolean recursive, boolean regexp)
            throws IOException {
        boolean isAbsolutePath = (new File(pattern)).isAbsolute();
        List<File> matchedFiles;
        if (regexp) {
            matchedFiles = collectMatchedFilesByRegexp(checkoutDir, pattern, recursive);
        } else {
            matchedFiles = collectMatchedFilesByWildcard(checkoutDir, pattern, recursive);
            // Convert wildcard to regexp
            pattern = PathsUtils.pathToRegExp(pattern);
        }
        return getUploadPathsMap(matchedFiles, checkoutDir, targetPath, flat, Pattern.compile(pattern), isAbsolutePath);
    }

    private static List<File> collectMatchedFilesByRegexp(File checkoutDir, String pattern, boolean recursive)
            throws IOException {
        // This method determines the base directory - the directory where the files scan will be done
        String baseDir = getRegexBaseDir(checkoutDir, pattern);
        // Part of the pattern might move to the base directory, this will remove this part from the pattern
        String newPattern = prepareRegexPattern(checkoutDir, pattern, baseDir);
        // Collects candidate files to execute the regex
        List<String> candidatePaths = FileCollectionUtil.collectFiles(baseDir, newPattern, recursive, true);
        return getMatchedFiles(baseDir, newPattern, candidatePaths);
    }

    private static List<File> collectMatchedFilesByWildcard(File checkoutDir, String pattern, boolean recursive)
            throws IOException {
        // This method determines the base directory - the directory where the files scan will be done
        String baseDir = getWildcardBaseDir(checkoutDir, pattern);
        // Part of the pattern might move to the base directory, this will remove this part from the pattern
        String newPattern = prepareWildcardPattern(checkoutDir, pattern, baseDir);
        // Collects candidate files to execute the regex
        List<String> candidatePaths = FileCollectionUtil.collectFiles(baseDir, newPattern, recursive, false);
        // Convert wildcard to regexp
        newPattern = PathsUtils.pathToRegExp(newPattern);
        return getMatchedFiles(baseDir, newPattern, candidatePaths);
    }

    private static List<File> getMatchedFiles(String baseDir, String newPattern, List<String> candidatePaths) throws IOException {
        List<File> matchedFiles = new ArrayList<File>();
        Pattern regexPattern = Pattern.compile(newPattern);
        File baseDirFile = new File(baseDir);
        for (String path : candidatePaths) {
            File file = new File(path);
            String relativePath = getRelativePath(baseDirFile, file).replace("\\", "/");
            if (regexPattern.matcher(relativePath).matches()) {
                matchedFiles.add(file.getCanonicalFile());
            }
        }
        return matchedFiles;
    }

    public static Multimap<String, File> getUploadPathsMap(List<File> files, File checkoutDir, String targetPath,
                                                           boolean flat, Pattern regexPattern, boolean absolutePath) {
        Multimap<String, File> filePathsMap = HashMultimap.create();

        for (File file : files) {
            String fileTargetPath = targetPath;
            if (StringUtils.endsWith(fileTargetPath, "/") && !flat) {
                fileTargetPath = calculateFileTargetPath(checkoutDir, file, targetPath);
                // handle win file system
                fileTargetPath = fileTargetPath.replace('\\', '/');
            }
            if (absolutePath) {
                fileTargetPath = PathsUtils.reformatRegexp(file.getPath(), fileTargetPath, regexPattern);
            } else {
                fileTargetPath = PathsUtils.reformatRegexp(
                        getRelativePath(checkoutDir, file), fileTargetPath, regexPattern);
            }
            filePathsMap.put(fileTargetPath, file);
        }
        return filePathsMap;
    }

    /**
     * Returns base directory path with slash in the end.
     * The base directory is the path where the file collection starts from.
     *
     * Base directory is the last directory that not contains wildcards in case of regexp=false.
     * In case of regexp=true the base directory will be the last existing directory that not contains a regex char.
     * @param checkoutDir the checkout directory
     * @param pattern the pattern provided by the user
     * @return String that represents the base directory
     */
    private static String getWildcardBaseDir(File checkoutDir, String pattern) throws FileNotFoundException {
        String baseDir = getWildcardAbsolutePattern(checkoutDir, pattern);
        baseDir = StringUtils.substringBefore(baseDir, "*");
        baseDir = StringUtils.substringBefore(baseDir, "?");
        baseDir = baseDir.substring(0, baseDir.lastIndexOf("/") + 1);
        // Parenthesis are used for capture groups and if not part of the path should be removed.
        // all path parenthesis should be escaped before this stage.
        baseDir = removeParenthesis(baseDir);
        // getAbsolutePattern method escapes path parenthesis. after cleaning the capture group
        // parenthesis we need to remove the EscapeChar.
        return removeParenthesisEscapeChar(baseDir);
    }

    /**
     * Returns base directory path with slash in the end.
     * The base directory is the path where the file collection starts from.
     *
     * Base directory is the last directory that not contains wildcards in case of regexp=false.
     * In case of regexp=true the base directory will be the last existing directory that not contains a regex char.
     * @param checkoutDir the checkout directory
     * @param pattern the pattern provided by the user
     * @return String that represents the base directory
     */
    private static String getRegexBaseDir(File checkoutDir, String pattern) throws FileNotFoundException {
        String baseDir = getRegexpAbsolutePattern(checkoutDir, pattern);

        baseDir = getExistingPath(baseDir);
        if (StringUtils.isEmpty(baseDir)) {
            throw new FileNotFoundException("Could not find any base path in the pattern: " + pattern);
        }
        if (!baseDir.endsWith("/")) {
            baseDir = baseDir + "/";
        }
        return baseDir;
    }

    /**
     * The user can provide pattern that will contain static path (without wildcards) that will become part of the base directory.
     * this method removes the part of the pattern that exists in the base directory.
     * @param checkoutDir the checkout directory
     * @param pattern the provided by the user pattern
     * @param baseDir the calculated base directory
     * @return new calculated pattern based on the provided patern and base directory
     */
    private static String prepareRegexPattern(File checkoutDir, String pattern, String baseDir) {
        String absolutePattern = getRegexpAbsolutePattern(checkoutDir, pattern);
        // String.replaceFirst fails to find some strings with regexp therefore StringUtils.substringAfter is used
        String newPattern = cleanRegexpPattern(absolutePattern, baseDir);
        newPattern = removeLeadingSeparator(newPattern);
        if (pattern.endsWith("/")) {
            newPattern = newPattern + ".*";
        }
        return newPattern;
    }

    /**
     * The user can provide pattern that will contain static path (without wildcards) that will become part of the base directory.
     * this method removes the part of the pattern that exists in the base directory.
     * @param checkoutDir the checkout directory
     * @param pattern the provided by the user pattern
     * @param baseDir the calculated base directory
     * @return new calculated pattern based on the provided patern and base directory
     */
    private static String prepareWildcardPattern(File checkoutDir, String pattern, String baseDir) {
        String absolutePattern = getWildcardAbsolutePattern(checkoutDir, pattern);
        // String.replaceFirst fails to find some strings with regexp therefore StringUtils.substringAfter is used.
        // absolutePattern may contain escaped parenthesis (which are part of the path) and not escaped parenthesis
        // (which are part of the capture groups). The not escaped parenthesis and the the escape char of the
        // escaped parenthesis should be removed from the new pattern.
        String newPattern = StringUtils.substringAfter(removeParenthesisEscapeChar(removeParenthesis(absolutePattern)), baseDir);
        newPattern = removeLeadingSeparator(newPattern);
        if (pattern.endsWith("/")) {
            newPattern = newPattern + "*";
        }
        return newPattern;
    }

    private static String removeLeadingSeparator(String newPattern) {
        if (newPattern.startsWith("/")) {
            newPattern = newPattern.substring(1);
        }
        return newPattern;
    }

    /**
     * Gets the relative path of a given file to the base
     *
     * @param base the base path to calculate the relative path
     * @param file the file itself
     * @return the calculated relative path
     */
    public static String getRelativePath(File base, File file) {
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

    public static String calculateFileTargetPath(File patternDir, File file, String targetPath) {
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

    /**
     * Returns the absolute path of pattern.
     * If the provided by the user pattern is absolute same pattern will be returned.
     * If the pattern is relative the checkout directory will be prepend and all the parentheses of the checkout path will be escaped.
     * in case of regexp=true escape chars will be prepended to regex chars in the checkout directory path.
     *
     * @param checkoutDir the checkout directory
     * @param pattern the provided by the user patter, can be absolute or relative
     * @return the absolute path of pattern
     */
    private static String getRegexpAbsolutePattern(File checkoutDir, String pattern) {
        if (new File(pattern).isAbsolute()) {
            return pattern;
        }
        String escapedCheckoutDir = PathsUtils.escapeRegexChars(checkoutDir.getAbsolutePath().replace("\\", "/"));
        return escapedCheckoutDir + "/" + pattern;
    }

    /**
     * Returns the absolute path of pattern.
     * If the provided by the user pattern is absolute same pattern will be returned.
     * If the pattern is relative the checkout directory will be prepend and all the parentheses of the checkout path will be escaped.
     * in case of regexp=true escape chars will be prepended to regex chars in the checkout directory path.
     *
     * @param checkoutDir the checkout directory
     * @param pattern the provided by the user patter, can be absolute or relative
     * @return the absolute path of pattern
     */
    private static String getWildcardAbsolutePattern(File checkoutDir, String pattern) {
        if (new File(pattern).isAbsolute()) {
            return pattern;
        }
        // In case of wildcard when collecting files not escaped parentheses will be removed in the next steps so we need to escape them
        return escapeParentheses(checkoutDir.getAbsolutePath().replace("\\", "/")) + "/" + pattern;
    }

    /**
     * Returns the last existing directory of the provided baseDire that not contains a regex characters.
     * @param baseDir the path to search in
     * @return the last existing directory of the provided baseDire that not contains a regex characters
     */
    private static String getExistingPath(String baseDir) {
        baseDir = PathsUtils.substringBeforeFirstRegex(removeParenthesis(baseDir));
        while (!new File(baseDir).isDirectory() && baseDir.contains("/")) {
            baseDir = StringUtils.substringBeforeLast(baseDir, "/");
        }
        return baseDir;
    }

    private static String removeParenthesisEscapeChar(String baseDir) {
        return baseDir.replace("\\)", ")").replace("\\(", "(");
    }

    private static String removeParenthesis(String baseDir) {
        baseDir = removeUnescapedChar(baseDir ,"(".charAt(0));
        baseDir = removeUnescapedChar(baseDir ,")".charAt(0));
        return baseDir;
    }

    private static String escapeParentheses(String path) {
        return path.replace("(", "\\(").replace(")","\\)");
    }

    private static String cleanRegexpPattern(String absolutePattern, String baseDir) {
        String separator = "/";
        while (baseDir.contains(separator)) {
            baseDir = StringUtils.substringAfter(baseDir, separator);
            absolutePattern = StringUtils.substringAfter(absolutePattern, separator);
        }
        // Placeholder parenthesis may be opened in the basedir as part of placeholder so the closing bracket should be removed.
        return cleanUnopenedParenthesis(absolutePattern);
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

    private static String stripFileNameFromPath(String relativePath) {
        File file = new File(relativePath);
        return file.getPath().substring(0, file.getPath().length() - file.getName().length());
    }

    /**
     * This method removes parenthesis that was not opened in the string.
     * In other words, every ')' char that has no '(' in front of it (regardless of the text in between them) will be deleted.
     * For example, "aaa)bbb(c(ddd)e)ff)" will return "aaabbb(c(ddd)e)ff".
     * @param pattern the string to remove from
     * @return string without unopened parenthesis
     */
    private static String cleanUnopenedParenthesis(String pattern) {
        int length = pattern.length();
        int numberOfUnclosedParenthesis = 0;
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            char c = pattern.charAt(i);
            if (c == ")".charAt(0)) {
                if (numberOfUnclosedParenthesis > 0) {
                    numberOfUnclosedParenthesis--;
                    stringBuilder.append(c);
                }
            } else {
                stringBuilder.append(c);
                if (c == "(".charAt(0)) {
                    numberOfUnclosedParenthesis++;
                }
            }
        }
        return stringBuilder.toString();
    }

    private static String ensureEnds(String s, char endsWith) {
        return StringUtils.endsWith(s, "/") ? s : (new StringBuilder()).append(s).append(endsWith).toString();
    }
}
