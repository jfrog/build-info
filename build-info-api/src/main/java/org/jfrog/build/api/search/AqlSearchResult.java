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

    public static class SearchEntry {
        private String repo;
        private String path;
        private String name;
        private String actual_sha1;
        private String actual_md5;

        public void setRepo(String repo) {
            this.repo = repo;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setActual_sha1(String actual_sha1) {
            this.actual_sha1 = actual_sha1;
        }

        public void setActual_md5(String actual_md5) {
            this.actual_md5 = actual_md5;
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

        public String getActual_sha1() {
            return actual_sha1;
        }

        public String getActual_md5() {
            return actual_md5;
        }
    }
}
