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
package org.jfrog.build.client;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.BlackDuckProperties;
import org.jfrog.build.api.BlackDuckPropertiesFields;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Issue;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.util.IssuesTrackerUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import static org.jfrog.build.api.BlackDuckPropertiesFields.*;
import static org.jfrog.build.api.BuildInfoConfigProperties.*;
import static org.jfrog.build.api.BuildInfoFields.*;
import static org.jfrog.build.api.BuildInfoProperties.*;
import static org.jfrog.build.api.IssuesTrackerFields.*;
import static org.jfrog.build.api.LicenseControlFields.*;
import static org.jfrog.build.client.ClientConfigurationFields.*;
import static org.jfrog.build.client.ClientProperties.*;

/**
 * @author freds
 */
public class ArtifactoryClientConfiguration {
    private final PrefixPropertyHandler root;
    /**
     * To configure the props builder itself, so all method of this classes delegated from here
     */
    private final PrefixPropertyHandler rootConfig;

    public final ResolverHandler resolver;
    public final PublisherHandler publisher;
    public final BuildInfoHandler info;
    public final ProxyHandler proxy;

    public ArtifactoryClientConfiguration(Log log) {
        this.root = new PrefixPropertyHandler(log, new TreeMap<String, String>());
        this.rootConfig = new PrefixPropertyHandler(root, BUILD_INFO_CONFIG_PREFIX);
        this.resolver = new ResolverHandler();
        this.publisher = new PublisherHandler();
        this.info = new BuildInfoHandler();
        this.proxy = new ProxyHandler();
    }

    public void fillFromProperties(Map<String, String> props, IncludeExcludePatterns patterns) {
        for (Map.Entry<String, String> entry : props.entrySet()) {
            String varKey = entry.getKey();
            if (PatternMatcher.pathConflicts(varKey, patterns)) {
                continue;
            }
            root.setStringValue(varKey, entry.getValue());
        }
    }

    public void fillFromProperties(Properties props) {
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            root.setStringValue((String) entry.getKey(), (String) entry.getValue());
        }
    }

    public Map<String, String> getAllProperties() {
        return root.props;
    }

    public Map<String, String> getAllRootConfig() {
        return rootConfig.props;
    }

    public void persistToPropertiesFile() {
        Predicate<String> nonNullPredicate = new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return StringUtils.isNotBlank(input);
            }
        };
        Properties props = new Properties();
        props.putAll(Maps.filterValues(root.props, nonNullPredicate));
        props.putAll(Maps.filterValues(rootConfig.props, nonNullPredicate));
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(new File(getPropertiesFile()));
            props.store(fos, "BuildInfo configuration property file");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            Closeables.closeQuietly(fos);
        }
    }

    /**
     * A fallback method for backward compatibility. If publisher/resolver context url is requested but not found this
     * method is called.
     *
     * @return URL of Artifactory server from the property artifactory.contextUrl
     * @deprecated Use only as a fallback when explicit publisher/resolver context url is missing
     */
    @Deprecated
    public String getContextUrl() {
        String value = root.getStringValue(PROP_CONTEXT_URL);
        if (StringUtils.isBlank(value)) {
            throw new IllegalStateException("Context URL cannot be empty");
        }
        return value;
    }

    /*public void setContextUrl(String contextUrl) {
        root.setStringValue(PROP_CONTEXT_URL, contextUrl);
    }*/

    public void setTimeout(Integer timeout) {
        root.setIntegerValue(PROP_TIMEOUT, timeout);
    }

    public Integer getTimeout() {
        return root.getIntegerValue(PROP_TIMEOUT);
    }

    public void setPropertiesFile(String propertyFile) {
        rootConfig.setStringValue(PROPERTIES_FILE, propertyFile);
    }

    public String getPropertiesFile() {
        return rootConfig.getStringValue(PROPERTIES_FILE);
    }

    public void setExportFile(String exportFile) {
        rootConfig.setStringValue(EXPORT_FILE, exportFile);
    }

    public String getExportFile() {
        return rootConfig.getStringValue(EXPORT_FILE);
    }

    public void setIncludeEnvVars(Boolean enabled) {
        rootConfig.setBooleanValue(INCLUDE_ENV_VARS, enabled);
    }

    public Boolean isIncludeEnvVars() {
        return rootConfig.getBooleanValue(INCLUDE_ENV_VARS, false);
    }

    public void setEnvVarsIncludePatterns(String patterns) {
        rootConfig.setStringValue(ENV_VARS_INCLUDE_PATTERNS, patterns);
    }

    public String getEnvVarsIncludePatterns() {
        return rootConfig.getStringValue(ENV_VARS_INCLUDE_PATTERNS);
    }

    public void setEnvVarsExcludePatterns(String patterns) {
        rootConfig.setStringValue(ENV_VARS_EXCLUDE_PATTERNS, patterns);
    }

    public String getEnvVarsExcludePatterns() {
        return rootConfig.getStringValue(ENV_VARS_EXCLUDE_PATTERNS);
    }

    public void setBuildListernerAdded(Boolean enabled) {
        root.setBooleanValue("__ArtifactoryPlugin_buildListener__", enabled);
    }

    public Boolean isBuildListernerAdded() {
        return root.getBooleanValue("__ArtifactoryPlugin_buildListener__", false);
    }

    public void setActivateRecorder(Boolean activateRecorder) {
        root.setBooleanValue(ACTIVATE_RECORDER, activateRecorder);
    }

    public Boolean isActivateRecorder() {
        return root.getBooleanValue(ACTIVATE_RECORDER, false);
    }

    public class ResolverHandler extends RepositoryConfiguration {

        public ResolverHandler() {
            super(PROP_RESOLVE_PREFIX);
        }

        public String getDownloadUrl() {
            // Legacy property from Gradle plugin apply from technique
            return root.getStringValue("artifactory.downloadUrl");
        }

        @SuppressWarnings({"deprecation"})
        public String getContextUrl() {
            String contextUrl = getStringValue(CONTEXT_URL);
            if (StringUtils.isBlank(contextUrl)) {
                // fallback to root contextUrl for backward compatibility
                contextUrl = ArtifactoryClientConfiguration.this.getContextUrl();
            }
            return contextUrl;
        }

        public void setContextUrl(String contextUrl) {
            setStringValue(CONTEXT_URL, contextUrl);
        }

        public void setBuildRoot(String buildRoot) {
            addMatrixParam(BUILD_ROOT, buildRoot);
        }

        public void setIvyRepositoryDefined(boolean ivyRepositoryDefined) {
            root.setBooleanValue(IVY_REPO_DEFINED, ivyRepositoryDefined);
        }

        public boolean isIvyRepositoryDefined() {
            return root.getBooleanValue(IVY_REPO_DEFINED, false);
        }

        public String getBuildRoot() {
            return getMatrixParams().get(BUILD_ROOT);
        }

        @Override
        public String getMatrixParamPrefix() {
            return getPrefix() + MATRIX;
        }
    }

    public class PublisherHandler extends RepositoryConfiguration {
        public PublisherHandler() {
            super(PROP_PUBLISH_PREFIX);
        }

        @SuppressWarnings({"deprecation"})
        public String getContextUrl() {
            String contextUrl = getStringValue(CONTEXT_URL);
            if (StringUtils.isBlank(contextUrl)) {
                // fallback to root contextUrl for backward compatibility
                contextUrl = ArtifactoryClientConfiguration.this.getContextUrl();
            }
            return contextUrl;
        }

        public void setContextUrl(String contextUrl) {
            setStringValue(CONTEXT_URL, contextUrl);
        }

        public void setSnapshotRepoKey(String repoKey) {
            setStringValue(SNAPSHOT_REPO_KEY, repoKey);
        }

        public String getSnapshotRepoKey() {
            return getStringValue(SNAPSHOT_REPO_KEY);
        }

        public void setPublishArtifacts(Boolean enabled) {
            setBooleanValue(PUBLISH_ARTIFACTS, enabled);
        }

        public Boolean isPublishArtifacts() {
            return getBooleanValue(PUBLISH_ARTIFACTS, true);
        }

        public void setPublishBuildInfo(Boolean enabled) {
            setBooleanValue(PUBLISH_BUILD_INFO, enabled);
        }

        public Boolean isPublishBuildInfo() {
            return getBooleanValue(PUBLISH_BUILD_INFO, true);
        }

        public void setIncludePatterns(String patterns) {
            setStringValue(INCLUDE_PATTERNS, patterns);
        }

        public String getIncludePatterns() {
            return getStringValue(INCLUDE_PATTERNS);
        }

        public void setExcludePatterns(String patterns) {
            setStringValue(EXCLUDE_PATTERNS, patterns);
        }

        public String getExcludePatterns() {
            return getStringValue(EXCLUDE_PATTERNS);
        }

        public void setEvenUnstable(Boolean enabled) {
            setBooleanValue(EVEN_UNSTABLE, enabled);
        }

        public Boolean isEvenUnstable() {
            return getBooleanValue(EVEN_UNSTABLE, false);
        }

        public void setBuildRoot(String buildRoot) {
            addMatrixParam(BUILD_ROOT, buildRoot);
        }

        public String getBuildRoot() {
            return getMatrixParams().get(BUILD_ROOT);
        }

        @Override
        public String getMatrixParamPrefix() {
            return PROP_DEPLOY_PARAM_PROP_PREFIX;
        }

        public void setArtifactSpecs(String artifactSpecs) {
            setStringValue(ARTIFACT_SPECS, artifactSpecs);
        }

        public ArtifactSpecs getArtifactSpecs() {
            String specs = getStringValue(ARTIFACT_SPECS);
            return new ArtifactSpecs(specs);
        }
    }

    public class ProxyHandler extends AuthenticationConfiguration {
        public ProxyHandler() {
            super(PROP_PROXY_PREFIX);
        }

        // TODO: Support proxy type SSL or not

        public void setHost(String host) {
            setStringValue(HOST, host);
        }

        public String getHost() {
            return getStringValue(HOST);
        }

        public void setPort(Integer port) {
            setIntegerValue(PORT, port);
        }

        public Integer getPort() {
            return getIntegerValue(PORT);
        }
    }

    public class AuthenticationConfiguration extends PrefixPropertyHandler {
        public AuthenticationConfiguration(String prefix) {
            super(root, prefix);
        }

        public void setUsername(String userName) {
            setStringValue(USERNAME, userName);
        }

        public String getUsername() {
            return getStringValue(USERNAME);
        }

        public void setPassword(String password) {
            setStringValue(PASSWORD, password);
        }

        public String getPassword() {
            return getStringValue(PASSWORD);
        }
    }

    public abstract class RepositoryConfiguration extends AuthenticationConfiguration {

        private ImmutableMap<String, String> calculatedMatrixParams;

        protected RepositoryConfiguration(String prefix) {
            super(prefix);
        }

        public void setName(String name) {
            setStringValue(NAME, name);
        }

        public String getName() {
            return getStringValue(NAME);
        }

        public void setUrl(String url) {
            setStringValue(URL, url);
        }

        public String urlWithMatrixParams(String rootUrl) {
            rootUrl = StringUtils.stripEnd(rootUrl, "/;");
            Map<String, String> matrixParams = getMatrixParams();
            if (matrixParams.isEmpty()) {
                return rootUrl;
            } else {
                StringBuilder builder = new StringBuilder(rootUrl);
                for (Map.Entry<String, String> entry : matrixParams.entrySet()) {
                    builder.append(';').append(entry.getKey()).append('=').append(entry.getValue());
                }
                builder.append(';');
                return builder.toString();
            }
        }

        public String getUrlWithMatrixParams() {
            return urlWithMatrixParams(getUrl());
        }

        public String getUrl() {
            String value = getStringValue(URL);
            if (StringUtils.isBlank(value)) {
                String repoKey = getRepoKey();
                if (StringUtils.isNotBlank(repoKey)) {
                    String contextUrl = getContextUrl();
                    if (StringUtils.isNotBlank(contextUrl)) {
                        contextUrl = StringUtils.stripEnd(contextUrl, "/ ");
                        return contextUrl + "/" + getRepoKey();
                    }
                }
            }
            return StringUtils.removeEnd(value, "/");
        }

        public void setRepoKey(String repoKey) {
            setStringValue(REPO_KEY, repoKey);
        }

        public String getRepoKey() {
            return getStringValue(REPO_KEY);
        }

        /**
         * In the context of a publisher it is used to publish a pom. In the context of a resolver it is used to add a
         * maven resolver (e.g. in Gradle).
         *
         * @param enabled true for enabling Maven resolution
         */
        public void setMaven(boolean enabled) {
            setBooleanValue(MAVEN, enabled);
        }

        public Boolean isMaven() {
            return getBooleanValue(MAVEN, true);
        }

        public void setIvy(Boolean enabled) {
            setBooleanValue(IVY, enabled);
        }

        public Boolean isIvy() {
            return getBooleanValue(IVY, false);
        }

        public void setM2Compatible(Boolean enabled) {
            setBooleanValue(IVY_M2_COMPATIBLE, enabled);
        }

        public boolean isM2Compatible() {
            return getBooleanValue(IVY_M2_COMPATIBLE, true);
        }

        public void setIvyArtifactPattern(String artPattern) {
            setStringValue(IVY_ART_PATTERN, artPattern);
        }

        public String getIvyArtifactPattern() {
            String value = getStringValue(IVY_ART_PATTERN);
            if (StringUtils.isBlank(value)) {
                return LayoutPatterns.M2_PATTERN;
            }
            return value.trim();
        }

        public void setIvyPattern(String ivyPattern) {
            setStringValue(IVY_IVY_PATTERN, ivyPattern);
        }

        public String getIvyPattern() {
            String value = getStringValue(IVY_IVY_PATTERN);
            if (StringUtils.isBlank(value)) {
                return LayoutPatterns.DEFAULT_IVY_PATTERN;
            }
            return value.trim();
        }


        public abstract String getMatrixParamPrefix();

        public abstract String getContextUrl();

        public void addMatrixParam(String key, String value) {
            ensureImmutableMatrixParams();
            if (StringUtils.isBlank(key) || StringUtils.isBlank(value)) {
                return;
            }
            String matrixParamPrefix = getMatrixParamPrefix();
            if (key.startsWith(matrixParamPrefix)) {
                props.put(key, value);
            } else {
                props.put(matrixParamPrefix + key, value);
            }
        }

        // INTERNAL METHOD
        public void addMatrixParams(Map<String, String> vars) {
            ensureImmutableMatrixParams();
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                addMatrixParam(entry.getKey(), entry.getValue());
            }
        }

        public ImmutableMap<String, String> getMatrixParams() {
            if (calculatedMatrixParams != null) {
                return calculatedMatrixParams;
            }
            Map<String, String> result = Maps.newHashMap();
            String matrixPrefix = getMatrixParamPrefix();
            for (Map.Entry<String, String> entry : props.entrySet()) {
                if (entry.getKey().startsWith(matrixPrefix)) {
                    result.put(entry.getKey().substring(matrixPrefix.length()), entry.getValue());
                }
            }
            this.calculatedMatrixParams = ImmutableMap.copyOf(result);
            return calculatedMatrixParams;
        }

        private void ensureImmutableMatrixParams() {
            if (calculatedMatrixParams != null) {
                throw new IllegalStateException("Matrix params already set and cannot be modified");
            }
        }

    }

    public class LicenseControlHandler extends PrefixPropertyHandler {
        public LicenseControlHandler() {
            super(root, BUILD_INFO_LICENSE_CONTROL_PREFIX);
        }

        public void setRunChecks(Boolean enabled) {
            setBooleanValue(RUN_CHECKS, enabled);
        }

        public Boolean isRunChecks() {
            return getBooleanValue(RUN_CHECKS, false);
        }

        public void setViolationRecipients(String recipients) {
            setStringValue(VIOLATION_RECIPIENTS, recipients);
        }

        public String getViolationRecipients() {
            return getStringValue(VIOLATION_RECIPIENTS);
        }

        public void setIncludePublishedArtifacts(Boolean enabled) {
            setBooleanValue(INCLUDE_PUBLISHED_ARTIFACTS, enabled);
        }

        public Boolean isIncludePublishedArtifacts() {
            return getBooleanValue(INCLUDE_PUBLISHED_ARTIFACTS, false);
        }

        public void setScopes(String scopes) {
            setStringValue(SCOPES, scopes);
        }

        public String getScopes() {
            return getStringValue(SCOPES);
        }

        public void setAutoDiscover(Boolean enabled) {
            setBooleanValue(AUTO_DISCOVER, enabled);
        }

        public Boolean isAutoDiscover() {
            return getBooleanValue(AUTO_DISCOVER, false);
        }
    }

    public class IssuesTrackerHandler extends PrefixPropertyHandler {
        public IssuesTrackerHandler() {
            super(root, BUILD_INFO_ISSUES_TRACKER_PREFIX);
        }

        public String getIssueTrackerName() {
            return getStringValue(ISSUES_TRACKER_NAME);
        }

        public void setIssueTrackerName(String issueTrackerName) {
            setStringValue(ISSUES_TRACKER_NAME, issueTrackerName);
        }

        public String getIssueTrackerVersion() {
            return getStringValue(ISSUES_TRACKER_VERSION);
        }

        public void setIssueTrackerVersion(String issueTrackerVersion) {
            setStringValue(ISSUES_TRACKER_VERSION, issueTrackerVersion);
        }

        public boolean getAggregateBuildIssues() {
            return getBooleanValue(AGGREGATE_BUILD_ISSUES);
        }

        public void setAggregateBuildIssues(boolean aggregateBuildIssues) {
            setBooleanValue(AGGREGATE_BUILD_ISSUES, aggregateBuildIssues);
        }

        public String getAggregationBuildStatus() {
            return getStringValue(AGGREGATION_BUILD_STATUS);
        }

        public void setAggregationBuildStatus(String aggregationBuildStatus) {
            setStringValue(AGGREGATION_BUILD_STATUS, aggregationBuildStatus);
        }

        public String getAffectedIssues() {
            return getStringValue(AFFECTED_ISSUES);
        }

        public void setAffectedIssues(String affectedIssues) {
            setStringValue(AFFECTED_ISSUES, affectedIssues);
        }

        public Set<Issue> getAffectedIssuesSet() {
            return IssuesTrackerUtils.getAffectedIssuesSet(getAffectedIssues());
        }
    }

    public class BlackDuckPropertiesHandler extends PrefixPropertyHandler {
        public BlackDuckPropertiesHandler() {
            super(root, BUILD_INFO_BLACK_DUCK_PROPERTIES_PREFIX);
        }

        public boolean isBlackDuckRunChecks() {
            return getBooleanValue(BLACK_DUCK_RUN_CHECKS, false);
        }

        public void setBlackDuckRunChecks(boolean blackDuckRunChecks) {
            setBooleanValue(BLACK_DUCK_RUN_CHECKS, blackDuckRunChecks);
        }

        public String getBlackDuckAppName() {
            return getStringValue(BLACK_DUCK_APP_NAME);
        }

        public void setBlackDuckAppName(String blackDuckAppName) {
            setStringValue(BLACK_DUCK_APP_NAME, blackDuckAppName);
        }

        public String getBlackDuckAppVersion() {
            return getStringValue(BLACK_DUCK_APP_VERSION);
        }

        public void setBlackDuckAppVersion(String blackDuckAppVersion) {
            setStringValue(BLACK_DUCK_APP_VERSION, blackDuckAppVersion);
        }

    }

    public class BuildInfoHandler extends PrefixPropertyHandler {
        public final LicenseControlHandler licenseControl = new LicenseControlHandler();
        public final IssuesTrackerHandler issues = new IssuesTrackerHandler();
        public final BlackDuckPropertiesHandler blackDuckProperties = new BlackDuckPropertiesHandler();

        private final Predicate<String> buildVariablesPredicate;

        public BuildInfoHandler() {
            super(root, BUILD_INFO_PREFIX);
            buildVariablesPredicate = new Predicate<String>() {
                public boolean apply(String input) {
                    return input.startsWith(BUILD_INFO_PREFIX + ENVIRONMENT_PREFIX);
                }
            };
        }

        public void setBuildName(String buildName) {
            setStringValue(BUILD_NAME, buildName);
        }

        public String getBuildName() {
            return getStringValue(BUILD_NAME);
        }

        public void setBuildNumber(String buildNumber) {
            setStringValue(BUILD_NUMBER, buildNumber);
        }

        public String getBuildNumber() {
            return getStringValue(BUILD_NUMBER);
        }

        public void setBuildTimestamp(String timestamp) {
            setStringValue(BUILD_TIMESTAMP, timestamp);
        }

        public String getBuildTimestamp() {
            return getStringValue(BUILD_TIMESTAMP);
        }

        public void setBuildStarted(String isoStarted) {
            setStringValue(BUILD_STARTED, isoStarted);
        }

        public void setBuildStarted(long timestamp) {
            setBuildStarted(Build.formatBuildStarted(timestamp));
        }

        public String getBuildStarted() {
            return getStringValue(BUILD_STARTED);
        }

        public void setPrincipal(String principal) {
            setStringValue(PRINCIPAL, principal);
        }

        public String getPrincipal() {
            return getStringValue(PRINCIPAL);
        }

        public void setBuildUrl(String buildUrl) {
            setStringValue(BUILD_URL, buildUrl);
        }

        public String getBuildUrl() {
            return getStringValue(BUILD_URL);
        }

        public void setVcsRevision(String vcsRevision) {
            setStringValue(VCS_REVISION, vcsRevision);
        }

        public String getVcsRevision() {
            return getStringValue(VCS_REVISION);
        }

        public void setAgentName(String agentName) {
            setStringValue(AGENT_NAME, agentName);
        }

        public String getAgentName() {
            return getStringValue(AGENT_NAME);
        }

        public void setAgentVersion(String agentVersion) {
            setStringValue(AGENT_VERSION, agentVersion);
        }

        public String getAgentVersion() {
            return getStringValue(AGENT_VERSION);
        }

        public void setBuildAgentName(String buildAgentName) {
            setStringValue(BUILD_AGENT_NAME, buildAgentName);
        }

        public String getBuildAgentName() {
            return getStringValue(BUILD_AGENT_NAME);
        }

        public void setBuildAgentVersion(String buildAgentVersion) {
            setStringValue(BUILD_AGENT_VERSION, buildAgentVersion);
        }

        public String getBuildAgentVersion() {
            return getStringValue(BUILD_AGENT_VERSION);
        }

        public void setParentBuildName(String parentBuildName) {
            setStringValue(BUILD_PARENT_NAME, parentBuildName);
        }

        public String getParentBuildName() {
            return getStringValue(BUILD_PARENT_NAME);
        }

        public void setParentBuildNumber(String parentBuildNumber) {
            setStringValue(BUILD_PARENT_NUMBER, parentBuildNumber);
        }

        public String getParentBuildNumber() {
            return getStringValue(BUILD_PARENT_NUMBER);
        }

        public void setDeleteBuildArtifacts(Boolean deleteBuildArtifacts) {
            setBooleanValue(DELETE_BUILD_ARTIFACTS, deleteBuildArtifacts);
        }

        public Boolean isDeleteBuildArtifacts() {
            return getBooleanValue(DELETE_BUILD_ARTIFACTS, true);
        }

        public void setBuildRetentionDays(Integer daysToKeep) {
            setIntegerValue(BUILD_RETENTION_DAYS, daysToKeep);
        }

        public Integer getBuildRetentionDays() {
            return getIntegerValue(BUILD_RETENTION_DAYS);
        }

        public void setBuildRetentionMinimumDate(String date) {
            setStringValue(BUILD_RETENTION_MINIMUM_DATE, date);
        }

        public String getBuildRetentionMinimumDate() {
            return getStringValue(BUILD_RETENTION_MINIMUM_DATE);
        }

        public void setBuildNumbersNotToDelete(String buildNumbersNotToDelete) {
            setStringValue(BUILD_NUMBERS_NOT_TO_DELETE, buildNumbersNotToDelete);
        }

        public String[] getBuildNumbersNotToDelete() {
            String value = getStringValue(BUILD_NUMBERS_NOT_TO_DELETE);
            if (StringUtils.isNotBlank(value)) {
                return StringUtils.split(value, ",");
            }
            return new String[0];
        }

        public void setReleaseComment(String comment) {
            setStringValue(RELEASE_COMMENT, comment);
        }

        public String getReleaseComment() {
            return getStringValue(RELEASE_COMMENT);
        }

        public void setReleaseEnabled(Boolean enabled) {
            setBooleanValue(RELEASE_ENABLED, enabled);
        }

        public Boolean isReleaseEnabled() {
            return getBooleanValue(RELEASE_ENABLED, false);
        }

        public void setBuildRoot(String buildRoot) throws UnsupportedEncodingException {
            publisher.setBuildRoot(buildRoot);
            resolver.setBuildRoot(URLEncoder.encode(buildRoot, "UTF-8"));
            setStringValue(BUILD_ROOT, buildRoot);
        }

        public String getBuildRoot() {
            return getStringValue(BUILD_ROOT);
        }

        public void addBuildVariables(Map<String, String> buildVariables, IncludeExcludePatterns patterns) {
            for (Map.Entry<String, String> entry : buildVariables.entrySet()) {
                String varKey = entry.getKey();
                if (PatternMatcher.pathConflicts(varKey, patterns)) {
                    continue;
                }
                addBuildVariable(varKey, entry.getValue());
            }
        }

        private void addBuildVariable(String key, String value) {
            setStringValue(ENVIRONMENT_PREFIX + key, value);
        }
    }
}
