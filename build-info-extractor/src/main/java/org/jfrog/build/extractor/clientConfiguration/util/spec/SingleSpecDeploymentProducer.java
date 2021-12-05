package org.jfrog.build.extractor.clientConfiguration.util.spec;

import com.google.common.collect.Multimap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.build.extractor.clientConfiguration.util.PathsUtils;
import org.jfrog.build.extractor.producerConsumer.ProducerConsumerExecutor;
import org.jfrog.filespecs.entities.FilesGroup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Actual FileSpec performer, scans the file-system for matching files and creates the deployment data.
 * Handles a single Spec from the 'files' section of a FileSpec.
 *
 * Created by Bar Belity on 07/03/2018.
 */
public class SingleSpecDeploymentProducer {

    private FilesGroup spec;
    private File workspace;
    private Multimap<String, String> buildProperties;

    private Pattern regexpPattern;
    private Pattern regexpExcludePattern;
    private Pattern pathPattern;
    private String targetPath;
    private String baseDir;
    private File baseDirFile;
    private boolean isTargetDirectory;
    private boolean isFlat;
    private boolean isRecursive;
    private boolean isRegexp;
    private boolean isAbsolutePath;
    private int separatorsCount;
    private Set<String> symlinkSet = new HashSet<>();

    SingleSpecDeploymentProducer(FilesGroup spec, File workspace, Multimap<String, String> buildProperties) {
        this.spec = spec;
        this.workspace = workspace;
        this.buildProperties = buildProperties;
    }

    /**
     * Executes a single FileSpec.
     * Find all files matching the spec, create and publish its DeployDetails.
     * @param deploymentSet Set containing the DeployDetails to deploy
     */
    public void executeSpec(Set<DeployDetails> deploymentSet, ProducerConsumerExecutor executor)
            throws IOException, NoSuchAlgorithmException, InterruptedException {
        init();
        File[] filesToScan = new File(baseDir).listFiles();
        if (filesToScan == null) {
            return;
        }

        for (File file : filesToScan) {
            if (Thread.interrupted()) {
                Thread.currentThread().interrupt();
                break;
            }

            if (file.isFile()) {
                processDeployCandidate(file, deploymentSet, executor);
                continue;
            }
            if (isRecursive) {
                collectFiles(file.getAbsolutePath(), -1, deploymentSet, executor);
                continue;
            }
            if (!isRegexp) {
                // In case of not recursive wildcard pattern we can stop scanning in certain depth.
                // This depth is when the number of slashes in the path and base directory with pattern are equal.
                collectFiles(file.getAbsolutePath(), separatorsCount, deploymentSet, executor);
            }
        }
    }

    /**
     * Helper method for calculating and setting fields for the FileSpec currently processed
     */
    private void init() throws FileNotFoundException {
        isFlat = !"false".equalsIgnoreCase(spec.getFlat());
        isRecursive = !"false".equalsIgnoreCase(spec.getRecursive());
        isRegexp = BooleanUtils.toBoolean(spec.getRegexp());
        targetPath = UploadSpecHelper.getLocalPath(spec.getTarget());
        isTargetDirectory = StringUtils.endsWith(targetPath, "/");

        // Extract pattern
        String pattern = spec.getPattern();
        isAbsolutePath = (new File(pattern)).isAbsolute();

        String newPattern;
        String patternForPath;

        if (isRegexp) {
            baseDir = UploadSpecHelper.getRegexBaseDir(workspace, pattern);
            // Part of the pattern might move to the base directory, this will remove this part from the pattern
            newPattern = UploadSpecHelper.prepareRegexPattern(workspace, pattern, baseDir);
            // Calculate pattern
            regexpPattern = Pattern.compile(newPattern);
            patternForPath = pattern;
        } else {
            baseDir = UploadSpecHelper.getWildcardBaseDir(workspace, pattern);
            // Part of the pattern might move to the base directory, this will remove this part from the pattern
            newPattern = UploadSpecHelper.prepareWildcardPattern(workspace, pattern, baseDir);
            // Convert wildcard to regexp and calculate pattern
            regexpPattern = Pattern.compile(PathsUtils.pathToRegExp(newPattern));
            // Convert wildcard to regexp before getUploadPathsMap
            patternForPath = PathsUtils.pathToRegExp(pattern);
        }

        // Calculate number of separators
        separatorsCount = StringUtils.countMatches(newPattern, "/") + StringUtils.countMatches(baseDir, "/");
        // Calculate pattern for path
        pathPattern = Pattern.compile(patternForPath);
        // Calculate exclude pattern
        String[] exclusions = spec.getExclusions();
        if (ArrayUtils.isEmpty(exclusions)) {
            // Support legacy exclude patterns. 'Exclude patterns' are deprecated and replaced by 'exclusions'.
            exclusions = spec.getExcludePatterns();
        }
        String excludePattern = UploadSpecHelper.prepareExcludePattern(exclusions, !isRegexp, isRecursive);
        regexpExcludePattern = StringUtils.isBlank(excludePattern) ? null : Pattern.compile(excludePattern);
        // Calculate base directory
        baseDirFile = new File(baseDir);
    }

    /**
     * Check all file candidates for upload in the provided directory, to specified depth.
     * @param dir base directory to start search for files
     * @param depth level of folders to search in
     * @param deploymentSet Set containing the DeployDetails to deploy
     */
    private void collectFiles(String dir, int depth, Set<DeployDetails> deploymentSet, ProducerConsumerExecutor executor)
            throws IOException, NoSuchAlgorithmException, InterruptedException {
        List<String> foldersToScan = new LinkedList<>();
        foldersToScan.add(dir);

        for (int i = 0; i < foldersToScan.size(); i++) {
            if (Thread.interrupted()) {
                Thread.currentThread().interrupt();
                break;
            }

            String folderString = foldersToScan.get(i);

            if (depth != -1 && !(StringUtils.countMatches(folderString, File.separator) <= depth)) {
                continue;
            }

            File file = new File(folderString);
            List<File> folderContent = new ArrayList<>();
            File[] fileList = file.listFiles();
            if (fileList != null) {
                folderContent.addAll(Arrays.asList(fileList));
            }
            if (folderContent.isEmpty()) {
                continue;
            }

            for (File entry : folderContent) {
                if (Thread.interrupted()) {
                    Thread.currentThread().interrupt();
                    break;
                }

                if (entry.isDirectory()) {
                    if (Files.isSymbolicLink(entry.toPath())) {
                        // If symlink already exists, don't add it to foldersToScan
                        if (!symlinkSet.add(entry.getCanonicalPath())) {
                            continue;
                        }
                    }
                    foldersToScan.add(entry.getAbsolutePath());
                    continue;
                }
                // File can be candidate only if it in the correct depth or if the spec is recursive (depth == -1)
                if (depth == -1 || StringUtils.countMatches(entry.getPath(), File.separator) == depth) {
                    // Send this path for further process
                    processDeployCandidate(entry, deploymentSet, executor);
                }
            }
        }
    }

    /**
     * Receives a candidate file to upload, creates DeployDetails for the file in case should upload it.
     * Adds the DeployDetails to the BlockingQueue.
     * @param file upload candidate
     * @param deploymentSet Set containing the DeployDetails to deploy
     */
    private void processDeployCandidate(File file, Set<DeployDetails> deploymentSet, ProducerConsumerExecutor executor)
            throws IOException, NoSuchAlgorithmException, InterruptedException {
        String filePath = file.getAbsolutePath().replace("\\", "/");

        // Check if matches pattern
        if (!isFileMatchPattern(filePath, regexpPattern, regexpExcludePattern, workspace, baseDirFile)) {
            return;
        }

        // Get the upload path
        String uploadPath = UploadSpecHelper.getUploadPath(file, pathPattern, targetPath, isFlat, isAbsolutePath, workspace, isTargetDirectory);

        // Create DeployDetails
        DeployDetails deployDetails = UploadSpecHelper.buildDeployDetails(uploadPath, file, spec.getTarget(),
                spec.getExplode(), spec.getProps(), buildProperties);

        // Add the created DeploymentDetails if artifact hasn't been added for deployment yet
        if (deploymentSet.add(deployDetails)) {
            validateUploadLimit(deploymentSet.size());
            executor.put(deployDetails);
        }
    }

    /**
     * Checks if the provided file path matches spec's patterns
     * @param filePath to candidate file
     * @param regexpPattern regexp to matched files
     * @param regexpExcludePattern regexp to excluded files
     * @param workspaceDir File object that represents the workspace
     * @param baseDirFile the directory to get files from
     * @return true if the file path matches all terms
     */
    private static boolean isFileMatchPattern(String filePath, Pattern regexpPattern, Pattern regexpExcludePattern,
                                              File workspaceDir, File baseDirFile) {
        File file = new File(filePath);
        String relativePath = UploadSpecHelper.getRelativePath(baseDirFile, file).replace("\\", "/");
        if (!regexpPattern.matcher(relativePath).matches()) {
            return false;
        }

        if (regexpExcludePattern != null) {
            boolean fileInWs = file.getAbsolutePath().startsWith(workspaceDir.getAbsolutePath());
            String relativeToWsPath = fileInWs ? UploadSpecHelper.getRelativeToWsPath(file.getAbsolutePath(),
                    workspaceDir.getAbsolutePath()) : file.getAbsolutePath();
            return !(regexpExcludePattern.matcher(relativeToWsPath).matches());
        }

        return true;
    }

    /**
     * Throws exception if more than 1M files to deploy found
     * @param numberOfFiles the number of artifacts to deploy
     */
    private static void validateUploadLimit(int numberOfFiles) {
        int filesLimit = 1000000;
        if (numberOfFiles >= filesLimit) {
            throw new IllegalStateException("Too many artifacts to deploy were found.");
        }
    }
}
