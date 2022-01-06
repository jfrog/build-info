package org.jfrog.build.extractor.issuesCollection;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.extractor.ci.Issue;
import org.jfrog.build.extractor.ci.IssueTracker;
import org.jfrog.build.extractor.ci.Issues;
import org.jfrog.build.extractor.ci.IssuesCollectionConfig;
import org.jfrog.build.extractor.ci.Vcs;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.clientConfiguration.util.GitUtils;
import org.jfrog.build.extractor.executor.CommandResults;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jfrog.build.api.IssuesCollectionConfig.ISSUES_COLLECTION_ERROR_PREFIX;

/**
 * This class handles the issues collection.
 * Issues are collected from the log, following the configuration that is passed as a Json file.
 */
public class IssuesCollector implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String LATEST = "LATEST";

    public static Pattern REVISION_NOT_EXIST;

    public IssuesCollector() {
    }

    public static Pattern getRevisionNotExistPattern() {
        if (REVISION_NOT_EXIST == null) {
            REVISION_NOT_EXIST = Pattern.compile("fatal: Invalid revision range [a-fA-F0-9]+\\.\\.");
        }
        return REVISION_NOT_EXIST;
    }

    /**
     * Main function that manages the issue collection process.
     */
    public Issues collectIssues(File execDir, Log logger, String config, ArtifactoryManagerBuilder artifactoryManagerBuilder,
                                String buildName, Vcs vcs, String project) throws InterruptedException, IOException {
        IssuesCollectionConfig parsedConfig = parseConfig(config);
        String previousVcsRevision = getPreviousVcsRevision(artifactoryManagerBuilder, buildName, vcs, project);
        Set<Issue> affectedIssues = doCollect(execDir, logger, parsedConfig, previousVcsRevision);
        return buildIssuesObject(parsedConfig, affectedIssues);
    }

    IssuesCollectionConfig parseConfig(String config) throws IOException {
        // When mapping the config from String to IssuesCollectionConfig one backslash is being removed, multiplying the backslashes solves this.
        config = config.replace("\\", "\\\\");
        ObjectMapper mapper = new ObjectMapper(new JsonFactory());
        IssuesCollectionConfig parsedConfig;
        try {
            parsedConfig = mapper.readValue(config, IssuesCollectionConfig.class);
        } catch (Exception e) {
            throw new IOException(ISSUES_COLLECTION_ERROR_PREFIX + "Failed parsing config: " + e.getMessage());
        }

        parsedConfig.validateConfig();
        return parsedConfig;
    }

    /**
     * Gets the previous vcs revision from the LATEST build published to Artifactory.
     */
    private String getPreviousVcsRevision(ArtifactoryManagerBuilder artifactoryManagerBuilder, String prevBuildName, Vcs prevVcs, String project) throws IOException {
        try (ArtifactoryManager artifactoryManager = artifactoryManagerBuilder.build()) {
            // Get LATEST build info from Artifactory
            BuildInfo previousBuildInfo = artifactoryManager.getBuildInfo(prevBuildName, LATEST, project);
            if (previousBuildInfo == null) {
                return "";
            }
            // Gets the first revision related to the current git repository.
            List<Vcs> vcsList = previousBuildInfo.getVcs();
            if (vcsList != null && vcsList.size() > 0) {
                for (Vcs curVcs : previousBuildInfo.getVcs()) {
                    if (StringUtils.isNotEmpty(curVcs.getRevision()) && StringUtils.equals(curVcs.getUrl(), prevVcs.getUrl())) {
                        return curVcs.getRevision();
                    }
                }
            }
            return "";
        }
    }

    /**
     * Collects affected issues from git log
     */
    private Set<Issue> doCollect(File execDir, Log logger, IssuesCollectionConfig issuesConfig, String previousVcsRevision) throws InterruptedException, IOException {
        String gitLog = getGitLog(execDir, logger, previousVcsRevision);

        int keyIndex = issuesConfig.getIssues().getKeyGroupIndex();
        int summaryIndex = issuesConfig.getIssues().getSummaryGroupIndex();
        Set<Issue> affectedIssues = new HashSet<>();

        Pattern pattern = Pattern.compile(issuesConfig.getIssues().getRegexp());
        String[] lines = gitLog.split("\\R");
        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                Issue foundIssue = getMatchingIssue(keyIndex, summaryIndex, matcher, issuesConfig);
                affectedIssues.add(foundIssue);
                logger.info("Added issue: " + foundIssue.getKey() + " to the build-info");
            }
        }
        return affectedIssues;
    }

    private Issues buildIssuesObject(IssuesCollectionConfig parsedConfig, Set<Issue> affectedIssues) {
        IssueTracker tracker = new IssueTracker(parsedConfig.getIssues().getTrackerName());
        boolean aggregateBuildIssues = parsedConfig.getIssues().isAggregate();
        String aggregationBuildStatus = parsedConfig.getIssues().getAggregationStatus();
        return new Issues(tracker, aggregateBuildIssues, aggregationBuildStatus, affectedIssues);
    }

    private Issue getMatchingIssue(int keyIndex, int summaryIndex, Matcher matcher, IssuesCollectionConfig issuesConfig) throws IOException {
        // Check for out of bound results.
        if (matcher.groupCount() < keyIndex || matcher.groupCount() < summaryIndex) {
            throw new IOException(ISSUES_COLLECTION_ERROR_PREFIX + "Unexpected result while parsing issues from git log. Make sure that the regular expression used to find issues, includes two capturing groups, for the issue ID and the summary.");
        }

        // Create found Affected Issue.
        String key = matcher.group(keyIndex);
        String summary = matcher.group(summaryIndex);
        String url = "";
        if (StringUtils.isNotEmpty(issuesConfig.getIssues().getTrackerUrl())) {
            url = issuesConfig.getIssues().getTrackerUrl() + "/" + key;
        }
        return new Issue(key, url, summary);
    }

    private String getGitLog(File execDir, Log logger, String previousVcsRevision) throws InterruptedException, IOException {
        CommandResults res = GitUtils.getGitLog(execDir, logger, previousVcsRevision);
        if (!res.isOk()) {
            if (getRevisionNotExistPattern().matcher(res.getErr()).find()) {
                logger.info("Revision: " + previousVcsRevision + " that was fetched from latest build info does not exist in the git revision range. No new issues are added.");
                return "";
            }
            throw new IOException(ISSUES_COLLECTION_ERROR_PREFIX + "Git log command failed: " + res.getErr());
        }
        return res.getRes();
    }
}
