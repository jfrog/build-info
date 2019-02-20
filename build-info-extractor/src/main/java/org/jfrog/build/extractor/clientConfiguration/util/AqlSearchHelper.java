package org.jfrog.build.extractor.clientConfiguration.util;

import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;

import java.io.IOException;
import java.util.*;

/**
 * Created by Tamirh on 25/04/2016.
 */
public class AqlSearchHelper {
    private ArtifactoryDependenciesClient client;
    private String buildName;
    private String buildNumber;

    AqlSearchHelper(ArtifactoryDependenciesClient client) {
        this.client = client;
    }

    /**
     * Finds and collects artifacts by aql
     */
    List<AqlSearchResult.SearchEntry> collectArtifactsByAql(String aqlBody) throws IOException {
        if (StringUtils.isBlank(aqlBody)) {
            return Collections.emptyList();
        }

        String aql = buildQuery(aqlBody);
        List<AqlSearchResult.SearchEntry> searchResults = aqlSearch(aql);
        // If buildName specified, filter the results to keep only artifacts matching the requested build
        if (StringUtils.isNotBlank(buildName) && searchResults.size()>0) {
            searchResults=filterAqlSearchResultsByBuild(searchResults);
        }
        return searchResults;
    }

    /**
     * Finds and collects artifacts by build only
     */
    List<AqlSearchResult.SearchEntry> collectArtifactsByBuild() throws IOException {
        String aql = createAqlBodyForBuild();
        aql = buildQuery(aql);
        List<AqlSearchResult.SearchEntry> searchResults = aqlSearch(aql);
        Map<String, Boolean> buildSha1Map = extractSha1FromAqlResponse(searchResults);
        return filterBuildAqlSearchResults(searchResults,buildSha1Map);
    }

    /**
     * Filters the found results to keep only artifacts matching the requested build
     */
    private List<AqlSearchResult.SearchEntry> filterAqlSearchResultsByBuild(List<AqlSearchResult.SearchEntry> searchResults) throws IOException {
        Map<String,Boolean> buildArtifactsSha1 = fetchBuildArtifactsSha1();
        return filterBuildAqlSearchResults(searchResults,buildArtifactsSha1);
    }

    /**
     * Filter search results by the following priorities:
     * 1st priority: Match {Sha1, build name, build number}
     * 2nd priority: Match {Sha1, build name}
     * 3rd priority: Match {Sha1}
     */
    private List<AqlSearchResult.SearchEntry> filterBuildAqlSearchResults(List<AqlSearchResult.SearchEntry> itemsToFilter, Map<String,Boolean> buildArtifactsSha1){
        // Maps that contain the search results, mapped by the priority they match.
        Map<String, List<AqlSearchResult.SearchEntry>> firstPriority = new HashMap<>();
        Map<String, List<AqlSearchResult.SearchEntry>> secondPriority = new HashMap<>();
        Map<String, List<AqlSearchResult.SearchEntry>> thirdPriority = new HashMap<>();
        // List that contains the filtered results, after step 2.
        List<AqlSearchResult.SearchEntry> filteredResults = new ArrayList<>();

        // Step 1 - Populate 3 priorities mappings.
        for (AqlSearchResult.SearchEntry item : itemsToFilter) {
            if (!buildArtifactsSha1.containsKey(item.getActualSha1())){
                continue;
            }

            boolean isBuildNameMatch = item.getBuildName().equals(this.buildName);
            boolean isBuildNumberMatch = item.getBuildNumber().equals(this.buildNumber);
            if (isBuildNameMatch) {
                if (isBuildNumberMatch) {
                    addToListInMap(firstPriority,item);
                    continue;
                }
                addToListInMap(secondPriority,item);
                continue;
            }
            addToListInMap(thirdPriority,item);
        }

        // Step 2 - Append mappings to the final results, respectively.
        for (Map.Entry<String, Boolean> entry : buildArtifactsSha1.entrySet()) {
            String shaToMatch = entry.getKey();
            if (firstPriority.containsKey(shaToMatch)) {
                filteredResults.addAll(firstPriority.get(shaToMatch));
            } else if (secondPriority.containsKey(shaToMatch)) {
                filteredResults.addAll(secondPriority.get(shaToMatch));
            }  else if (thirdPriority.containsKey(shaToMatch)) {
                filteredResults.addAll(thirdPriority.get(shaToMatch));
            }
        }
        return filteredResults;
    }

    private void addToListInMap(Map<String, List<AqlSearchResult.SearchEntry>> map, AqlSearchResult.SearchEntry item) {
        List<AqlSearchResult.SearchEntry> curList=map.get(item.getActualSha1());
        if (curList==null) curList = new ArrayList<>();
        curList.add(item);
        map.put(item.getActualSha1(),curList);
    }

    /**
     * Maps all Sha1 values that exist in the results found
     */
    private Map<String, Boolean> extractSha1FromAqlResponse(List<AqlSearchResult.SearchEntry> searchResults) {
        Map<String, Boolean> resultsMap = new HashMap<>();
        searchResults.forEach((result)-> resultsMap.put(result.getActualSha1(),true));
        return resultsMap;
    }

    /**
     * Sends an aql query to get all Sha1 value of the requested build, then returns a Map of the Sha1 values.
     */
    private Map<String, Boolean> fetchBuildArtifactsSha1() throws  IOException {
        String buildQuery = createAqlQueryForBuild(buildIncludeQueryPart(Arrays.asList("name", "repo", "path", "actual_sha1")));
        return extractSha1FromAqlResponse(aqlSearch(buildQuery));
    }

    private String createAqlQueryForBuild(String includeQueryPart) {
        return String.format("items.find(%s)%s",createAqlBodyForBuild(),includeQueryPart);
    }

    private String createAqlBodyForBuild() {
        return String.format("{\"artifact.module.build.name\": \"%s\",\"artifact.module.build.number\": \"%s\"}",buildName,buildNumber);
    }

    private List<AqlSearchResult.SearchEntry> aqlSearch(String aql) throws IOException {
        AqlSearchResult aqlSearchResult = client.searchArtifactsByAql(aql);
        return aqlSearchResult.getResults();
    }

    private String buildQuery(String aql){
        aql = "items.find(" + aql + ")";
        aql += buildIncludeQueryPart(getQueryReturnFields());
        return aql;
    }

    private String buildIncludeQueryPart(List<String> fieldsToInclude) {
        return ".include(" + StringUtils.join(prepareFieldsForQuery(fieldsToInclude),',') + ")";
    }

    private List<String> prepareFieldsForQuery(List<String> fields) {
        fields.forEach( (field) -> fields.set(fields.indexOf(field),'"' + field + '"'));
        return fields;
    }

    private List<String> getQueryReturnFields() {
        return Arrays.asList("name", "repo", "path", "actual_md5", "actual_sha1", "size", "type", "property");
    }

    public String getBuildName() {
        return buildName;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }
}