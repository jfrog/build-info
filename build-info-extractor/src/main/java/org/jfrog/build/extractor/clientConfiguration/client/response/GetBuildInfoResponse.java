package org.jfrog.build.extractor.clientConfiguration.client.response;

import org.jfrog.build.api.Build;

public class GetBuildInfoResponse {
    private Build buildInfo;

    public Build getBuildInfo() {
        return buildInfo;
    }

    public void setBuildInfo(Build buildInfo) {
        this.buildInfo = buildInfo;
    }
}
