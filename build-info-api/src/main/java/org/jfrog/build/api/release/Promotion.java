package org.jfrog.build.api.release;

import org.jfrog.build.api.Build;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * @author Noam Y. Tenne
 */
public class Promotion implements Serializable {

    public static final String STAGED = "Staged";
    public static final String RELEASED = "Released";
    public static final String ROLLED_BACK = "Rolled-back";

    private String status;
    private String comment;
    private String ciUser;
    private String timestamp;
    private boolean dryRun;
    private String targetRepo;
    private boolean copy;
    private boolean artifacts = true;
    private boolean dependencies = false;
    private Set<String> scopes;
    private Map<String, Collection<String>> properties;

    public Promotion() {
    }

    public Promotion(String status, String comment, String ciUser, String timestamp, boolean dryRun, String targetRepo,
            boolean copy, boolean artifacts, boolean dependencies, Set<String> scopes,
            Map<String, Collection<String>> properties) {
        this.status = status;
        this.comment = comment;
        this.ciUser = ciUser;
        this.timestamp = timestamp;
        this.dryRun = dryRun;
        this.targetRepo = targetRepo;
        this.copy = copy;
        this.artifacts = artifacts;
        this.dependencies = dependencies;
        this.scopes = scopes;
        this.properties = properties;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCiUser() {
        return ciUser;
    }

    public void setCiUser(String ciUser) {
        this.ciUser = ciUser;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Date getTimestampDate() {
        return getTimestampAsDate(getTimestamp());
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public String getTargetRepo() {
        return targetRepo;
    }

    public void setTargetRepo(String targetRepo) {
        this.targetRepo = targetRepo;
    }

    public boolean isCopy() {
        return copy;
    }

    public void setCopy(boolean copy) {
        this.copy = copy;
    }

    public boolean isArtifacts() {
        return artifacts;
    }

    public void setArtifacts(boolean artifacts) {
        this.artifacts = artifacts;
    }

    public boolean isDependencies() {
        return dependencies;
    }

    public void setDependencies(boolean dependencies) {
        this.dependencies = dependencies;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    public Map<String, Collection<String>> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Collection<String>> properties) {
        this.properties = properties;
    }

    private Date getTimestampAsDate(String timestamp) {
        if (timestamp == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat(Build.STARTED_FORMAT);
        try {
            return format.parse(timestamp);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}