package org.jfrog.build.extractor.clientConfiguration.util;

import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.filespecs.aql.AqlConverter;
import org.jfrog.filespecs.entities.Aql;
import org.jfrog.filespecs.entities.FilesGroup;

import java.io.IOException;
import java.util.*;

public class AqlHelper {

    protected static final String LATEST = "LATEST";
    protected static final String LAST_RELEASE = "LAST_RELEASE";
    protected static final String DELIMITER = "/";
    protected static final String ESCAPE_CHAR = "\\";

    protected Log log;
    protected ArtifactoryManager artifactoryManager;
    protected String buildName;
    protected String buildNumber;
    protected final FilesGroup filesGroup;

    AqlHelper(ArtifactoryManager artifactoryManager, Log log, FilesGroup file) throws IOException {
        this.artifactoryManager = artifactoryManager;
        this.log = log;
        this.filesGroup = file;
        buildQueryAdditionalParts();
    }

    private void buildQueryAdditionalParts() throws IOException {
        this.buildName = getBuildName(this.filesGroup.getBuild());
        // AQL doesn't support projects.
        this.buildNumber = getBuildNumber(artifactoryManager, buildName, this.filesGroup.getBuild(), null);
    }

    public List<AqlSearchResult.SearchEntry> run() throws IOException {
        String aql;

        if (this.filesGroup.getSpecType() == FilesGroup.SpecType.BUILD) {
            // The file-specs-java library doesn't support files groups of type BUILD.
            // To handle that, we create the AQL query body separately here, put it in the files group, use the files group
            // to create a full, valid AQL query and finally revert the files group by removing the AQL from it.
            String queryBody = createAqlBodyForBuild(buildName, buildNumber);
            Aql query = new Aql();
            query.setFind(queryBody);
            this.filesGroup.setAql(query);
            aql = AqlConverter.convertFilesGroupToAql(this.filesGroup);
            this.filesGroup.setAql(null);
        } else {
            aql = AqlConverter.convertFilesGroupToAql(this.filesGroup);
        }

        log.debug("Searching Artifactory using AQL query:\n" + aql);
        AqlSearchResult aqlSearchResult = artifactoryManager.searchArtifactsByAql(aql);
        List<AqlSearchResult.SearchEntry> queryResults = aqlSearchResult.getResults();

        List<AqlSearchResult.SearchEntry> artifactsSha1SearchResults;
        if (this.filesGroup.getSpecType() == FilesGroup.SpecType.BUILD) {
            artifactsSha1SearchResults = queryResults;
        } else {
            artifactsSha1SearchResults = fetchBuildArtifactsSha1();
        }

        List<AqlSearchResult.SearchEntry> results = filterResult(queryResults, artifactsSha1SearchResults);
        return (results == null ? new ArrayList<>() : results);
    }

    /**
     * If buildName specified, filter the results to keep only artifacts matching the requested build
     */
    private List<AqlSearchResult.SearchEntry> filterResult(List<AqlSearchResult.SearchEntry> queryResults, List<AqlSearchResult.SearchEntry> artifactsSha1SerachResults) throws IOException {
        if (StringUtils.isNotBlank(buildName) && queryResults.size() > 0) {
            Map<String, Boolean> buildArtifactsSha1 = extractSha1FromAqlResponse(artifactsSha1SerachResults);
            queryResults = filterAqlSearchResultsByBuild(queryResults, buildArtifactsSha1, buildName, buildNumber);
        }

        return queryResults;
    }

    /**
     * Sends an aql query to get all Sha1 value of the requested build, then returns the search results.
     */
    private List<AqlSearchResult.SearchEntry> fetchBuildArtifactsSha1() throws IOException {
        // If a user without admin privileges tries to send AQL query that includes 'actual_sha1' only, a bad request will be return.
        // In order to fix this, we include name, repo & path.
        String includeSha1Field = ".include(\"name\",\"repo\",\"path\",\"actual_sha1\")";
        String buildAql = String.format("items.find(%s)%s", createAqlBodyForBuild(buildName, buildNumber), includeSha1Field);
        log.debug("Searching Artifactory for build's checksums using AQL query:\n" + buildAql);
        AqlSearchResult aqlSearchResult = artifactoryManager.searchArtifactsByAql(buildAql);
        return aqlSearchResult.getResults();
    }

    private static String createAqlBodyForBuild(String buildName, String buildNumber) {
        return String.format("{\"artifact.module.build.name\": \"%s\",\"artifact.module.build.number\": \"%s\"}", buildName, buildNumber);
    }

    private static String getBuildName(String build) {
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

    private String getBuildNumber(ArtifactoryManager client, String buildName, String build, String project) throws IOException {
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
            String retrievedBuildNumber = client.getLatestBuildNumber(buildName, buildNumber, project);
            if (retrievedBuildNumber == null) {
                logBuildNotFound(buildName, buildNumber);
            }
            buildNumber = retrievedBuildNumber;
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

    /**
     * When searching artifacts of a specific build, artifactory uses the checksum list in the published build-info,
     * and returns all the items that match this list.
     * That kind of search may return duplicated or unnecessary artifacts due to late move/copy/promote operations.
     * Therefore, we need to filter the search results according to the following priorities:
     * 1st priority: Match {Sha1, build name, build number}
     * 2nd priority: Match {Sha1, build name}
     * 3rd priority: Match {Sha1}
     */
    private static List<AqlSearchResult.SearchEntry> filterAqlSearchResultsByBuild(List<AqlSearchResult.SearchEntry> itemsToFilter, Map<String, Boolean> buildArtifactsSha1, String buildName, String buildNumber) {
        // Maps that contain the search results, mapped by the priority they match.
        Map<String, List<AqlSearchResult.SearchEntry>> firstPriority = new HashMap<>();
        Map<String, List<AqlSearchResult.SearchEntry>> secondPriority = new HashMap<>();
        Map<String, List<AqlSearchResult.SearchEntry>> thirdPriority = new HashMap<>();
        // List that contains the filtered results, after step 2.
        List<AqlSearchResult.SearchEntry> filteredResults = new ArrayList<>();

        // Step 1 - Populate 3 priorities mappings.
        for (AqlSearchResult.SearchEntry item : itemsToFilter) {
            if (!buildArtifactsSha1.containsKey(item.getActualSha1())) {
                continue;
            }

            boolean isBuildNameMatch = buildName.equals(item.getBuildName());
            boolean isBuildNumberMatch = buildNumber.equals(item.getBuildNumber());
            if (isBuildNameMatch) {
                if (isBuildNumberMatch) {
                    addToListInMap(firstPriority, item);
                    continue;
                }
                addToListInMap(secondPriority, item);
                continue;
            }
            addToListInMap(thirdPriority, item);
        }

        // Step 2 - Append mappings to the final results, respectively.
        for (Map.Entry<String, Boolean> entry : buildArtifactsSha1.entrySet()) {
            String shaToMatch = entry.getKey();
            if (firstPriority.containsKey(shaToMatch)) {
                filteredResults.addAll(firstPriority.get(shaToMatch));
            } else if (secondPriority.containsKey(shaToMatch)) {
                filteredResults.addAll(secondPriority.get(shaToMatch));
            } else if (thirdPriority.containsKey(shaToMatch)) {
                filteredResults.addAll(thirdPriority.get(shaToMatch));
            }
        }
        return filteredResults;
    }

    private static void addToListInMap(Map<String, List<AqlSearchResult.SearchEntry>> map, AqlSearchResult.SearchEntry item) {
        List<AqlSearchResult.SearchEntry> curList = map.get(item.getActualSha1());
        if (curList == null) curList = new ArrayList<>();
        curList.add(item);
        map.put(item.getActualSha1(), curList);
    }

    /**
     * Maps all Sha1 values that exist in the results found
     */
    private static Map<String, Boolean> extractSha1FromAqlResponse(List<AqlSearchResult.SearchEntry> searchResults) {
        Map<String, Boolean> resultsMap = new HashMap<>();
        searchResults.forEach((result) -> resultsMap.put(result.getActualSha1(), true));
        return resultsMap;
    }
}
