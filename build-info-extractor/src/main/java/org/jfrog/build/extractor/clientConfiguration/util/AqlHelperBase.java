package org.jfrog.build.extractor.clientConfiguration.util;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.clientConfiguration.util.spec.FileSpec;

import java.io.IOException;
import java.util.*;

public class AqlHelperBase {

    protected static final String LATEST = "LATEST";
    protected static final String LAST_RELEASE = "LAST_RELEASE";
    protected static final String DELIMITER = "/";
    protected static final String ESCAPE_CHAR = "\\";

    protected ArtifactoryDependenciesClient client;
    private Log log;
    protected String queryBody;
    protected String includeFields;
    protected String querySuffix;
    protected String buildName;
    protected String buildNumber;

    AqlHelperBase(ArtifactoryDependenciesClient client, Log log, FileSpec file) throws IOException {
        this.client = client;
        this.log = log;
        convertFileSpecToAql(file);
    }

    protected void buildQueryAdditionalParts(FileSpec file) throws IOException {
        this.buildName = getBuildName(file.getBuild());
        this.buildNumber = getBuildNumber(client, buildName, file.getBuild());
        this.querySuffix = buildQuerySuffix(file.getSortBy(), file.getSortOrder(), file.getOffset(), file.getLimit());
        this.includeFields = buildIncludeQueryPart(file.getSortBy(), querySuffix);
    }

    protected void convertFileSpecToAql(FileSpec file) throws IOException {
        buildQueryAdditionalParts(file);
        this.queryBody = file.getAql();
    }

    public List<AqlSearchResult.SearchEntry> run() throws IOException {
        String aql = "items.find(" + queryBody + ")" + includeFields + querySuffix;
        log.debug("Searching Artifactory using AQL query:\n" + aql);
        AqlSearchResult aqlSearchResult = client.searchArtifactsByAql(aql);
        List<AqlSearchResult.SearchEntry> queryResults = aqlSearchResult.getResults();

        List<AqlSearchResult.SearchEntry> results = filterResult(queryResults);
        return (results == null ? new ArrayList<>() : results);
    }

    protected static String getBuildName(String build) {
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

    protected String getBuildNumber(ArtifactoryDependenciesClient client, String buildName, String build) throws IOException {
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
            String retrievedBuildNumber = client.getLatestBuildNumberFromArtifactory(buildName, buildNumber);
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

    protected static String buildIncludeQueryPart(String[] sortByFields, String suffix) {
        List<String> fieldsToInclude = getQueryReturnFields(sortByFields);
        if (StringUtils.isBlank(suffix)) {
            fieldsToInclude.add("property");
        }
        return ".include(" + StringUtils.join(prepareFieldsForQuery(fieldsToInclude), ',') + ")";
    }

    private static List<String> getQueryReturnFields(String[] sortByFields) {
        ArrayList<String> includeFields = new ArrayList<String>(
                Arrays.asList("name", "repo", "path", "actual_md5", "actual_sha1", "size", "type"));
        for (String field : sortByFields) {
            if (includeFields.indexOf(field) == -1) {
                includeFields.add(field);
            }
        }
        return includeFields;
    }

    private static List<String> prepareFieldsForQuery(List<String> fields) {
        fields.forEach((field) -> fields.set(fields.indexOf(field), '"' + field + '"'));
        return fields;
    }

    protected static String buildQuerySuffix(String[] sortBy, String sortOrder, String offset, String limit) {
        StringBuilder query = new StringBuilder();
        if (sortBy != ArrayUtils.EMPTY_STRING_ARRAY) {
            sortOrder = StringUtils.defaultIfEmpty(sortOrder, "asc");
            query.append(".sort({\"$").append(sortOrder).append("\":");
            query.append("[").append(prepareSortFieldsForQuery(sortBy)).append("]})");
        }
        if (StringUtils.isNotBlank(offset)) {
            query.append(".offset(").append(offset).append(")");
        }
        if (StringUtils.isNotBlank(limit)) {
            query.append(".limit(").append(limit).append(")");
        }
        return query.toString();
    }

    private static String prepareSortFieldsForQuery(String[] sortByFields) {
        StringBuilder fields = new StringBuilder();
        int size = sortByFields.length;
        for (int i = 0; i < size; i++) {
            fields.append("\"").append(sortByFields[i]).append("\"");
            if (i < size - 1) {
                fields.append(",");
            }
        }
        return fields.toString();
    }

    /**
     * When searching artifacts of a specific build, artifactory uses the checksum list in the the published build-info,
     * and returns all the items that match this list.
     * That kind of search may return duplicated or unnecessary artifacts due to late move/copy/promote operations.
     * Therefore, we need to filter the search results according to the following priorities:
     * 1st priority: Match {Sha1, build name, build number}
     * 2nd priority: Match {Sha1, build name}
     * 3rd priority: Match {Sha1}
     */
    protected static List<AqlSearchResult.SearchEntry> filterAqlSearchResultsByBuild(List<AqlSearchResult.SearchEntry> itemsToFilter, Map<String, Boolean> buildArtifactsSha1, String buildName, String buildNumber) {
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
    protected static Map<String, Boolean> extractSha1FromAqlResponse(List<AqlSearchResult.SearchEntry> searchResults) {
        Map<String, Boolean> resultsMap = new HashMap<>();
        searchResults.forEach((result) -> resultsMap.put(result.getActualSha1(), true));
        return resultsMap;
    }

    protected static String createAqlBodyForBuild(String buildName, String buildNumber) {
        return String.format("{\"artifact.module.build.name\": \"%s\",\"artifact.module.build.number\": \"%s\"}", buildName, buildNumber);
    }

    /**
     * If buildName specified, filter the results to keep only artifacts matching the requested build
     */
    protected List<AqlSearchResult.SearchEntry> filterResult(List<AqlSearchResult.SearchEntry> queryResults) throws IOException {
        if (StringUtils.isNotBlank(buildName) && queryResults.size() > 0) {
            Map<String, Boolean> buildArtifactsSha1 = fetchBuildArtifactsSha1();
            queryResults = filterAqlSearchResultsByBuild(queryResults, buildArtifactsSha1, buildName, buildNumber);
        }

        return queryResults;
    }

    /**
     * Sends an aql query to get all Sha1 value of the requested build, then returns a Map of the Sha1 values.
     */
    private Map<String, Boolean> fetchBuildArtifactsSha1() throws IOException {
        // If a user without admin privileges tries to send AQL query that includes 'actual_sha1' only, a bad request will be return.
        // In order to fix this, we include name, repo & path.
        String includeSha1Field = ".include(\"name\",\"repo\",\"path\",\"actual_sha1\")";
        String buildAql = createAqlQueryForBuild(includeSha1Field);
        log.debug("Searching Artifactory for build's checksums using AQL query:\n" + buildAql);
        AqlSearchResult aqlSearchResult = client.searchArtifactsByAql(buildAql);
        return extractSha1FromAqlResponse(aqlSearchResult.getResults());
    }

    private String createAqlQueryForBuild(String includeQueryPart) {
        return String.format("items.find(%s)%s", createAqlBodyForBuild(buildName, buildNumber), includeQueryPart);
    }

}
