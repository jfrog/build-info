package org.jfrog.build.extractor.clientConfiguration.util;

import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.clientConfiguration.util.spec.FileSpec;
import org.jfrog.build.extractor.clientConfiguration.util.spec.Spec;

import java.io.IOException;
import java.util.List;

public class EditPropertiesHelper {
    public enum EditPropertiesActionType {
        SET,
        DELETE
    }

    private final ArtifactoryManager artifactoryManager;
    private final Log log;

    public EditPropertiesHelper(ArtifactoryManager artifactoryManager, Log log) {
        this.artifactoryManager = artifactoryManager;
        this.log = log;
    }

    public boolean editProperties(Spec spec, EditPropertiesActionType editType, String props) throws IOException {
        ArtifactorySearcher searcher = new ArtifactorySearcher(artifactoryManager, log);
        // Here to mark that at least one action has been successfully made. Needed for the failNoOp flag.
        boolean propertiesSet = false;

        for (FileSpec file : spec.getFiles()) {
            log.debug("Editing properties using spec: \n" + file.toString());
            if (editType == EditPropertiesActionType.SET) {
                propertiesSet = setPropertiesOnResults(searcher.SearchByFileSpec(file), props) || propertiesSet;
            } else {
                propertiesSet = deletePropertiesOnResults(searcher.SearchByFileSpec(file), props) || propertiesSet;
            }

        }
        return propertiesSet;
    }

    private boolean setPropertiesOnResults(List<AqlSearchResult.SearchEntry> searchResults, String props) throws IOException {
        boolean propertiesSet = false;
        log.info("Setting properties...");
        for (AqlSearchResult.SearchEntry result : searchResults) {
            String relativePath = buildEntryUrl(result);
            log.info(String.format("Setting the properties: '%s', on artifact: %s", props, relativePath));
            artifactoryManager.setProperties(relativePath, props, true);
            propertiesSet = true;
        }
        log.info("Done setting properties.");
        return propertiesSet;
    }

    private boolean deletePropertiesOnResults(List<AqlSearchResult.SearchEntry> searchResults, String props) throws IOException {
        boolean propertiesSet = false;
        log.info("Deleting properties...");
        for (AqlSearchResult.SearchEntry result : searchResults) {
            String relativePath = buildEntryUrl(result);
            log.info(String.format("Deleting the properties: '%s', on artifact: %s", props, relativePath));
            artifactoryManager.deleteProperties(relativePath, props);
            propertiesSet = true;
        }
        log.info("Done deleting properties.");
        return propertiesSet;
    }

    private String buildEntryUrl(AqlSearchResult.SearchEntry result) {
        String path = result.getPath().equals(".") ? "" : result.getPath() + "/";
        return result.getRepo() + "/" + path + result.getName();
    }
}
