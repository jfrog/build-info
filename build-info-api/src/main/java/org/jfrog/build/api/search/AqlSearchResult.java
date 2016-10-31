package org.jfrog.build.api.search;

import com.fasterxml.jackson.annotation.JsonProperty;

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
        private String actualSha1;
        private String actualMd5;
        private String[] virtualRepos = new String[]{};


        public void setRepo(String repo) {
            this.repo = repo;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public void setName(String name) {
            this.name = name;
        }

        @JsonProperty("actual_sha1")
        public void setActualSha1(String actualSha1) {
            this.actualSha1 = actualSha1;
        }

        @JsonProperty("actual_md5")
        public void setActualMd5(String actualMd5) {
            this.actualMd5 = actualMd5;
        }

        @JsonProperty("virtual_repos")
        public void setVirtualRepos(String[] virtualRepos) {
            this.virtualRepos = virtualRepos;
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

        @JsonProperty("actual_sha1")
        public String getActualSha1() {
            return actualSha1;
        }

        @JsonProperty("actual_md5")
        public String getActualMd5() {
            return actualMd5;
        }

        @JsonProperty("virtual_repos")
        public String[] getVirtualRepos() {
            return virtualRepos;
        }

    }
}
