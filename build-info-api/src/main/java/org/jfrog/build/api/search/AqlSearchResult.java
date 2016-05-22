package org.jfrog.build.api.search;

import java.util.ArrayList;
import java.util.List;


public class AqlSearchResult {
    private List<SearchEntry> results = new ArrayList<SearchEntry>();

    public List<SearchEntry> getResults() {
        return results;
    }

    public void setResults(List<SearchEntry> results) {
        this.results = results;
    }

    public static class SearchEntry  {
        private String repo;
        private String path;
        private String name;

        public void setRepo(String repo) {
            this.repo = repo;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRepo() {
            return repo;
        }

        public String getPath() {
            return path;
        }

        public String getName() {
            return name;
        }
    }
}
