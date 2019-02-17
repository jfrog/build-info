package org.jfrog.build.extractor.clientConfiguration.util;

import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.clientConfiguration.util.spec.FileSpec;
import org.jfrog.build.extractor.clientConfiguration.util.spec.Spec;
import org.jfrog.build.extractor.clientConfiguration.util.spec.validator.SearchBasedSpecValidator;

import java.io.IOException;
import java.util.List;

public class EditPropertiesHelper {
    public enum EditPropertiesCommandType {
        SET,
        DELETE
    }

    private ArtifactoryDependenciesClient client;
    private Log log;
    private String artifactoryEditPropsUrl;

    public EditPropertiesHelper(ArtifactoryDependenciesClient client, Log log) {
        this.client = client;
        this.log = log;
        this.artifactoryEditPropsUrl = StringUtils.stripEnd(client.getArtifactoryUrl(), "/") + "/api/storage/";
    }

    public boolean editProperties(Spec spec, EditPropertiesCommandType editType, String props) throws IOException {
        ArtifactorySearcher searcher = new ArtifactorySearcher(client, log);
        List<AqlSearchResult.SearchEntry> searchResults;
        boolean success = false;
        new SearchBasedSpecValidator().validate(spec);

        for (FileSpec file : spec.getFiles()) {
            log.debug("Editing properties using spec: \n" + file.toString());
            searchResults = searcher.SearchByFileSpec(file);
            success = success || editPropertiesOnResults(searchResults, editType, props);
        }
        return success;
    }

    private boolean editPropertiesOnResults(List<AqlSearchResult.SearchEntry> searchResults,
                                            EditPropertiesCommandType editType, String props) throws IOException {
        boolean propertiesSet = false;

        if (editType == EditPropertiesCommandType.SET) {
            log.info("Setting properties...");
            validateSetProperties(props);
        } else {
            log.info("Deleting properties...");
        }

        String curEntryUrl;
        for (AqlSearchResult.SearchEntry result : searchResults) {
            curEntryUrl = artifactoryEditPropsUrl + result.getRepo() + "/" + result.getPath();

            if (editType == EditPropertiesCommandType.SET) {
                client.setProperties(curEntryUrl, props);
            } else {
                client.deleteProperties(curEntryUrl, props);
            }

            propertiesSet = true;
        }

        if (editType == EditPropertiesCommandType.SET) {
            log.info("Done setting properties.");
        } else {
            log.info("Done deleting properties.");
        }

        return propertiesSet;
    }

    private void validateSetProperties(String props) throws IOException {
        for (String prop : props.trim().split(";")) {
            if (prop.isEmpty()) {
                continue;
            }

            String key = StringUtils.substringBefore(prop, "=");
            if (key.isEmpty()) {
                throw new IOException("Setting properties: Every property must have a key.");
            }

            String values = StringUtils.substringAfter(prop, "=");
            // Verify values aren't empty nor commas only
            if (values.isEmpty() || StringUtils.countMatches(values, ",") == values.length()) {
                throw new IOException("Setting properties: Every property must have at least one value.");
            }
        }
    }
}
