package org.jfrog.build.extractor.clientConfiguration.util;

import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.DownloadableArtifact;
import org.jfrog.build.api.dependency.pattern.PatternType;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;

import java.io.IOException;
import java.util.*;

/**
 * Created by Tamirh on 25/04/2016.
 */
public class AqlDependenciesHelper implements DependenciesHelper {

    private DependenciesDownloader downloader;
    private Log log;
    private String artifactoryUrl;
    private String target;
    private String buildName;
    private String buildNumber;

    public AqlDependenciesHelper(DependenciesDownloader downloader, String target, Log log) {
        this.downloader = downloader;
        this.log = log;
        this.artifactoryUrl = downloader.getClient().getArtifactoryUrl();
        this.target = target;
    }

    /**
     * Retrieves dependencies by aql
     */
    @Override
    public List<Dependency> retrievePublishedDependencies(String aql, String[] excludePattern, boolean explode) throws IOException {
        if (StringUtils.isBlank(aql)) {
            return Collections.emptyList();
        }
        Set<DownloadableArtifact> downloadableArtifacts = collectArtifactsToDownload(aql, explode);
        return downloadDependencies(downloadableArtifacts);
    }

    /**
     * Finds and collects artifacts to download by aql
     */
    Set<DownloadableArtifact> collectArtifactsToDownload(String aqlBody, boolean explode) throws IOException {
        String aql = buildQuery(aqlBody);
        List<AqlSearchResult.SearchEntry> searchResults = aqlSearch(aql);
        // If buildName specified, filter the results to keep only artifacts matching the requested build
        if (StringUtils.isNotBlank(buildName) && searchResults.size()>0) {
            searchResults=filterAqlSearchResultsByBuild(searchResults);
        }
        return fetchDownloadableArtifactsFromResult(searchResults,explode);
    }

    /**
     * Retrieves dependencies for file spec of type BUILD (build specified without pattern/aql)
     */
    public List<Dependency> retrievePublishedDependenciesByBuildOnly(boolean explode) throws IOException {
        Set<DownloadableArtifact> downloadableArtifacts = collectArtifactsToDownloadByBuildOnly(explode);
        return downloadDependencies(downloadableArtifacts);
    }

    /**
     * Finds and collects artifacts to download for file spec of type BUILD
     */
    private Set<DownloadableArtifact> collectArtifactsToDownloadByBuildOnly(boolean explode) throws IOException {
        String aql = createAqlBodyForBuild();
        aql = buildQuery(aql);
        List<AqlSearchResult.SearchEntry> searchResults = aqlSearch(aql);
        Map<String, Boolean> buildSha1Map = extractSha1FromAqlResponse(searchResults);
        searchResults = filterBuildAqlSearchResults(searchResults,buildSha1Map);
        return fetchDownloadableArtifactsFromResult(searchResults,explode);
    }

    List<Dependency> downloadDependencies(Set<DownloadableArtifact> downloadableArtifacts) throws IOException {
        log.info("Beginning to resolve Build Info published dependencies.");
        List<Dependency> dependencies = downloader.download(downloadableArtifacts);
        log.info("Finished resolving Build Info published dependencies.");
        return dependencies;
    }

    @Override
    public void setFlatDownload(boolean flat) {
        this.downloader.setFlatDownload(flat);
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
        AqlSearchResult aqlSearchResult = downloader.getClient().searchArtifactsByAql(aql);
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

    /**
     * Converts the found results to DownloadableArtifact types before downloading.
     */
    private Set<DownloadableArtifact> fetchDownloadableArtifactsFromResult(List<AqlSearchResult.SearchEntry> searchResults, boolean explode) {
        Set<DownloadableArtifact> downloadableArtifacts = new HashSet<>();
        for (AqlSearchResult.SearchEntry searchEntry : searchResults) {
            String path = searchEntry.getPath().equals(".") ? "" : searchEntry.getPath() + "/";
            DownloadableArtifact downloadableArtifact = new DownloadableArtifact(StringUtils.stripEnd(artifactoryUrl, "/") + "/" +
                    searchEntry.getRepo(), target, path + searchEntry.getName(), "", "", PatternType.NORMAL);
            downloadableArtifact.setExplode(explode);
            downloadableArtifacts.add(downloadableArtifact);
        }
        return downloadableArtifacts;
    }

    public void setArtifactoryUrl(String artifactoryUrl) {
        this.artifactoryUrl = artifactoryUrl;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getArtifactoryUrl() {
        return artifactoryUrl;
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