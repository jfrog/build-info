package org.jfrog.build.extractor.clientConfiguration.util.spec;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.build.extractor.clientConfiguration.util.PathsUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.jfrog.build.extractor.clientConfiguration.util.PathsUtils.removeUnescapedChar;

/**
 * Created by diman on 15/05/2017.
 */
public class UploadSpecHelper {

    private static final String SHA1 = "SHA1";
    private static final String MD5 = "MD5";

    /**
     * Create a DeployDetails from the given properties
     *
     * @param targetPath target of the created artifact in Artifactory
     * @param artifactFile the artifact to deploy
     * @param uploadTarget target repository in Artifactory
     * @param explode explode archive
     * @param props properties to attach to the deployed file
     * @param buildProperties a map of properties to add to the DeployDetails objects
     */
    public static DeployDetails buildDeployDetails(String targetPath, File artifactFile,
                                                   String uploadTarget, String explode, String props,
                                                   Multimap<String, String> buildProperties)
            throws IOException, NoSuchAlgorithmException {
        String path = UploadSpecHelper.wildcardCalculateTargetPath(targetPath, artifactFile);
        path = StringUtils.replace(path, "//", "/");

        // calculate the sha1 checksum and add it to the deploy artifactsToDeploy
        Map<String, String> checksums;
        try {
            checksums = FileChecksumCalculator.calculateChecksums(artifactFile, SHA1, MD5);
        } catch (NoSuchAlgorithmException e) {
            throw new NoSuchAlgorithmException(
                    String.format("Could not find checksum algorithm for %s or %s.", SHA1, MD5), e);
        }
        DeployDetails.Builder builder = new DeployDetails.Builder()
                .file(artifactFile)
                .artifactPath(path)
                .targetRepository(getRepositoryKey(uploadTarget))
                .md5(checksums.get(MD5)).sha1(checksums.get(SHA1))
                .explode(BooleanUtils.toBoolean(explode))
                .addProperties(SpecsHelper.getPropertiesMap(props))
                .packageType(DeployDetails.PackageType.GENERIC);
        if (buildProperties != null && !buildProperties.isEmpty()) {
            builder.addProperties(buildProperties);
        }

        return builder.build();
    }

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
     * Gets the relative path of a given file to the workspace path.
     * @param absolutePath the file's absolute path
     * @param workspacePath path to the job's workspace
     * @return the relative path of a given file to the workspace path
     */
    public static String getRelativeToWsPath(String absolutePath, String workspacePath) {
        String absoluteFilePath = absolutePath.replace("\\", "/");
        workspacePath = workspacePath.replace("\\", "/");
        return absoluteFilePath.substring(workspacePath.length() + 1);
    }

    public static String prepareExcludePattern(String[] excludePatterns, boolean isWildcard, boolean recursive) {
        StringBuilder patternSb = new StringBuilder();
        if (!ArrayUtils.isEmpty(excludePatterns)) {
            for (String pattern : excludePatterns) {
                if (StringUtils.isBlank(pattern)) {
                    continue;
                }
                if (isWildcard) {
                    pattern = PathsUtils.pathToRegExp(pattern);
                }
                if (recursive && pattern.endsWith(File.pathSeparator)) {
                    pattern += ".*";
                }
                patternSb.append("(").append(pattern).append(")|");
            }
            if (patternSb.length() > 0) {
                patternSb.deleteCharAt(patternSb.length() - 1);

            }
        }
        return patternSb.toString();
    }

    protected static String getUploadPath(File file, Pattern pathPattern, String targetPath, boolean isFlat,
                                        boolean isAbsolutePath, File workspaceDir, boolean isTargetDirectory) {
        String sourcePath;
        String fileTargetPath = targetPath;
        if (isTargetDirectory && !isFlat) {
            fileTargetPath = UploadSpecHelper.calculateFileTargetPath(workspaceDir, file, targetPath);
        }

        if (isAbsolutePath) {
            if (!isFlat) {
                if (isTargetDirectory) {
                    fileTargetPath = targetPath + file.getPath();
                } else {
                    fileTargetPath = targetPath;
                }
            }
            sourcePath = file.getPath();
        } else {
            sourcePath = UploadSpecHelper.getRelativePath(workspaceDir, file);
        }

        return PathsUtils.reformatRegexp(sourcePath, fileTargetPath.replace('\\', '/'), pathPattern);
    }

    public static Multimap<String, File> getUploadPathsMap(List<File> files, File workspaceDir, String targetPath,
                                                           boolean isFlat, Pattern regexPattern, boolean isAbsolutePath) {
        Multimap<String, File> filePathsMap = HashMultimap.create();
        boolean isTargetDirectory = StringUtils.endsWith(targetPath, "/");

        for (File file : files) {
            String fileTargetPath = getUploadPath(file, regexPattern, targetPath, isFlat, isAbsolutePath, workspaceDir, isTargetDirectory);
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
     * @param workspaceDir the workspaceDir directory
     * @param pattern the pattern provided by the user
     * @return String that represents the base directory
     */
    public static String getWildcardBaseDir(File workspaceDir, String pattern) {
        String baseDir = getWildcardAbsolutePattern(workspaceDir, pattern);
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
     * @param workspaceDir the workspaceDir directory
     * @param pattern the pattern provided by the user
     * @return String that represents the base directory
     */
    public static String getRegexBaseDir(File workspaceDir, String pattern) throws FileNotFoundException {
        String baseDir = getRegexpAbsolutePattern(workspaceDir, pattern);

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
     * @param workspaceDir the workspaceDir directory
     * @param pattern the provided by the user pattern
     * @param baseDir the calculated base directory
     * @return new calculated pattern based on the provided patern and base directory
     */
    public static String prepareRegexPattern(File workspaceDir, String pattern, String baseDir) {
        String absolutePattern = getRegexpAbsolutePattern(workspaceDir, pattern);
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
     * @param workspaceDir the workspaceDir directory
     * @param pattern the provided by the user pattern
     * @param baseDir the calculated base directory
     * @return new calculated pattern based on the provided patern and base directory
     */
    public static String prepareWildcardPattern(File workspaceDir, String pattern, String baseDir) {
        String absolutePattern = getWildcardAbsolutePattern(workspaceDir, pattern);
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
        }

        String filePath = file.getAbsolutePath();
        String basePath = base.getAbsolutePath();

        basePath = ensureEndsWithSeparator(basePath);
        int len = 0;
        int lastSeparatorIndex = 0;
        String basePathToCompare = basePath.toLowerCase();
        String filePathToCompare = filePath.toLowerCase();
        if (basePathToCompare.equals(ensureEndsWithSeparator(filePathToCompare))) {
            return ".";
        }
        for (; len < filePath.length() && len < basePath.length() && filePathToCompare.charAt(
                len) == basePathToCompare.charAt(len); len++) {
            if (basePath.charAt(len) == File.separatorChar) {
                lastSeparatorIndex = len;
            }
        }
        if (len == 0) {
            return null;
        }
        StringBuilder relativePath = new StringBuilder();
        for (int i = len; i < basePath.length(); i++) {
            if (basePath.charAt(i) == File.separatorChar) {
                relativePath.append("..");
                relativePath.append(File.separator);
            }
        }
        relativePath.append(filePath.substring(lastSeparatorIndex + 1));
        return relativePath.toString();
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
     * Remove repository's name from a given Spec's target
     * @param path target path in Artifactory
     * @return the local path inside the repository
     */
    public static String getLocalPath(String path) {
        path = StringUtils.substringAfter(path, "/");
        // When the path is the root of the repo substringAfter will return empty string. in such case slash need to be added
        if ("".equals(path)) {
            return "/";
        }
        return path;
    }

    /**
     * Returns the absolute path of pattern.
     * If the provided by the user pattern is absolute same pattern will be returned.
     * If the pattern is relative the checkout directory will be prepend and all the parentheses of the checkout path will be escaped.
     * in case of regexp=true escape chars will be prepended to regex chars in the checkout directory path.
     *
     * @param workspaceDir the workspaceDir directory
     * @param pattern the provided by the user patter, can be absolute or relative
     * @return the absolute path of pattern
     */
    private static String getRegexpAbsolutePattern(File workspaceDir, String pattern) {
        if (new File(pattern).isAbsolute()) {
            return pattern;
        }
        String escapedWorkspaceDir = PathsUtils.escapeRegexChars(workspaceDir.getAbsolutePath().replace("\\", "/"));
        return escapedWorkspaceDir + "/" + pattern;
    }

    /**
     * Returns the absolute path of pattern.
     * If the provided by the user pattern is absolute same pattern will be returned.
     * If the pattern is relative the checkout directory will be prepend and all the parentheses of the checkout path will be escaped.
     * in case of regexp=true escape chars will be prepended to regex chars in the checkout directory path.
     *
     * @param workspaceDir the workspaceDir directory
     * @param pattern the provided by the user patter, can be absolute or relative
     * @return the absolute path of pattern
     */
    private static String getWildcardAbsolutePattern(File workspaceDir, String pattern) {
        if (new File(pattern).isAbsolute()) {
            return pattern;
        }
        // In case of wildcard when collecting files not escaped parentheses will be removed in the next steps so we need to escape them
        return escapeParentheses(workspaceDir.getAbsolutePath().replace("\\", "/")) + "/" + pattern;
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

    private static String ensureEndsWithSeparator(String s) {
        return StringUtils.endsWith(s, File.separator) ? s : s + File.separator;
    }

    private static String getRepositoryKey(String path) {
        return StringUtils.substringBefore(path, "/");
    }
}
