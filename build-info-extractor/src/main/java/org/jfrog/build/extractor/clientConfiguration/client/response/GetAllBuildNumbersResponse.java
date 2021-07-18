package org.jfrog.build.extractor.clientConfiguration.client.response;

import java.util.ArrayList;
import java.util.List;

public class GetAllBuildNumbersResponse {
    public String uri;
    public List<BuildsNumberDetails> buildsNumbers = new ArrayList<>();

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public List<BuildsNumberDetails> getBuildsNumbers() {
        return buildsNumbers;
    }

    public void setBuildsNumbers(List<BuildsNumberDetails> buildsNumbers) {
        this.buildsNumbers = buildsNumbers;
    }

    public static class BuildsNumberDetails {
        public String uri;
        public String started;

        public String getStarted() {
            return started;
        }

        public void setStarted(String started) {
            this.started = started;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }
    }
}
