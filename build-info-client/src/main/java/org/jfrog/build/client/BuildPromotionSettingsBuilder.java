package org.jfrog.build.client;

import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;

import java.util.Set;

/**
 * @author Noam Y. Tenne
 */
public class BuildPromotionSettingsBuilder {

    private String buildName;
    private String buildNumber;
    private String buildStarted;
    private String targetRepo;
    private boolean includeArtifacts = true;
    private boolean includeDependencies;
    private Set<String> scopes;
    private Multimap<String, String> properties;
    private boolean dryRun;
    private String promotionStatus;
    private String promotionComment;

    public BuildPromotionSettingsBuilder buildName(String buildName) {
        this.buildName = buildName;
        return this;
    }

    public BuildPromotionSettingsBuilder buildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
        return this;
    }

    public BuildPromotionSettingsBuilder buildStarted(String buildStarted) {
        this.buildStarted = buildStarted;
        return this;
    }

    public BuildPromotionSettingsBuilder targetRepo(String targetRepo) {
        this.targetRepo = targetRepo;
        return this;
    }

    public BuildPromotionSettingsBuilder includeArtifacts(boolean includeArtifacts) {
        this.includeArtifacts = includeArtifacts;
        return this;
    }

    public BuildPromotionSettingsBuilder includeDependencies(boolean includeDependencies) {
        this.includeDependencies = includeDependencies;
        return this;
    }

    public BuildPromotionSettingsBuilder scopes(Set<String> scopes) {
        this.scopes = scopes;
        return this;
    }

    public BuildPromotionSettingsBuilder properties(Multimap<String, String> properties) {
        this.properties = properties;
        return this;
    }

    public BuildPromotionSettingsBuilder dryRun(boolean dryRun) {
        this.dryRun = dryRun;
        return this;
    }

    public BuildPromotionSettingsBuilder promotionStatus(String promotionStatus) {
        this.promotionStatus = promotionStatus;
        return this;
    }

    public BuildPromotionSettingsBuilder promotionComment(String promotionComment) {
        this.promotionComment = promotionComment;
        return this;
    }

    public BuildPromotionSettings build() {
        if (StringUtils.isBlank(buildName)) {
            throw new IllegalArgumentException("Build name is required for promotion.");
        }
        if (StringUtils.isBlank(buildNumber)) {
            throw new IllegalArgumentException("Build number is required for promotion.");
        }
        if (StringUtils.isBlank(targetRepo)) {
            throw new IllegalArgumentException("Target repository is required for promotion.");
        }
        return new BuildPromotionSettings(buildName, buildNumber, buildStarted, targetRepo, includeArtifacts,
                includeDependencies, scopes, properties, dryRun, promotionStatus, promotionComment);
    }
}