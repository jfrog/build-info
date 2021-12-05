package org.jfrog.build.api.dependency;

import org.apache.commons.lang3.StringUtils;

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
        private String repoKey;
        private String filePath;

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
            this.artifactoryUrl = StringUtils.substringBefore(uri, "/api/storage/");

            String repoPath = StringUtils.substringAfter(uri, "/api/storage/");
            this.repoKey = StringUtils.substringBefore(repoPath, "/");
            this.filePath = StringUtils.substringAfter(repoPath, "/");
        }

        public String getRepoUri() {
            return artifactoryUrl + "/" + repoKey;
        }

        public String getRepoPath() {
            return repoKey + "/" + filePath;
        }

        public String getFilePath() {
            return filePath;
        }
    }
}
