package org.jfrog.build.extractor.clientConfiguration.util;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.dependency.BuildPatternArtifacts;
import org.jfrog.build.api.dependency.BuildPatternArtifactsRequest;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.clientConfiguration.util.spec.FileSpec;
import org.jfrog.build.extractor.clientConfiguration.util.spec.SpecsHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class ArtifactorySearcher {
    private ArtifactoryDependenciesClient client;
    private Log log;
    private final String LATEST = "LATEST";
    private final String LAST_RELEASE = "LAST_RELEASE";
    private static final String DELIMITER = "/";
    private static final String ESCAPE_CHAR = "\\";

    ArtifactorySearcher(ArtifactoryDependenciesClient client, Log log) {
        this.client = client;
        this.log = log;
    }

    List<AqlSearchResult.SearchEntry> SearchByFileSpec(FileSpec file) throws IOException {
        AqlSearchHelper aqlHelper = new AqlSearchHelper(client);
        WildcardsSearchHelper wildcardHelper = new WildcardsSearchHelper(client);
        List<AqlSearchResult.SearchEntry> results = new ArrayList<>();
        log.info("Searching for artifacts...");
        switch(file.getSpecType()) {
            case PATTERN: {
                setWildcardHelperProperties(wildcardHelper,file);
                results = wildcardHelper.collectArtifactsByPattern(file.getPattern(), file.getExcludePatterns());
                break;
            }
            case BUILD: {
                setAqlHelperProperties(aqlHelper,file);
                results = aqlHelper.collectArtifactsByBuild();
                break;
            }
            case AQL: {
                setAqlHelperProperties(aqlHelper,file);
                results = aqlHelper.collectArtifactsByAql(file.getAql());
                break;
            }
        }
        log.info(String.format("Found %s artifacts.", results.size()));
        return results;
    }

    private void setWildcardHelperProperties(WildcardsSearchHelper wildcardHelper, FileSpec file) throws IOException {
        wildcardHelper.setRecursive(!"false".equalsIgnoreCase(file.getRecursive()));
        wildcardHelper.setProps(file.getProps());
        String buildName = getBuildName(file.getBuild());
        wildcardHelper.setBuildName(buildName);
        wildcardHelper.setBuildNumber(getBuildNumber(buildName, file.getBuild()));
    }

    private void setAqlHelperProperties(AqlSearchHelper aqlHelper, FileSpec file) throws IOException{
        String buildName = getBuildName(file.getBuild());
        aqlHelper.setBuildName(buildName);
        aqlHelper.setBuildNumber(getBuildNumber(buildName, file.getBuild()));
    }

    private String getBuildName(String build) {
        if (StringUtils.isBlank(build)) {
            return build;
        }
        // The delimiter must not be prefixed with escapeChar (if it is, it should be part of the build number)
        // the code below gets substring from before the last delimiter.
        // If the new string ends with escape char it means the last delimiter was part of the build number and we need
        // to go back to the previous delimiter.
        // If no proper delimiter was found the full string will be the build name.
        String buildName = StringUtils.substringBeforeLast(build, DELIMITER);
        while (StringUtils.isNotBlank(buildName) && buildName.contains(DELIMITER) && buildName.endsWith(ESCAPE_CHAR)) {
            buildName = StringUtils.substringBeforeLast(buildName, DELIMITER);
        }
        return buildName.endsWith(ESCAPE_CHAR) ? build : buildName;
    }

    private String getBuildNumber(String buildName, String build) throws IOException {
        String buildNumber = "";
        if (StringUtils.isNotBlank(buildName)) {
            if (!build.startsWith(buildName)) {
                throw new IllegalStateException(String.format("build '%s' does not start with build name '%s'.", build, buildName));
            }
            // Case build number was not provided, the build name and the build are the same. build number will be latest
            if (build.equals(buildName)) {
                buildNumber = LATEST;
            } else {
                // Get build name by removing build name and the delimiter
                buildNumber = build.substring(buildName.length() + DELIMITER.length());
                // Remove the escape chars before the delimiters
                buildNumber = buildNumber.replace(ESCAPE_CHAR + DELIMITER, DELIMITER);
            }
            if (LATEST.equals(buildNumber.trim()) || LAST_RELEASE.equals(buildNumber.trim())) {
                if (this.client.isArtifactoryOSS()) {
                    throw new IllegalArgumentException(String.format("%s is not supported in Artifactory OSS.", buildNumber));
                }
                List<BuildPatternArtifactsRequest> artifactsRequest = Lists.newArrayList();
                artifactsRequest.add(new BuildPatternArtifactsRequest(buildName, buildNumber));
                List<BuildPatternArtifacts> artifactsResponses =  this.client.retrievePatternArtifacts(artifactsRequest);
                // Artifactory returns null if no build was found
                if (artifactsResponses.get(0) != null) {
                    buildNumber = artifactsResponses.get(0).getBuildNumber();
                } else {
                    logBuildNotFound(buildName, buildNumber);
                    return null;
                }
            }
        }
        return buildNumber;
    }

    private void logBuildNotFound(String buildName, String buildNumber) {
        StringBuilder sb = new StringBuilder("The build name ").append(buildName);
        if (LAST_RELEASE.equals(buildNumber.trim())) {
            sb.append(" with the status RELEASED");
        }
        sb.append(" could not be found.");
        log.warn(sb.toString());
    }
}
