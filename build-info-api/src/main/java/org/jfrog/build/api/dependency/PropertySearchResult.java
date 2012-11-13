package org.jfrog.build.api.dependency;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Shay Yaakov
 */
public class PropertySearchResult {
    private List<SearchEntry> results = new ArrayList<SearchEntry>();

    public List<SearchEntry> getResults() {
        return results;
    }

    public void setResults(List<SearchEntry> results) {
        this.results = results;
    }

    public static class SearchEntry {
        private String uri;
        private String artifactoryUrl;
        private String repoUri;

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
            this.artifactoryUrl = StringUtils.substringBefore(uri, "/api/storage/");
            this.repoUri = StringUtils.substringAfter(uri, "/api/storage/");
        }

        public String getArtifactoryUrl() {
            return artifactoryUrl;
        }

        public String getRepoUri() {
            return repoUri;
        }

        public String getFilePath() {
            return StringUtils.substringAfter(repoUri, "/");
        }
    }
}
