package org.jfrog.build.extractor.pip.extractor;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jfrog.build.api.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Bar Belity on 09/07/2020.
 */
public class PipLogParser {

    static final Pattern COLLECTING_PACKAGE_PATTERN = Pattern.compile("^Collecting\\s(\\w[\\w-\\.]+)");
    static final Pattern DOWNLOADED_FILE_PATTERN = Pattern.compile("^\\s\\sDownloading\\s[^\\s]*\\/packages\\/[^\\s]*\\/([^\\s]*)");
    static final Pattern INSTALLED_PACKAGE_PATTERN = Pattern.compile("^Requirement\\salready\\ssatisfied\\:\\s(\\w[\\w-\\.]+)");

    /**
     * Parse a pip-install execution log and return the installation packages and files.
     *
     * @param installationLog - Log output of a pip-install execution.
     * @param logger          - The logger.
     * @return the extracted dependencies from provided log, mapping package-name to a downloaded package-file.
     */
    static Map<String, String> parse(String installationLog, Log logger) {
        Map<String, String> downloadedDependencies = new HashMap<>();
        String[] lines = installationLog.split("\\R");
        MutableBoolean expectingPackageFilePath = new MutableBoolean(false);
        String packageName = "";

        for (String line : lines) {
            // Extract downloaded package name.
            Matcher matcher = COLLECTING_PACKAGE_PATTERN.matcher(line);
            if (matcher.find()) {
                packageName = extractPackageName(downloadedDependencies, matcher, packageName, expectingPackageFilePath, logger);
                continue;
            }

            // Extract downloaded file, stored in Artifactory.
            matcher = DOWNLOADED_FILE_PATTERN.matcher(line);
            if (matcher.find()) {
                extractDownloadedFileName(downloadedDependencies, matcher, packageName, expectingPackageFilePath, logger);
                continue;
            }

            // Extract already installed package name.
            matcher = INSTALLED_PACKAGE_PATTERN.matcher(line);
            if (matcher.find()) {
                extractAlreadyInstalledPackage(downloadedDependencies, matcher, logger);
            }
        }

        // If there is a package we are still waiting for its path, save it with empty path.
        if (expectingPackageFilePath.isTrue()) {
            downloadedDependencies.put(StringUtils.lowerCase(packageName), "");
        }

        return downloadedDependencies;
    }

    // Extract downloaded package name.
    static String extractPackageName(Map<String, String> downloadedDependencies, Matcher matcher, String packageName, MutableBoolean expectingPackageFilePath, Log logger) {
        if (expectingPackageFilePath.isTrue()) {
            // This may occur when a package-installation file is saved in pip-cache-dir, thus not being downloaded during the installation.
            // Re-running pip-install with 'no-cache-dir' fixes this issue.
            logger.debug(String.format("Could not resolve download path for package: %s, continuing...", packageName));
            // Save package with empty file path.
            downloadedDependencies.put(StringUtils.lowerCase(packageName), "");
        }

        // Save dependency info.
        expectingPackageFilePath.setTrue();
        return matcher.group(1);
    }

    // Extract downloaded file, stored in Artifactory.
    static void extractDownloadedFileName(Map<String, String> downloadedDependencies, Matcher matcher, String packageName, MutableBoolean expectingPackageFilePath, Log logger) {
        // If this pattern matched before package-name was found, do not collect this path.
        if (expectingPackageFilePath.isFalse()) {
            logger.debug(String.format("Could not determine package-name for path: %s, continuing...", matcher.group(1)));
            return;
        }

        // Save dependency information.
        String filePath = matcher.group(1);
        downloadedDependencies.put(StringUtils.lowerCase(packageName), filePath);
        expectingPackageFilePath.setFalse();
        logger.debug(String.format("Found package: %s installed with: %s", packageName, filePath));
    }

    // Extract already installed packages names.
    static void extractAlreadyInstalledPackage(Map<String, String> downloadedDependencies, Matcher matcher, Log logger) {
        // Save dependency with empty file name.
        downloadedDependencies.put(StringUtils.lowerCase(matcher.group(1)), "");
        logger.debug(String.format("Found package: %s  already installed", matcher.group(1)));
    }
}
