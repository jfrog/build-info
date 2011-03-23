package org.jfrog.build.client;

import org.jfrog.build.api.release.Promotion;

/**
 * @author Noam Y. Tenne
 */
public class StagingSettings {

    private String buildName;
    private String buildNumber;
    private Promotion promotion;

    public StagingSettings(String buildName, String buildNumber, Promotion promotion) {
        this.buildName = buildName;
        this.buildNumber = buildNumber;
        this.promotion = promotion;
    }

    public String getBuildName() {
        return buildName;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public Promotion getPromotion() {
        return promotion;
    }
}
