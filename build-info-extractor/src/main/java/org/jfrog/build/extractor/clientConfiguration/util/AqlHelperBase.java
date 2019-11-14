package org.jfrog.build.extractor.clientConfiguration.util;

import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.clientConfiguration.util.spec.FileSpec;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class AqlHelperBase {
    protected ArtifactoryDependenciesClient client;
    private Log log;
    protected String queryBody;
    protected String includeFields;
    protected String querySuffix;
    protected String buildName;
    protected String buildNumber;

    AqlHelperBase(ArtifactoryDependenciesClient client, Log log) {
        this.client = client;
        this.log = log;
    }

    protected void buildQueryAdditionalParts(FileSpec file) throws IOException {
        this.buildName = AqlUtils.getBuildName(file.getBuild());
        this.buildNumber = AqlUtils.getBuildNumber(client, buildName, file.getBuild(), log);
        this.querySuffix = AqlUtils.buildQuerySuffix(file.getSortBy(), file.getSortOrder(), file.getOffset(), file.getLimit());
        this.includeFields = AqlUtils.buildIncludeQueryPart(file.getSortBy(), querySuffix);

    }

    public void convertFileSpecToAql(FileSpec file) throws IOException {
        buildQueryAdditionalParts(file);
        this.queryBody = file.getAql();

    }

    public List<AqlSearchResult.SearchEntry> run() throws IOException {
        String aql = "items.find(" + queryBody + ")" + includeFields + querySuffix;
        log.debug("Searching Artifactory using AQL query:\n" + aql);
        AqlSearchResult aqlSearchResult = client.searchArtifactsByAql(aql);
        List<AqlSearchResult.SearchEntry> queryResults = aqlSearchResult.getResults();

        return filterResult(queryResults);
    }

    // If buildName specified, filter the results to keep only artifacts matching the requested build
    protected List<AqlSearchResult.SearchEntry> filterResult(List<AqlSearchResult.SearchEntry> queryResults) throws IOException {
        if (StringUtils.isNotBlank(buildName) && queryResults.size() > 0) {
            Map<String, Boolean> buildArtifactsSha1 = fetchBuildArtifactsSha1();
            queryResults = AqlUtils.filterAqlSearchResultsByBuild(queryResults, buildArtifactsSha1, buildName, buildNumber);
        }

        return queryResults;
    }

    /**
     * Sends an aql query to get all Sha1 value of the requested build, then returns a Map of the Sha1 values.
     */
    private Map<String, Boolean> fetchBuildArtifactsSha1() throws IOException {
        String includeSha1Field = ".include(\"actual_sha1\")";
        String buildAql = createAqlQueryForBuild(includeSha1Field);
        log.debug("Searching Artifactory for build's checksums using AQL query:\n" + buildAql);
        AqlSearchResult aqlSearchResult = client.searchArtifactsByAql(buildAql);
        return AqlUtils.extractSha1FromAqlResponse(aqlSearchResult.getResults());
    }

    private String createAqlQueryForBuild(String includeQueryPart) {
        return String.format("items.find(%s)%s", AqlUtils.createAqlBodyForBuild(buildName, buildNumber), includeQueryPart);
    }

}
