package org.jfrog.build.extractor.clientConfiguration.util;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by diman on 02/01/2017.
 */
public class FileCollectionUtil {

    /**
     * Return all files in the provided directory that can match the pattern.
     * the number of files is limited to 1M files.
     *
     * @param baseDir the directory to get files from
     * @param fixedPattern the pattern it can match
     * @param isRecursive is the spec recursive
     * @param regexp dose the spec regex
     * @return all files in the base directory that can match the pattern.
     */
    public static List<String> collectFiles(String baseDir, String fixedPattern, boolean isRecursive, boolean regexp) {
        File[] filesToScan = new File(baseDir).listFiles();
        List<String> results = new ArrayList<String>();
        if (filesToScan != null) {
            for (File file : filesToScan) {
                if (file.isFile()) {
                    results.add(file.getAbsolutePath().replace("\\", "/"));
                    isPassedLimit(results.size());
                } else if (isRecursive) {
                    results.addAll(collectFiles(file.getAbsolutePath(), -1, results.size()));
                } else if (!regexp) {
                    // In case of not recursive wildcard pattern we can stop scanning in certain depth.
                    // this depth is when the number of slashes in the path and base directory with pattern are equals.
                    int numberOfSeparators = StringUtils.countMatches(fixedPattern, "/") + StringUtils.countMatches(baseDir, "/");
                    results.addAll(collectFiles(file.getAbsolutePath(), numberOfSeparators, results.size()));
                }
            }
        }
        return results;
    }

    /**
     * Returns all files that exists in the provided directory and its subdirectories.
     * scanning to max depth provided by the user.
     * limited to 1m files minus provided by the user numberOfCandidates
     * @param dir the directory to search in
     * @param numberOfCandidates the number of already existing candidates
     * @return all files that exists in the provided directory and its subdirectories till the provided depth.
     */
    private static List<String> collectFiles(String dir, int depth, int numberOfCandidates) {
        List<String> result = new ArrayList<String>();
        List<String> foldersToScan = new LinkedList<String>();
        foldersToScan.add(dir);

        for (int i = 0; i < foldersToScan.size(); i++) {
            String folderString = foldersToScan.get(i);
            if (depth == -1 || StringUtils.countMatches(folderString, File.separator) <= depth) {
                File file = new File(folderString);
                List<File> folderContent = new ArrayList<File>();
                File[] fileList = file.listFiles();
                if (fileList != null) {
                    folderContent.addAll(Arrays.asList(fileList));
                }
                if (!folderContent.isEmpty()) {
                    for (File entry : folderContent) {
                        if (entry.isDirectory()) {
                            foldersToScan.add(entry.getAbsolutePath());
                        } else {
                            // File can be candidate only if it in the correct depth or if the spec is recursive (depth == -1)
                            if (depth == -1 || StringUtils.countMatches(entry.getPath(), File.separator) == depth) {
                                result.add(entry.getAbsolutePath().replace("\\", "/"));
                                isPassedLimit(numberOfCandidates + result.size());
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Throws exception if more than 1M candidate files provided by numberOfFiles
     * @param numberOfFiles the number of found candidates
     */
    private static void isPassedLimit(int numberOfFiles) {
        int limit = 1000000;
        if (numberOfFiles >= limit) {
            throw new IllegalStateException("Too many candidate files found.");
        }
    }

}
