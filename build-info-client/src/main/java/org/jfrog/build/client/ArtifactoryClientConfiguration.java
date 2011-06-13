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
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import static org.jfrog.build.api.BuildInfoConfigProperties.*;
import static org.jfrog.build.api.BuildInfoFields.*;
import static org.jfrog.build.api.BuildInfoProperties.BUILD_INFO_LICENSE_CONTROL_PREFIX;
import static org.jfrog.build.api.BuildInfoProperties.BUILD_INFO_PREFIX;
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

    public void fillFromProperties(Map<String, String> props) {
        for (Map.Entry<String, String> entry : props.entrySet()) {
            root.setStringValue(entry.getKey(), entry.getValue());
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
        Properties props = new Properties();
        props.putAll(root.props);
        props.putAll(rootConfig.props);
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

    public String getContextUrl() {
        String value = root.getStringValue(PROP_CONTEXT_URL);
        if (StringUtils.isBlank(value)) {
            throw new IllegalStateException("Context URL cannot be empty");
        }
        return value;
    }

    public void setContextUrl(String contextUrl) {
        root.setStringValue(PROP_CONTEXT_URL, contextUrl);
    }

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
        private final Predicate<String> matrixPredicate;

        public ResolverHandler() {
            super(PROP_RESOLVE_PREFIX);
            matrixPredicate = new Predicate<String>() {
                public boolean apply(String input) {
                    return input.startsWith(getMatrixParamPrefix());
                }
            };
        }

        public String getDownloadUrl() {
            // Legacy property from Gradle plugin apply from technique
            return root.getStringValue("artifactory.downloadUrl");
        }

        public String getContextUrl() {
            return root.getStringValue(PROP_CONTEXT_URL);
        }

        public void setContextUrl(String contextUrl) {
            root.setStringValue(PROP_CONTEXT_URL, contextUrl);
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

        public String injectMatrixParams(String rootUrl) {
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
            return injectMatrixParams(getUrl());
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

        public void addMatrixParam(String key, String value) {
            if (StringUtils.isBlank(key)) {
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
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                addMatrixParam(entry.getKey(), entry.getValue());
            }
        }

        public Map<String, String> getMatrixParams() {
            Map<String, String> result = Maps.newHashMap();
            String matrixPrefix = getMatrixParamPrefix();
            for (Map.Entry<String, String> entry : props.entrySet()) {
                if (entry.getKey().startsWith(matrixPrefix)) {
                    result.put(entry.getKey().substring(matrixPrefix.length()), entry.getValue());
                }
            }
            return result;
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

    public class BuildInfoHandler extends PrefixPropertyHandler {
        public final LicenseControlHandler licenseControl = new LicenseControlHandler();

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

        public void setBuildRoot(String buildRoot) {
            publisher.setBuildRoot(buildRoot);
            resolver.setBuildRoot(buildRoot);
            setStringValue(BUILD_ROOT, buildRoot);
        }

        public String getBuildRoot() {
            return getStringValue(BUILD_ROOT);
        }

        public void addBuildVariables(Map<String, String> buildVariables) {
            for (Map.Entry<String, String> entry : buildVariables.entrySet()) {
                addBuildVariable(entry.getKey(), entry.getValue());
            }
        }

        public void addBuildVariable(String key, String value) {
            setStringValue(ENVIRONMENT_PREFIX + key, value);
        }

        public void fillCommonSysProps() {
            String[] commonSysProps = {"os.arch", "os.name", "os.version", "java.version", "java.vm.info",
                    "java.vm.name", "java.vm.specification.name", "java.vm.vendor"};
            for (String prop : commonSysProps) {
                addBuildVariable(prop, System.getProperty(prop));
            }
        }

        public Map<String, String> getBuildVariables() {
            return Maps.filterKeys(props, buildVariablesPredicate);
        }
    }
}
