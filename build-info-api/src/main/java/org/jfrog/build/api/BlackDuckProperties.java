package org.jfrog.build.api;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.io.Serializable;

/**
 * @author mamo
 */
@XStreamAlias("blackduck")
public class BlackDuckProperties implements Serializable {

    private boolean runChecks;
    private String appName;
    private String appVersion;
    private String reportRecipients; //csv
    private String scopes; //csv
    private boolean includePublishedArtifacts;
    private boolean autoCreateMissingComponentRequests;
    private boolean autoDiscardStaleComponentRequests;

    public BlackDuckProperties() {
    }

    public boolean isRunChecks() {
        return runChecks;
    }

    public void setRunChecks(boolean runChecks) {
        this.runChecks = runChecks;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getReportRecipients() {
        return reportRecipients;
    }

    public void setReportRecipients(String reportRecipients) {
        this.reportRecipients = reportRecipients;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public boolean isIncludePublishedArtifacts() {
        return includePublishedArtifacts;
    }

    public void setIncludePublishedArtifacts(boolean includePublishedArtifacts) {
        this.includePublishedArtifacts = includePublishedArtifacts;
    }

    public boolean isAutoCreateMissingComponentRequests() {
        return autoCreateMissingComponentRequests;
    }

    public void setAutoCreateMissingComponentRequests(boolean autoCreateMissingComponentRequests) {
        this.autoCreateMissingComponentRequests = autoCreateMissingComponentRequests;
    }

    public boolean isAutoDiscardStaleComponentRequests() {
        return autoDiscardStaleComponentRequests;
    }

    public void setAutoDiscardStaleComponentRequests(boolean autoDiscardStaleComponentRequests) {
        this.autoDiscardStaleComponentRequests = autoDiscardStaleComponentRequests;
    }
}
