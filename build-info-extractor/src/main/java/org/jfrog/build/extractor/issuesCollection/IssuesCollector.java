package org.jfrog.build.extractor.issuesCollection;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.*;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.executor.CommandExecutor;
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
    private static final String GIT_LOG_LIMIT = "100";

    private CommandExecutor commandExecutor;

    public IssuesCollector() {
        this.commandExecutor = new CommandExecutor("git", null);
    }

    /**
     * Main function that manages the issue collection process.
     */
    public Issues collectIssues(File execDir, Log logger, String config, ArtifactoryBuildInfoClientBuilder clientBuilder,
                                String buildName) throws InterruptedException, IOException {
        IssuesCollectionConfig parsedConfig = parseConfig(config);
        String previousVcsRevision = getPreviousVcsRevision(clientBuilder, buildName);
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
    private String getPreviousVcsRevision(ArtifactoryBuildInfoClientBuilder clientBuilder, String buildName) throws IOException {
        ArtifactoryBuildInfoClient client = clientBuilder.build();
        // Get LATEST build info from Artifactory
        Build previousBuildInfo = client.getBuildInfo(buildName, LATEST);
        if (previousBuildInfo == null) {
            return "";
        }
        if (StringUtils.isNotEmpty(previousBuildInfo.getVcsRevision())) {
            return previousBuildInfo.getVcsRevision();
        }
        // If revision is not listed explicitly, get revision from the first not empty Vcs of the Vcs list.
        List<Vcs> vcsList = previousBuildInfo.getVcs();
        if (vcsList != null && vcsList.size() > 0) {
            for (Vcs curVcs : previousBuildInfo.getVcs()) {
                if (StringUtils.isNotEmpty(curVcs.getRevision())) {
                    return curVcs.getRevision();
                }
            }
        }
        return "";
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
        List<String> args = new ArrayList<>();
        args.add("log");
        args.add("--pretty=format:%s");
        args.add("-" + GIT_LOG_LIMIT);
        if (!previousVcsRevision.isEmpty()) {
            args.add(previousVcsRevision + "..");
        }
        CommandResults res = this.commandExecutor.exeCommand(execDir, args, logger);
        if (!res.isOk()) {
            throw new IOException(ISSUES_COLLECTION_ERROR_PREFIX + "Git log command failed: " + res.getErr());
        }
        return res.getRes();
    }
}
