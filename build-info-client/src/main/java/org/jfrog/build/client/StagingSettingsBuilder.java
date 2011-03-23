package org.jfrog.build.client;

import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.release.Promotion;

/**
 * @author Noam Y. Tenne
 */
public class StagingSettingsBuilder {

    private String buildName;
    private String buildNumber;
    private Promotion promotion;

    public StagingSettingsBuilder(StagingSettingsBuilder copy) {
        this.buildName = copy.buildName;
        this.buildNumber = copy.buildNumber;
        this.promotion = copy.promotion;
    }

    public StagingSettingsBuilder(String buildName, String buildNumber) {
        this.buildName = buildName;
        this.buildNumber = buildNumber;
    }

    public StagingSettingsBuilder promotion(Promotion promotion) {
        this.promotion = promotion;
        return this;
    }

    public StagingSettings build() {
        if (StringUtils.isBlank(buildName)) {
            throw new IllegalArgumentException("Build name is required for promotion.");
        }
        if (StringUtils.isBlank(buildNumber)) {
            throw new IllegalArgumentException("Build number is required for promotion.");
        }
        return new StagingSettings(buildName, buildNumber, promotion != null ? promotion : new Promotion());
    }
}