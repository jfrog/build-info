package org.jfrog.build.api.builder;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.release.Promotion;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

/**
 * @author Noam Y. Tenne
 */
public class PromotionBuilder {

    private String status;
    private String comment;
    private String ciUser;
    private String timestamp;
    private boolean dryRun;
    private String targetRepo;
    private boolean copy;
    private boolean artifacts = true;
    private boolean dependencies = true;
    private Set<String> scopes;
    private Multimap<String, String> properties;

    public PromotionBuilder() {
    }

    public PromotionBuilder status(String status) {
        this.status = status;
        return this;
    }

    public PromotionBuilder comment(String comment) {
        this.comment = comment;
        return this;
    }

    public PromotionBuilder ciUser(String ciUser) {
        this.ciUser = ciUser;
        return this;
    }

    public PromotionBuilder artifactoryUser(String artifactoryUser) {
        return this;
    }

    public PromotionBuilder timestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public PromotionBuilder timestampDate(Date timestampDate) {
        if (timestampDate == null) {
            throw new IllegalArgumentException("Cannot format a null date.");
        }
        this.timestamp = new SimpleDateFormat(Build.STARTED_FORMAT).format(timestampDate);
        return this;
    }

    public PromotionBuilder dryRun(boolean dryRun) {
        this.dryRun = dryRun;
        return this;
    }

    public PromotionBuilder targetRepo(String targetRepo) {
        this.targetRepo = targetRepo;
        return this;
    }

    public PromotionBuilder copy(boolean copy) {
        this.copy = copy;
        return this;
    }

    public PromotionBuilder artifacts(boolean artifacts) {
        this.artifacts = artifacts;
        return this;
    }

    public PromotionBuilder dependencies(boolean dependencies) {
        this.dependencies = dependencies;
        return this;
    }

    public PromotionBuilder scopes(Set<String> scopes) {
        this.scopes = scopes;
        return this;
    }

    public PromotionBuilder addScope(String scope) {
        if (scopes == null) {
            scopes = Sets.newHashSet();
        }
        scopes.add(scope);
        return this;
    }

    public PromotionBuilder properties(Multimap<String, String> properties) {
        this.properties = properties;
        return this;
    }

    public PromotionBuilder addProperty(String key, String value) {
        if (properties == null) {
            properties = HashMultimap.create();
        }
        properties.put(key, value);
        return this;
    }

    public Promotion build() {
        return new Promotion(status, comment, ciUser, timestamp, dryRun, targetRepo, copy, artifacts, dependencies,
                scopes, properties);
    }
}
