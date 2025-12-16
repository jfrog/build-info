package org.jfrog.build.extractor.clientConfiguration.util;

import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.UrlUtils;
import org.jfrog.build.extractor.ci.Vcs;
import org.jfrog.build.extractor.executor.CommandExecutor;
import org.jfrog.build.extractor.executor.CommandResults;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for extracting VCS (Git) information from the .git directory.
 * File paths are validated to prevent directory traversal attacks.
 */
public class GitUtils {
    private static final String GIT_LOG_LIMIT = "100";

    private static File getDotGit(File file) {
        if (file == null) {
            return null;
        }
        File dotGit = new File(file, ".git");
        if (dotGit.exists()) {
            return dotGit;
        }
        return getDotGit(file.getParentFile());
    }

    /**
     * Extract Vcs details from .git configuration.
     *
     * @return Vcs with the details found, empty Vcs if failed to find .git directory
     */
    public static Vcs extractVcs(File workingDir, Log log) throws IOException {
        log.debug("Extracting Vcs details from the .git directory.");
        File dotGit = getDotGit(workingDir);
        if (dotGit == null) {
            log.debug("Could not find the .git directory.");
            return new Vcs();
        }

        if (dotGit.isFile()) {
            // dotGit is a file and not a directory, assume submodule.
            dotGit = getSubmoduleDotGit(dotGit);
        }

        Vcs vcs = new Vcs();
        vcs.setRevision(extractVcsRevision(dotGit, log));
        vcs.setUrl(extractVcsUrl(dotGit, log));
        vcs.setMessage(extractVcsMessage(dotGit, log));
        vcs.setBranch(extractVcsBranch(dotGit, log));
        return vcs;
    }

    /**
     * A submodule's .git is a file, referencing the actual path of the .git directory of the submodule.
     * The actual .git directory is under the parent project's .git/modules directory.
     *
     * @param dotGit The .git file of the submodule
     * @return File representing the actual .git directory of the submodule
     * @throws IOException If fails to find the submodule's .gir directory
     */
    private static File getSubmoduleDotGit(File dotGit) throws IOException {
        String dotGitRelativePathString = extractSubmoduleDotGitPath(dotGit);
        String dotGitAbsolutePathString = dotGit.getParent() + File.separator + dotGitRelativePathString;
        File dotGitFile = new File(dotGitAbsolutePathString);
        if (!dotGitFile.exists()) {
            throw new IOException("Could not find the .git directory of a submodule.");
        }
        return dotGitFile;
    }

    private static String extractSubmoduleDotGitPath(File dotGit) throws IOException {
        // Read .git file - the first row is the path to the actual submodule's .git.
        try (BufferedReader br = new BufferedReader(new FileReader(dotGit))) {
            String line = br.readLine();
            if (line != null && line.startsWith("gitdir: ")) {
                return line.substring(line.indexOf(":") + 1).trim();
            }
        }
        throw new IOException("Failed to parse .git path for submodule.");
    }

    private static String extractVcsUrl(File dotGit, Log log) throws IOException {
        // Validate child path to prevent directory traversal
        File pathToConfig = PathSanitizer.validateChildPath(dotGit, "config");

        String originalUrl = "";
        // Fetch url from config file
        try (BufferedReader br = new BufferedReader(new FileReader(pathToConfig))) {
            String line;
            boolean isNextLineUrl = false;
            while ((line = br.readLine()) != null) {
                if (isNextLineUrl) {
                    line = line.trim();
                    if (line.startsWith("url")) {
                        String[] split = line.split("=");
                        if (split.length < 2) {
                            throw new IOException("Failed to parse .git config");
                        }
                        originalUrl = split[1].trim();
                        break;
                    }
                }
                if (line.equals("[remote \"origin\"]")) {
                    isNextLineUrl = true;
                }
            }
        }
        if (!originalUrl.endsWith(".git")) {
            originalUrl += ".git";
        }

        String maskedUrl = UrlUtils.removeCredentialsFromUrl(originalUrl);
        log.debug("Fetched url from git config: " + maskedUrl);
        return maskedUrl;
    }

    private static String extractVcsRevision(File dotGit, Log log) throws IOException {
        RevisionOrRef revisionOrRef = getRevisionOrBranchPath(dotGit);

        // If found revision, done
        if (StringUtils.isNotEmpty(revisionOrRef.revision)) {
            log.debug("Fetched revision from git config: " + revisionOrRef.revision);
            return revisionOrRef.revision;
        }

        // Else, if found ref try getting revision using it
        // Validate child path to prevent directory traversal
        File pathToRef = PathSanitizer.validateChildPath(dotGit, revisionOrRef.ref);
        if (pathToRef.exists()) {
            // Read HEAD file for ref or revision
            try (BufferedReader br = new BufferedReader(new FileReader(pathToRef))) {
                String line;
                while ((line = br.readLine()) != null) {
                    log.debug("Fetched revision from git config: " + line.trim());
                    return line.trim();
                }
            }
        } else {
            // Try to find .git/packed-refs and look for the HEAD there
            // Validate child path to prevent directory traversal
            File pathToPackedRefs = PathSanitizer.validateChildPath(dotGit, "packed-refs");
            if (pathToPackedRefs.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(pathToPackedRefs))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.endsWith(revisionOrRef.ref)) {
                            String[] split = line.split("\\s+");
                            return split[0];
                        }
                    }
                }
            }
        }

        // Could not find revision in ref
        log.warn("Failed fetching revision from git config, from ref: " + revisionOrRef.ref);
        return "";
    }

    private static RevisionOrRef getRevisionOrBranchPath(File dotGit) throws IOException {
        // Validate child path to prevent directory traversal
        File pathToHead = PathSanitizer.validateChildPath(dotGit, "HEAD");
        RevisionOrRef result = new RevisionOrRef();

        // Read HEAD file for ref or revision
        try (BufferedReader br = new BufferedReader(new FileReader(pathToHead))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("ref")) {
                    String[] split = line.split(":");
                    if (split.length < 2) {
                        throw new IOException("Failed to parse .git config");
                    }
                    result.ref = split[1].trim();
                    return result;
                }
                result.revision = line;
            }
        }

        return result;
    }

    private static String extractVcsBranch(File dotGit, Log log) throws IOException {
        RevisionOrRef revisionOrRef = getRevisionOrBranchPath(dotGit);
        // If found ref, returns the name after the last "/" as the git branch name.
        if (StringUtils.isNotBlank(revisionOrRef.ref)) {
            String[] splitArr = revisionOrRef.ref.split("/");
            return splitArr[splitArr.length - 1];
        }
        log.debug("Failed fetching git branch from git directory: " + dotGit + ". Might be in detached head.");
        return "";
    }

    private static String extractVcsMessage(File dotGit, Log log) {
        String warnMessage = "Failed fetching commit message from git directory: " + dotGit + "\nWith the following error: ";
        try {
            CommandResults res = getGitLog(dotGit, log, 1);
            if (res.isOk()) {
                return res.getRes().trim();
            }
            log.warn(warnMessage + res.getErr());
        } catch (InterruptedException | IOException e) {
            log.warn(warnMessage + e.getMessage());
        }
        return "";
    }

    /**
     * This class is used in the process of extracting Vcs revision from the .git configuration
     */
    private static class RevisionOrRef {
        private String revision;
        private String ref;
    }

    public static CommandResults getGitLog(File execDir, Log logger, String previousVcsRevision) throws InterruptedException, IOException {
        return getGitLog(execDir, logger, previousVcsRevision, 0);
    }

    public static CommandResults getGitLog(File execDir, Log logger, int limit) throws InterruptedException, IOException {
        return getGitLog(execDir, logger, "", limit);
    }

    public static CommandResults getGitLog(File execDir, Log logger, String previousVcsRevision, int limit) throws InterruptedException, IOException {
        List<String> args = new ArrayList<>();
        args.add("log");
        args.add("--pretty=format:%s");
        args.add("-" + (limit == 0 ? GIT_LOG_LIMIT : limit));
        if (!previousVcsRevision.isEmpty()) {
            args.add(previousVcsRevision + "..");
        }
        CommandExecutor commandExecutor = new CommandExecutor("git", null);
        return commandExecutor.exeCommand(execDir, args, null, logger);
    }
}
