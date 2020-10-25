package org.jfrog.build.extractor.clientConfiguration.util;

import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.clientConfiguration.util.spec.FileSpec;
import org.jfrog.build.extractor.clientConfiguration.util.spec.Spec;

import java.io.IOException;
import java.util.List;

public class EditPropertiesHelper {
    public enum EditPropertiesActionType {
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

    public boolean editProperties(Spec spec, EditPropertiesActionType editType, String props) throws IOException {
        ArtifactorySearcher searcher = new ArtifactorySearcher(client, log);
        // Here to mark that at least one action has been successfully made. Needed for the failNoOp flag.
        boolean propertiesSet = false;

        for (FileSpec file : spec.getFiles()) {
            log.debug("Editing properties using spec: \n" + file.toString());
            if (editType == EditPropertiesActionType.SET) {
                propertiesSet = propertiesSet || setPropertiesOnResults(searcher.SearchByFileSpec(file), props);
            } else {
                propertiesSet = propertiesSet || deletePropertiesOnResults(searcher.SearchByFileSpec(file), props);
            }

        }
        return propertiesSet;
    }

    private boolean setPropertiesOnResults(List<AqlSearchResult.SearchEntry> searchResults, String props) throws IOException {
        boolean propertiesSet = false;
        log.info("Setting properties...");
        validateSetProperties(props);
        for (AqlSearchResult.SearchEntry result : searchResults) {
            String url = buildEntryUrl(result);
            log.info(String.format("Setting the properties: \'%s\', on artifact: %s", props, url));
            client.setProperties(url, props);
            propertiesSet = true;
        }
        log.info("Done setting properties.");
        return propertiesSet;
    }

    private boolean deletePropertiesOnResults(List<AqlSearchResult.SearchEntry> searchResults, String props) throws IOException {
        boolean propertiesSet = false;
        log.info("Deleting properties...");
        for (AqlSearchResult.SearchEntry result : searchResults) {
            String url = buildEntryUrl(result);
            log.info(String.format("Deleting the properties: \'%s\', on artifact: %s", props, url));
            client.deleteProperties(url, props);
            propertiesSet = true;
        }
        log.info("Done deleting properties.");
        return propertiesSet;
    }

    private String buildEntryUrl(AqlSearchResult.SearchEntry result) {
        String path = result.getPath().equals(".") ? "" : result.getPath() + "/";
        return artifactoryEditPropsUrl + result.getRepo() + "/" + path + result.getName();
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
