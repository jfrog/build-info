package org.jfrog.build.client;

import java.util.Collections;
import java.util.Map;

public class ArtifactoryResponse {
    private Map content = Collections.emptyMap();

    public Map getContent() {
        return content;
    }

    public void setContent(Map content) {
        this.content = content;
    }
}
