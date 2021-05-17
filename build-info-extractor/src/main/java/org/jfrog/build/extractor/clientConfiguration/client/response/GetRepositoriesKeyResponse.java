package org.jfrog.build.extractor.clientConfiguration.client.response;

import java.util.List;
import java.util.stream.Collectors;

public class GetRepositoriesKeyResponse {
    private List<LocalRepository> results;

    public List<LocalRepository> getResults() {
        return results;
    }

    public void setResults(List<LocalRepository> key) {
        this.results = key;
    }

    public List<String> getRepositoriesKey() {
        return results.stream().map(LocalRepository::getKey).collect(Collectors.toList());
    }

    public class LocalRepository {
        private String key;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }
}
