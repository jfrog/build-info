package org.jfrog.build.extractor.clientConfiguration.util;

import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.clientConfiguration.util.spec.FileSpec;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class BuildAqlHelper extends AqlHelperBase {

    BuildAqlHelper(ArtifactoryDependenciesClient client, Log log) {
        super(client, log);
    }

    @Override
    public void convertFileSpecToAql(FileSpec file) throws IOException {
        super.buildQueryAdditionalParts(file);
        this.queryBody = AqlUtils.createAqlBodyForBuild(buildName, buildNumber);
    }

    @Override
    protected List<AqlSearchResult.SearchEntry> filterResult(List<AqlSearchResult.SearchEntry> queryResults) throws IOException {
        Map<String, Boolean> buildArtifactsSha1 = AqlUtils.extractSha1FromAqlResponse(queryResults);
        return AqlUtils.filterAqlSearchResultsByBuild(queryResults, buildArtifactsSha1, buildName, buildNumber);
    }

}
