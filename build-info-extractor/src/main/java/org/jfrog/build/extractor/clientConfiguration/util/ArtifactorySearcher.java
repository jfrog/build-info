package org.jfrog.build.extractor.clientConfiguration.util;

import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.clientConfiguration.util.spec.FileSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class ArtifactorySearcher {
    private ArtifactoryDependenciesClient client;
    private Log log;
    private static final String LAST_RELEASE = "LAST_RELEASE";

    ArtifactorySearcher(ArtifactoryDependenciesClient client, Log log) {
        this.client = client;
        this.log = log;
    }

    List<AqlSearchResult.SearchEntry> SearchByFileSpec(FileSpec file) throws IOException {
        List<AqlSearchResult.SearchEntry> results = null;
        AqlHelperBase aqlHelper = new AqlHelperBase(client, log);
        log.info("Searching for artifacts...");
        switch (file.getSpecType()) {
            case PATTERN: {
                aqlHelper = new PatternAqlHelper(client, log);
                break;
            }
            case BUILD: {
                aqlHelper = new BuildAqlHelper(client, log);
                break;
            }
            case AQL: {
                break;
            }
        }
        aqlHelper.convertFileSpecToAql(file);
        results = aqlHelper.run();
        results = (results == null ? new ArrayList<>() : results);
        log.info(String.format("Found %s artifacts.", results.size()));
        return results;
    }
}
