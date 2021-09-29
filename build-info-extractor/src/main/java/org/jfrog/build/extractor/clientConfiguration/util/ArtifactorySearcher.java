package org.jfrog.build.extractor.clientConfiguration.util;

import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.filespecs.entities.FilesGroup;

import java.io.IOException;
import java.util.List;

class ArtifactorySearcher {
    private final ArtifactoryManager artifactoryManager;
    private final Log log;

    ArtifactorySearcher(ArtifactoryManager artifactoryManager, Log log) {
        this.artifactoryManager = artifactoryManager;
        this.log = log;
    }

    List<AqlSearchResult.SearchEntry> SearchByFileSpec(FilesGroup file) throws IOException {
        List<AqlSearchResult.SearchEntry> results;
        AqlHelper aqlHelper = new AqlHelper(artifactoryManager, log, file);
        log.info("Searching for artifacts...");
        results = aqlHelper.run();
        log.info(String.format("Found %s artifacts.", results.size()));
        return results;
    }
}
