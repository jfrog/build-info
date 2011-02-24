package org.jfrog.build.client;

import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;

import java.util.Set;

/**
 * @author Noam Y. Tenne
 */
public class StagingSettingsBuilder {

    private boolean move = true;
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
    private String ciUser;

    public StagingSettingsBuilder(StagingSettingsBuilder copy) {
        this.move = copy.move;
        this.buildName = copy.buildName;
        this.buildNumber = copy.buildNumber;
        this.buildStarted = copy.buildStarted;
        this.targetRepo = copy.targetRepo;
        this.includeArtifacts = copy.includeArtifacts;
        this.includeDependencies = copy.includeDependencies;
        this.scopes = copy.scopes;
        this.properties = copy.properties;
        this.dryRun = copy.dryRun;
        this.promotionStatus = copy.promotionStatus;
        this.promotionComment = copy.promotionComment;
        this.ciUser = copy.ciUser;
    }

    public StagingSettingsBuilder(String buildName, String buildNumber, String targetRepo) {
        this.buildName = buildName;
        this.buildNumber = buildNumber;
        this.targetRepo = targetRepo;
    }

    public StagingSettingsBuilder move(boolean move) {
        this.move = move;
        return this;
    }

    public StagingSettingsBuilder buildStarted(String buildStarted) {
        this.buildStarted = buildStarted;
        return this;
    }

    public StagingSettingsBuilder includeArtifacts(boolean includeArtifacts) {
        this.includeArtifacts = includeArtifacts;
        return this;
    }

    public StagingSettingsBuilder includeDependencies(boolean includeDependencies) {
        this.includeDependencies = includeDependencies;
        return this;
    }

    public StagingSettingsBuilder scopes(Set<String> scopes) {
        this.scopes = scopes;
        return this;
    }

    public StagingSettingsBuilder properties(Multimap<String, String> properties) {
        this.properties = properties;
        return this;
    }

    public StagingSettingsBuilder dryRun(boolean dryRun) {
        this.dryRun = dryRun;
        return this;
    }

    public StagingSettingsBuilder promotionStatus(String promotionStatus) {
        this.promotionStatus = promotionStatus;
        return this;
    }

    public StagingSettingsBuilder promotionComment(String promotionComment) {
        this.promotionComment = promotionComment;
        return this;
    }

    public StagingSettingsBuilder ciUser(String ciUser) {
        this.ciUser = ciUser;
        return this;
    }

    public StagingSettings build() {
        if (StringUtils.isBlank(buildName)) {
            throw new IllegalArgumentException("Build name is required for promotion.");
        }
        if (StringUtils.isBlank(buildNumber)) {
            throw new IllegalArgumentException("Build number is required for promotion.");
        }
        if (StringUtils.isBlank(targetRepo)) {
            throw new IllegalArgumentException("Target repository is required for promotion.");
        }
        return new StagingSettings(move, buildName, buildNumber, buildStarted, targetRepo, includeArtifacts,
                includeDependencies, scopes, properties, dryRun, promotionStatus, promotionComment, ciUser);
    }
}