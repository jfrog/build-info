/*
 * Copyright (C) 2011 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.build.api;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * Information about the agent that triggered the build (e.g. Hudson, TeamCity etc.).
 *
 * @author Noam Y. Tenne
 */
@XStreamAlias("licensecontrol")
public class LicenseControl implements Serializable {

    private boolean runChecks = true;
    private boolean includePublishedArtifacts = false;
    private boolean autoDiscover = true;

    private String[] licenseViolationRecipients;
    private String[] scopes;

    public LicenseControl() {
    }

    public LicenseControl(boolean runChecks) {
        this.runChecks = runChecks;
    }

    public boolean isRunChecks() {
        return runChecks;
    }

    public boolean isIncludePublishedArtifacts() {
        return includePublishedArtifacts;
    }

    public void setIncludePublishedArtifacts(boolean includePublishedArtifacts) {
        this.includePublishedArtifacts = includePublishedArtifacts;
    }

    public void setRunChecks(boolean runChecks) {
        this.runChecks = runChecks;
    }

    public boolean isAutoDiscover() {
        return autoDiscover;
    }

    public void setAutoDiscover(boolean autoDiscover) {
        this.autoDiscover = autoDiscover;
    }

    public String[] getLicenseViolationRecipients() {
        return licenseViolationRecipients;
    }

    public void setLicenseViolationRecipients(String[] licenseViolationRecipients) {
        this.licenseViolationRecipients = licenseViolationRecipients;
    }

    public String[] getScopes() {
        return scopes;
    }

    public void setScopes(String[] scopes) {
        this.scopes = scopes;
    }

    /**
     * Set the emails of recipients about license violations from a comma separated list
     *
     * @param licenseViolationRecipients
     */
    public void setLicenseViolationsRecipientsList(String licenseViolationRecipients) {
        if (StringUtils.isNotBlank(licenseViolationRecipients)) {
            String[] recipients = StringUtils.split(licenseViolationRecipients, " ");
            setLicenseViolationRecipients(recipients);
        }
    }

    public void setScopesList(String scopes) {
        if (StringUtils.isNotBlank(scopes)) {
            String[] splitScopes = StringUtils.split(scopes, " ");
            setScopes(splitScopes);
        }
    }

    public String getScopesList() {
        StringBuilder builder = new StringBuilder();
        String[] scopes = getScopes();
        if (scopes == null || scopes.length == 0) {
            return builder.toString();
        }
        for (String scope : scopes) {
            builder.append(scope).append(" ");
        }
        return builder.toString();
    }

    public String getLicenseViolationsRecipientsList() {
        StringBuilder builder = new StringBuilder();
        String[] recipients = getLicenseViolationRecipients();
        if (recipients == null || recipients.length == 0) {
            return builder.toString();
        }
        for (String recipient : recipients) {
            builder.append(recipient).append(" ");
        }
        return builder.toString();
    }

    public org.jfrog.build.api.ci.LicenseControl ToBuildInfoLicenseControl() {
        org.jfrog.build.api.ci.LicenseControl result = new org.jfrog.build.api.ci.LicenseControl();
        result.setAutoDiscover(autoDiscover);
        result.setLicenseViolationRecipients(licenseViolationRecipients);
        result.setScopes(scopes);
        result.setIncludePublishedArtifacts(includePublishedArtifacts);
        result.setRunChecks(runChecks);
        return result;
    }
}