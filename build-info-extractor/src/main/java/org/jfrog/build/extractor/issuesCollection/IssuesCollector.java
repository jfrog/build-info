package org.jfrog.build.extractor.issuesCollection;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.*;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.executor.CommandExecutor;
import org.jfrog.build.extractor.executor.CommandResults;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jfrog.build.api.IssuesCollectionConfig.ISSUES_COLLECTION_ERROR_PREFIX;

public class IssuesCollector {
    private static final String LATEST = "LATEST";
    private static final String GIT_LOG_LIMIT = "100";

    private CommandExecutor commandExecutor;

    public IssuesCollector() {
        this.commandExecutor = new CommandExecutor("git", null);
    }

    public IssuesCollectionConfig parseConfig(String config) throws IOException {
        // When mapping the config from String to IssuesCollectionConfig one backslash is being removed, multiplying the backslashes solves this.
        config = config.replace("\\", "\\\\");
        ObjectMapper mapper = new ObjectMapper(new JsonFactory());
        IssuesCollectionConfig parsedConfig = mapper.readValue(config, IssuesCollectionConfig.class);
        parsedConfig.validateConfig();
        return parsedConfig;
    }

    public String getPreviousVcsRevision(ArtifactoryBuildInfoClient client, String buildName) throws IOException {
        Build previousBuildInfo = client.getBuildInfo(buildName, LATEST);
        if (previousBuildInfo == null) {
            return "";
        }
        if (StringUtils.isNotEmpty(previousBuildInfo.getVcsRevision())) {
            return previousBuildInfo.getVcsRevision();
        }
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

    public Issues doCollect(File dotGitPath, Log logger, IssuesCollectionConfig issuesConfig, String previousVcsRevision) throws InterruptedException, IOException {
        Set<Issue> affectedIssues = collectAffectedIssues(dotGitPath, logger, issuesConfig, previousVcsRevision);
        IssueTracker tracker = new IssueTracker(issuesConfig.getIssues().getTrackerName());
        boolean aggregateBuildIssues = issuesConfig.getIssues().isAggregate();
        String aggregationBuildStatus = issuesConfig.getIssues().getAggregationStatus();
        return new Issues(tracker, aggregateBuildIssues, aggregationBuildStatus, affectedIssues);
    }

    /**
     * Collect affected issues from git log
     */
    private Set<Issue> collectAffectedIssues(File dotGitPath, Log logger, IssuesCollectionConfig issuesConfig, String previousVcsRevision) throws InterruptedException, IOException {
        verifyGitExists(dotGitPath, logger);
        String gitLog = getGitLog(dotGitPath, logger, previousVcsRevision);

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
                logger.debug("Found issue: " + foundIssue.getKey());
            }
        }
        return affectedIssues;
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

    private void verifyGitExists(File dotGitPath, Log logger) throws InterruptedException, IOException {
        List<String> args = new ArrayList<>();
        args.add("help");
        CommandResults res = this.commandExecutor.exeCommand(dotGitPath, args, logger);
        if (!res.isOk()) {
            throw new IOException(ISSUES_COLLECTION_ERROR_PREFIX + "Git executable not found in path");
        }
    }

    private String getGitLog(File dotGitPath, Log logger, String previousVcsRevision) throws InterruptedException, IOException {
        List<String> args = new ArrayList<>();
        args.add("log");
        args.add("--pretty=format:%s");
        args.add("-" + GIT_LOG_LIMIT);
        if (!previousVcsRevision.isEmpty()) {
            args.add(previousVcsRevision + "..");
        }
        CommandResults res = this.commandExecutor.exeCommand(dotGitPath, args, logger);
        if (!res.isOk()) {
            throw new IOException(res.getErr());
        }
        return res.getRes();
    }
}
