package org.jfrog.build.extractor.clientConfiguration.util;

import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.clientConfiguration.util.spec.FileSpec;

import java.io.IOException;
import java.util.List;

class ArtifactorySearcher {
    private final ArtifactoryManager artifactoryManager;
    private Log log;

    ArtifactorySearcher(ArtifactoryManager artifactoryManager, Log log) {
        this.artifactoryManager = artifactoryManager;
        this.log = log;
    }

    List<AqlSearchResult.SearchEntry> SearchByFileSpec(FileSpec file) throws IOException {
        List<AqlSearchResult.SearchEntry> results;
        AqlHelperBase aqlHelper = null;
        log.info("Searching for artifacts...");
        switch (file.getSpecType()) {
            case PATTERN: {
                aqlHelper = new PatternAqlHelper(artifactoryManager, log, file);
                break;
            }
            case BUILD: {
                aqlHelper = new BuildAqlHelper(artifactoryManager, log, file);
                break;
            }
            case AQL: {
                aqlHelper = new AqlHelperBase(artifactoryManager, log, file);
                break;
            }
        }
        results = aqlHelper.run();
        log.info(String.format("Found %s artifacts.", results.size()));
        return results;
    }
}
