package org.jfrog.build.extractor.clientConfiguration.util;

import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Vcs;
import org.jfrog.build.api.util.Log;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitUtils {
    private static Pattern CredentialsInUrlRegexpPattern = Pattern.compile("((http|https):\\/\\/\\w.*?:\\w.*?@)");

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
     */
    public static Vcs extractVcs(File workingDir, Log log) throws IOException {
        File dotGit = getDotGit(workingDir);
        if (dotGit == null) {
            throw new FileNotFoundException("Could not find .git directory for extracting Vcs details");
        }
        Vcs vcs = new Vcs();
        vcs.setRevision(extractVcsRevision(dotGit, log));
        vcs.setUrl(extractVcsUrl(dotGit, log));
        return vcs;
    }

    private static String extractVcsUrl(File dotGit, Log log) throws IOException {
        File pathToConfig = new File(dotGit, "config");

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

        String maskedUrl = maskCredentialsInUrl(originalUrl);
        log.debug("Fetched url from git config: " + maskedUrl);
        return maskedUrl;
    }

    static String maskCredentialsInUrl(String originalUrl) throws IOException {
        Matcher matcher = CredentialsInUrlRegexpPattern.matcher(originalUrl);
        if (!matcher.find()) {
            return originalUrl;
        }
        String credentialsPart = matcher.group();
        String[] split = credentialsPart.split("//");
        if (split.length < 2) {
            throw new IOException("Failed to parse .git config");
        }
        return StringUtils.replace(originalUrl, credentialsPart, split[0] + "//***.***@", 1);
    }

    private static String extractVcsRevision(File dotGit, Log log) throws IOException {
        RevisionOrRef revisionOrRef = getRevisionOrBranchPath(dotGit);

        // If found revision, done
        if (StringUtils.isNotEmpty(revisionOrRef.revision)) {
            log.debug("Fetched revision from git config: " + revisionOrRef.revision);
            return revisionOrRef.revision;
        }

        // Else, if found ref try getting revision using it
        File pathToRef = new File(dotGit, revisionOrRef.ref);

        // Read HEAD file for ref or revision
        try (BufferedReader br = new BufferedReader(new FileReader(pathToRef))) {
            String line;
            while ((line = br.readLine()) != null) {
                log.debug("Fetched revision from git config: " + line.trim());
                return line.trim();
            }
        }

        // Could not find revision in ref
        log.warn("Failed fetching revision from git config, from ref: " + revisionOrRef.ref);
        return "";
    }

    private static RevisionOrRef getRevisionOrBranchPath(File dotGit) throws IOException {
        File pathToHead = new File(dotGit, "HEAD");
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

    /**
     * This class is used in the process of extracting Vcs revision from the .git configuration
     */
    private static class RevisionOrRef {
        private String revision;
        private String ref;
    }
}
