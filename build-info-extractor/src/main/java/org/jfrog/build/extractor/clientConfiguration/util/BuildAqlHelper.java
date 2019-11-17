package org.jfrog.build.extractor.clientConfiguration.util;

import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.clientConfiguration.util.spec.FileSpec;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class BuildAqlHelper extends AqlHelperBase {

    BuildAqlHelper(ArtifactoryDependenciesClient client, Log log, FileSpec file) throws IOException {
        super(client, log, file);
    }

    @Override
    protected void convertFileSpecToAql(FileSpec file) throws IOException {
        super.buildQueryAdditionalParts(file);
        this.queryBody = createAqlBodyForBuild(buildName, buildNumber);
    }

    @Override
    protected List<AqlSearchResult.SearchEntry> filterResult(List<AqlSearchResult.SearchEntry> queryResults) throws IOException {
        Map<String, Boolean> buildArtifactsSha1 = extractSha1FromAqlResponse(queryResults);
        return filterAqlSearchResultsByBuild(queryResults, buildArtifactsSha1, buildName, buildNumber);
    }

}
