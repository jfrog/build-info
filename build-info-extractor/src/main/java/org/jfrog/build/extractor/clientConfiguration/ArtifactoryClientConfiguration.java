package org.jfrog.build.extractor.clientConfiguration;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.CommonUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.extractor.ci.BuildInfoFields;
import org.jfrog.build.extractor.ci.Issue;
import org.jfrog.build.extractor.clientConfiguration.util.IssuesTrackerUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Predicate;

import static org.jfrog.build.extractor.ModuleParallelDeployHelper.DEFAULT_DEPLOYMENT_THREADS;
import static org.jfrog.build.extractor.ci.BuildInfoConfigProperties.ACTIVATE_RECORDER;
import static org.jfrog.build.extractor.ci.BuildInfoConfigProperties.BUILD_INFO_CONFIG_PREFIX;
import static org.jfrog.build.extractor.ci.BuildInfoConfigProperties.ENV_VARS_EXCLUDE_PATTERNS;
import static org.jfrog.build.extractor.ci.BuildInfoConfigProperties.ENV_VARS_INCLUDE_PATTERNS;
import static org.jfrog.build.extractor.ci.BuildInfoConfigProperties.EXPORT_FILE;
import static org.jfrog.build.extractor.ci.BuildInfoConfigProperties.INCLUDE_ENV_VARS;
import static org.jfrog.build.extractor.ci.BuildInfoConfigProperties.PROPERTIES_FILE;
import static org.jfrog.build.extractor.ci.BuildInfoFields.AGENT_NAME;
import static org.jfrog.build.extractor.ci.BuildInfoFields.AGENT_VERSION;
import static org.jfrog.build.extractor.ci.BuildInfoFields.ARTIFACTORY_PLUGIN_VERSION;
import static org.jfrog.build.extractor.ci.BuildInfoFields.BACKWARD_COMPATIBLE_DEPLOYABLE_ARTIFACTS;
import static org.jfrog.build.extractor.ci.BuildInfoFields.BUILD_AGENT_NAME;
import static org.jfrog.build.extractor.ci.BuildInfoFields.BUILD_AGENT_VERSION;
import static org.jfrog.build.extractor.ci.BuildInfoFields.BUILD_NAME;
import static org.jfrog.build.extractor.ci.BuildInfoFields.BUILD_NUMBER;
import static org.jfrog.build.extractor.ci.BuildInfoFields.BUILD_NUMBERS_NOT_TO_DELETE;
import static org.jfrog.build.extractor.ci.BuildInfoFields.BUILD_PARENT_NAME;
import static org.jfrog.build.extractor.ci.BuildInfoFields.BUILD_PARENT_NUMBER;
import static org.jfrog.build.extractor.ci.BuildInfoFields.BUILD_PROJECT;
import static org.jfrog.build.extractor.ci.BuildInfoFields.BUILD_RETENTION_ASYNC;
import static org.jfrog.build.extractor.ci.BuildInfoFields.BUILD_RETENTION_COUNT;
import static org.jfrog.build.extractor.ci.BuildInfoFields.BUILD_RETENTION_DAYS;
import static org.jfrog.build.extractor.ci.BuildInfoFields.BUILD_RETENTION_MINIMUM_DATE;
import static org.jfrog.build.extractor.ci.BuildInfoFields.BUILD_ROOT;
import static org.jfrog.build.extractor.ci.BuildInfoFields.BUILD_STARTED;
import static org.jfrog.build.extractor.ci.BuildInfoFields.BUILD_TIMESTAMP;
import static org.jfrog.build.extractor.ci.BuildInfoFields.BUILD_URL;
import static org.jfrog.build.extractor.ci.BuildInfoFields.DELETE_BUILD_ARTIFACTS;
import static org.jfrog.build.extractor.ci.BuildInfoFields.DEPLOYABLE_ARTIFACTS;
import static org.jfrog.build.extractor.ci.BuildInfoFields.ENVIRONMENT_PREFIX;
import static org.jfrog.build.extractor.ci.BuildInfoFields.GENERATED_BUILD_INFO;
import static org.jfrog.build.extractor.ci.BuildInfoFields.INCREMENTAL;
import static org.jfrog.build.extractor.ci.BuildInfoFields.MIN_CHECKSUM_DEPLOY_SIZE_KB;
import static org.jfrog.build.extractor.ci.BuildInfoFields.PRINCIPAL;
import static org.jfrog.build.extractor.ci.BuildInfoFields.RELEASE_COMMENT;
import static org.jfrog.build.extractor.ci.BuildInfoFields.RELEASE_ENABLED;
import static org.jfrog.build.extractor.ci.BuildInfoFields.RUN_PARAMETERS;
import static org.jfrog.build.extractor.ci.BuildInfoFields.VCS_BRANCH;
import static org.jfrog.build.extractor.ci.BuildInfoFields.VCS_MESSAGE;
import static org.jfrog.build.extractor.ci.BuildInfoFields.VCS_REVISION;
import static org.jfrog.build.extractor.ci.BuildInfoFields.VCS_URL;
import static org.jfrog.build.extractor.ci.BuildInfoProperties.BUILD_INFO_ISSUES_TRACKER_PREFIX;
import static org.jfrog.build.extractor.ci.BuildInfoProperties.BUILD_INFO_PREFIX;
import static org.jfrog.build.extractor.ci.IssuesTrackerFields.AFFECTED_ISSUES;
import static org.jfrog.build.extractor.ci.IssuesTrackerFields.AGGREGATE_BUILD_ISSUES;
import static org.jfrog.build.extractor.ci.IssuesTrackerFields.AGGREGATION_BUILD_STATUS;
import static org.jfrog.build.extractor.ci.IssuesTrackerFields.ISSUES_TRACKER_NAME;
import static org.jfrog.build.extractor.ci.IssuesTrackerFields.ISSUES_TRACKER_VERSION;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.ADD_DEPLOYABLE_ARTIFACTS;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.ARTIFACT_SPECS;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.CONTEXT_URL;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.DOCKER_HOST;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.DOCKER_IMAGE_TAG;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.DOTNET_NUGET_PROTOCOL;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.DOTNET_USE_DOTNET_CORE_CLI;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.DOWN_SNAPSHOT_REPO_KEY;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.EVEN_UNSTABLE;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.EXCLUDE_PATTERNS;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.FILTER_EXCLUDED_ARTIFACTS_FROM_BUILD;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.GO_PUBLISHED_VERSION;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.HOST;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.INCLUDE_PATTERNS;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.IVY;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.IVY_ART_PATTERN;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.IVY_IVY_PATTERN;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.IVY_M2_COMPATIBLE;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.IVY_REPO_DEFINED;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.JIB_IMAGE_FILE;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.KANIKO_IMAGE_FILE;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.MATRIX;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.MAVEN;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.NAME;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.NO_PROXY;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.NPM_CI_COMMAND;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.PACKAGE_MANAGER_ARGS;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.PACKAGE_MANAGER_MODULE;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.PACKAGE_MANAGER_PATH;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.PASSWORD;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.PIP_ENV_ACTIVATION;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.PORT;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.PUBLICATIONS;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.PUBLISH_ARTIFACTS;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.PUBLISH_BUILD_INFO;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.PUBLISH_FORK_COUNT;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.RECORD_ALL_DEPENDENCIES;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.RELEASE_REPO_KEY;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.REPO_KEY;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.SNAPSHOT_REPO_KEY;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.URL;
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.USERNAME;
import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.DEPRECATED_PROP_DEPLOY_PARAM_PROP_PREFIX;
import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.PROP_CONNECTION_RETRIES;
import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.PROP_CONTEXT_URL;
import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX;
import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.PROP_DOCKER_PREFIX;
import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.PROP_DOTNET_PREFIX;
import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.PROP_GO_PREFIX;
import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.PROP_HTTPS_PROXY_PREFIX;
import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.PROP_INSECURE_TLS;
import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.PROP_MAX_CO_PER_ROUTE;
import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.PROP_MAX_TOTAL_CO;
import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.PROP_NPM_PREFIX;
import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.PROP_PACKAGE_MANAGER_PREFIX;
import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.PROP_PIP_PREFIX;
import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.PROP_PROXY_PREFIX;
import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.PROP_PUBLISH_PREFIX;
import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.PROP_RESOLVE_PREFIX;
import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.PROP_SO_TIMEOUT;
import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.PROP_TIMEOUT;

/**
 * @author freds
 */
public class ArtifactoryClientConfiguration {
    // Try checksum deploy of files greater than 10KB
    public static final transient int DEFAULT_MIN_CHECKSUM_DEPLOY_SIZE_KB = 10;
    public static final String DEFAULT_NUGET_PROTOCOL = "v2";

    public final ResolverHandler resolver;
    public final PublisherHandler publisher;
    public final BuildInfoHandler info;
    public final ProxyHandler proxy;
    public final ProxyHandler httpsProxy;
    public final PackageManagerHandler packageManagerHandler;
    public final NpmHandler npmHandler;
    public final PipHandler pipHandler;
    public final DotnetHandler dotnetHandler;
    public final DockerHandler dockerHandler;
    public final GoHandler goHandler;
    public final PrefixPropertyHandler root;
    /**
     * To configure the props builder itself, so all method of this classes delegated from here
     */
    private final PrefixPropertyHandler rootConfig;

    public ArtifactoryClientConfiguration(Log log) {
        this.root = new PrefixPropertyHandler(log, new ConcurrentSkipListMap<String, String>());
        this.rootConfig = new PrefixPropertyHandler(root, BUILD_INFO_CONFIG_PREFIX);
        this.resolver = new ResolverHandler();
        this.publisher = new PublisherHandler();
        this.info = new BuildInfoHandler();
        this.proxy = new ProxyHandler();
        this.httpsProxy = new HttpsProxyHandler();
        this.packageManagerHandler = new PackageManagerHandler();
        this.npmHandler = new NpmHandler();
        this.pipHandler = new PipHandler();
        this.dotnetHandler = new DotnetHandler();
        this.dockerHandler = new DockerHandler();
        this.goHandler = new GoHandler();
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

    /**
     * Add properties to the client configuration.
     *
     * @param props The properties to be added.
     */
    public void fillFromProperties(Properties props) {
        fillFromProperties(props, null);
    }

    /**
     * Add properties to the client configuration, excluding specific properties, if they already exist in the client configuration.
     *
     * @param props                  The properties to be added to the client configuration.
     * @param excludeIfAlreadyExists A collection of property names which will not be added to the client configuration
     *                               if they already exist in it.
     */
    public void fillFromProperties(Properties props, Set<String> excludeIfAlreadyExists) {
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String key = (String) entry.getKey();
            if (excludeIfAlreadyExists == null ||
                    !excludeIfAlreadyExists.contains(key) || root.getStringValue(key) == null) {
                root.setStringValue(key, (String) entry.getValue());
            }
        }
    }

    public Map<String, String> getAllProperties() {
        return root.props;
    }

    public Map<String, String> getAllRootConfig() {
        return rootConfig.props;
    }

    public Log getLog() {
        return root.getLog();
    }

    public void persistToPropertiesFile() {
        if (StringUtils.isEmpty(getPropertiesFile())) {
            return;
        }
        Properties props = new Properties();
        props.putAll(filterMapNullValues(root.props));
        props.putAll(filterMapNullValues(rootConfig.props));
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(new File(getPropertiesFile()).getCanonicalFile());
            props.store(fos, "BuildInfo configuration property file");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }

    public static Map<String, String> filterMapNullValues(Map<String, String> map) {
        Map<String, String> result = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (StringUtils.isNotBlank(entry.getValue())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
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
        return root.getStringValue(PROP_CONTEXT_URL);
    }

    public void setTimeoutSec(Integer timeout) {
        setTimeout(timeout);
    }

    public Integer getTimeout() {
        return root.getIntegerValue(PROP_TIMEOUT);
    }

    public void setTimeout(Integer timeout) {
        root.setIntegerValue(PROP_TIMEOUT, timeout);
    }

    public void setConnectionRetries(Integer connectionRetries) {
        root.setIntegerValue(PROP_CONNECTION_RETRIES, connectionRetries);
    }

    public Integer getConnectionRetries() {
        return root.getIntegerValue(PROP_CONNECTION_RETRIES);
    }

    public boolean getInsecureTls() {
        return root.getBooleanValue(PROP_INSECURE_TLS, false);
    }

    public void setInsecureTls(boolean enabled) {
        root.setBooleanValue(PROP_INSECURE_TLS, enabled);
    }

    public Integer getSocketTimeout() {
        return root.getIntegerValue(PROP_SO_TIMEOUT);
    }

    public void setSocketTimeout(Integer socketTimeout) {
        root.setIntegerValue(PROP_SO_TIMEOUT, socketTimeout);
    }

    public Integer getMaxTotalConnection() {
        return root.getIntegerValue(PROP_MAX_TOTAL_CO);
    }

    public void setMaxTotalConnection(Integer maxTotalConnection) {
        root.setIntegerValue(PROP_MAX_TOTAL_CO, maxTotalConnection);
    }

    public Integer getMaxConnectionPerRoute() {
        return root.getIntegerValue(PROP_MAX_CO_PER_ROUTE);
    }

    public void setMaxConnectionPerRoute(Integer maxConnectionPerRoute) {
        root.setIntegerValue(PROP_MAX_CO_PER_ROUTE, maxConnectionPerRoute);
    }

    public String getPropertiesFile() {
        return rootConfig.getStringValue(PROPERTIES_FILE);
    }

    public void setPropertiesFile(String propertyFile) {
        rootConfig.setStringValue(PROPERTIES_FILE, propertyFile);
    }

    public String getExportFile() {
        return rootConfig.getStringValue(EXPORT_FILE);
    }

    public void setExportFile(String exportFile) {
        rootConfig.setStringValue(EXPORT_FILE, exportFile);
    }

    public void setIncludeEnvVars(Boolean enabled) {
        rootConfig.setBooleanValue(INCLUDE_ENV_VARS, enabled);
    }

    public Boolean isIncludeEnvVars() {
        return rootConfig.getBooleanValue(INCLUDE_ENV_VARS, false);
    }

    public String getEnvVarsIncludePatterns() {
        return rootConfig.getStringValue(ENV_VARS_INCLUDE_PATTERNS);
    }

    public void setEnvVarsIncludePatterns(String patterns) {
        rootConfig.setStringValue(ENV_VARS_INCLUDE_PATTERNS, patterns);
    }

    public String getEnvVarsExcludePatterns() {
        return rootConfig.getStringValue(ENV_VARS_EXCLUDE_PATTERNS);
    }

    public void setEnvVarsExcludePatterns(String patterns) {
        rootConfig.setStringValue(ENV_VARS_EXCLUDE_PATTERNS, patterns);
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

        public boolean isIvyRepositoryDefined() {
            return root.getBooleanValue(IVY_REPO_DEFINED, false);
        }

        public void setIvyRepositoryDefined(boolean ivyRepositoryDefined) {
            root.setBooleanValue(IVY_REPO_DEFINED, ivyRepositoryDefined);
        }

        public String getBuildRoot() {
            return getMatrixParams().get(BUILD_ROOT);
        }

        public void setBuildRoot(String buildRoot) {
            addMatrixParam(BUILD_ROOT, buildRoot);
        }

        @Override
        public String getMatrixParamPrefix() {
            return getPrefix() + MATRIX;
        }

        @Override
        public String getDeprecatedMatrixParamPrefix() {
            return getDeprecatedPrefix() + MATRIX;
        }

        public String getDownloadSnapshotRepoKey() {
            return getStringValue(DOWN_SNAPSHOT_REPO_KEY);
        }

        public void setDownloadSnapshotRepoKey(String repoKey) {
            setStringValue(DOWN_SNAPSHOT_REPO_KEY, repoKey);
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

        public String getSnapshotRepoKey() {
            return getStringValue(SNAPSHOT_REPO_KEY);
        }

        public void setSnapshotRepoKey(String repoKey) {
            setStringValue(SNAPSHOT_REPO_KEY, repoKey);
        }

        public String getReleaseRepoKey() {
            return getStringValue(RELEASE_REPO_KEY);
        }

        public void setReleaseRepoKey(String repoKey) {
            setStringValue(RELEASE_REPO_KEY, repoKey);
        }

        public void setPublishArtifacts(Boolean enabled) {
            setBooleanValue(PUBLISH_ARTIFACTS, enabled);
        }

        public Boolean isPublishArtifacts() {
            return getBooleanValue(PUBLISH_ARTIFACTS, true);
        }

        public Boolean shouldAddDeployableArtifacts() {
            return getBooleanValue(ADD_DEPLOYABLE_ARTIFACTS, true);
        }

        public void setPublishBuildInfo(Boolean enabled) {
            setBooleanValue(PUBLISH_BUILD_INFO, enabled);
        }

        public Boolean isPublishBuildInfo() {
            return getBooleanValue(PUBLISH_BUILD_INFO, true);
        }

        public void setPublishForkCount(int value) {
            setIntegerValue(PUBLISH_FORK_COUNT, value);
        }

        public Integer getPublishForkCount() {
            return getIntegerValue(PUBLISH_FORK_COUNT, DEFAULT_DEPLOYMENT_THREADS);
        }

        public boolean isRecordAllDependencies() {
            return getBooleanValue(RECORD_ALL_DEPENDENCIES, false);
        }

        public void setRecordAllDependencies(Boolean enabled) {
            setBooleanValue(RECORD_ALL_DEPENDENCIES, enabled);
        }

        public String getIncludePatterns() {
            return getStringValue(INCLUDE_PATTERNS);
        }

        public void setIncludePatterns(String patterns) {
            setStringValue(INCLUDE_PATTERNS, patterns);
        }

        public boolean isFilterExcludedArtifactsFromBuild() {
            return getBooleanValue(FILTER_EXCLUDED_ARTIFACTS_FROM_BUILD, false);
        }

        public void setFilterExcludedArtifactsFromBuild(boolean excludeArtifactsFromBuild) {
            setBooleanValue(FILTER_EXCLUDED_ARTIFACTS_FROM_BUILD, excludeArtifactsFromBuild);
        }

        public String getExcludePatterns() {
            return getStringValue(EXCLUDE_PATTERNS);
        }

        public void setExcludePatterns(String patterns) {
            setStringValue(EXCLUDE_PATTERNS, patterns);
        }

        public void setEvenUnstable(Boolean enabled) {
            setBooleanValue(EVEN_UNSTABLE, enabled);
        }

        public Boolean isEvenUnstable() {
            return getBooleanValue(EVEN_UNSTABLE, false);
        }

        public String getBuildRoot() {
            return getMatrixParams().get(BUILD_ROOT);
        }

        public void setBuildRoot(String buildRoot) {
            addMatrixParam(BUILD_ROOT, buildRoot);
        }

        @Override
        public String getMatrixParamPrefix() {
            return PROP_DEPLOY_PARAM_PROP_PREFIX;
        }

        @Override
        public String getDeprecatedMatrixParamPrefix() {
            return DEPRECATED_PROP_DEPLOY_PARAM_PROP_PREFIX;
        }

        public ArtifactSpecs getArtifactSpecs() {
            String specs = getStringValue(ARTIFACT_SPECS);
            return new ArtifactSpecs(specs);
        }

        public void setArtifactSpecs(String artifactSpecs) {
            setStringValue(ARTIFACT_SPECS, artifactSpecs);
        }

        public String getPublications() {
            return getStringValue(PUBLICATIONS);
        }

        public void setPublications(String publications) {
            setStringValue(PUBLICATIONS, publications);
        }

        public int getMinChecksumDeploySizeKb() {
            return getIntegerValue(MIN_CHECKSUM_DEPLOY_SIZE_KB, DEFAULT_MIN_CHECKSUM_DEPLOY_SIZE_KB);
        }

        public void setMinChecksumDeploySizeKb(int minChecksumDeploySizeKb) {
            setIntegerValue(MIN_CHECKSUM_DEPLOY_SIZE_KB, minChecksumDeploySizeKb);
        }
    }

    public class HttpsProxyHandler extends ProxyHandler {
        public HttpsProxyHandler() {
            super(PROP_HTTPS_PROXY_PREFIX);
        }

        @Override
        public String getPassword() {
            return System.getenv(PROP_HTTPS_PROXY_PREFIX + PASSWORD);
        }
    }

    public class ProxyHandler extends AuthenticationConfiguration {
        public ProxyHandler() {
            super(PROP_PROXY_PREFIX);
        }

        public ProxyHandler(String prefix) {
            super(prefix);
        }

        // TODO: Support proxy type SSL or not

        public String getHost() {
            return getStringValue(HOST);
        }

        public void setHost(String host) {
            setStringValue(HOST, host);
        }

        public Integer getPort() {
            return getIntegerValue(PORT, 0);
        }

        public void setPort(Integer port) {
            setIntegerValue(PORT, port);
        }

        public String getNoProxy() {
            return getStringValue(NO_PROXY);
        }

        @Override
        public String getPassword() {
            String password = System.getenv(PROP_PROXY_PREFIX + PASSWORD);
            if (StringUtils.isNotBlank(password)) {
                return password;
            }
            // Backward compatibility.
            return super.getPassword();
        }

        public void setNoProxy(String noProxy) {
            setStringValue(NO_PROXY, noProxy);
        }
    }

    // Used by other build tools (npm, pip...).
    public class PackageManagerHandler extends PrefixPropertyHandler {

        public PackageManagerHandler() {
            super(root, PROP_PACKAGE_MANAGER_PREFIX);
        }

        public String getArgs() {
            return rootConfig.getStringValue(PACKAGE_MANAGER_ARGS);
        }

        public void setArgs(String packageManagerArgs) {
            rootConfig.setStringValue(PACKAGE_MANAGER_ARGS, packageManagerArgs);
        }

        public String getPath() {
            return rootConfig.getStringValue(PACKAGE_MANAGER_PATH);
        }

        public void setPath(String packageManagerPath) {
            rootConfig.setStringValue(PACKAGE_MANAGER_PATH, packageManagerPath);
        }

        public String getModule() {
            return rootConfig.getStringValue(PACKAGE_MANAGER_MODULE);
        }

        public void setModule(String packageManagerModule) {
            rootConfig.setStringValue(PACKAGE_MANAGER_MODULE, packageManagerModule);
        }
    }

    public class NpmHandler extends PrefixPropertyHandler {
        public NpmHandler() {
            super(root, PROP_NPM_PREFIX);
        }

        public boolean isCiCommand() {
            return rootConfig.getBooleanValue(NPM_CI_COMMAND, false);
        }

        public void setCiCommand(boolean ciCommand) {
            rootConfig.setBooleanValue(NPM_CI_COMMAND, ciCommand);
        }
    }

    public class GoHandler extends PrefixPropertyHandler {
        public GoHandler() {
            super(root, PROP_GO_PREFIX);
        }

        public String getGoPublishedVersion() {
            return rootConfig.getStringValue(GO_PUBLISHED_VERSION);
        }

        public void setGoPublishedVersion(String version) {
            rootConfig.setStringValue(GO_PUBLISHED_VERSION, version);
        }
    }

    public class PipHandler extends PrefixPropertyHandler {
        public PipHandler() {
            super(root, PROP_PIP_PREFIX);
        }

        public String getEnvActivation() {
            return rootConfig.getStringValue(PIP_ENV_ACTIVATION);
        }

        public void setEnvActivation(String envActivation) {
            rootConfig.setStringValue(PIP_ENV_ACTIVATION, envActivation);
        }
    }

    public class DotnetHandler extends PrefixPropertyHandler {
        public DotnetHandler() {
            super(root, PROP_DOTNET_PREFIX);
        }

        public boolean useDotnetCoreCli() {
            return rootConfig.getBooleanValue(DOTNET_USE_DOTNET_CORE_CLI, false);
        }

        public void setUseDotnetCli(boolean useDotnetCli) {
            rootConfig.setBooleanValue(DOTNET_USE_DOTNET_CORE_CLI, useDotnetCli);
        }

        public String apiProtocol() {
            return rootConfig.getStringValue(DOTNET_NUGET_PROTOCOL, DEFAULT_NUGET_PROTOCOL);
        }

        public void setApiProtocol(String apiProtocol) {
            rootConfig.setStringValue(DOTNET_NUGET_PROTOCOL, apiProtocol);
        }
    }

    public class DockerHandler extends PrefixPropertyHandler {
        public DockerHandler() {
            super(root, PROP_DOCKER_PREFIX);
        }

        public String getImageTag() {
            return rootConfig.getStringValue(DOCKER_IMAGE_TAG);
        }

        public void setImageTag(String imageTag) {
            rootConfig.setStringValue(DOCKER_IMAGE_TAG, imageTag);
        }

        public String getHost() {
            return rootConfig.getStringValue(DOCKER_HOST);
        }

        public void setHost(String host) {
            rootConfig.setStringValue(DOCKER_HOST, host);
        }

        public String getKanikoImageFile() {
            return rootConfig.getStringValue(KANIKO_IMAGE_FILE);
        }

        public void setKanikoImageFile(String kanikoImageFile) {
            rootConfig.setStringValue(KANIKO_IMAGE_FILE, kanikoImageFile);
        }

        public String getJibImageFile() {
            return rootConfig.getStringValue(JIB_IMAGE_FILE);
        }

        public void setJibImageFile(String jibImageFile) {
            rootConfig.setStringValue(JIB_IMAGE_FILE, jibImageFile);
        }
    }

    public class AuthenticationConfiguration extends PrefixPropertyHandler {
        public AuthenticationConfiguration(String prefix) {
            super(root, prefix);
        }

        public String getUsername() {
            return getStringValue(USERNAME);
        }

        public void setUsername(String userName) {
            setStringValue(USERNAME, userName);
        }

        public String getPassword() {
            return getStringValue(PASSWORD);
        }

        public void setPassword(String password) {
            setStringValue(PASSWORD, password);
        }
    }

    public abstract class RepositoryConfiguration extends AuthenticationConfiguration {

        private ImmutableMap<String, String> calculatedMatrixParams;

        protected RepositoryConfiguration(String prefix) {
            super(prefix);
        }

        public String getName() {
            return getStringValue(NAME);
        }

        public void setName(String name) {
            setStringValue(NAME, name);
        }

        public String urlWithMatrixParams(String rootUrl) {
            if (rootUrl == null) {
                return null;
            }
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

        public void setUrl(String url) {
            setStringValue(URL, url);
        }

        public String getUrl(String repo) {
            String value = getStringValue(URL);
            if (StringUtils.isBlank(value)) {
                if (StringUtils.isNotBlank(repo)) {
                    String contextUrl = getContextUrl();
                    if (StringUtils.isNotBlank(contextUrl)) {
                        contextUrl = StringUtils.stripEnd(contextUrl, "/ ");
                        return contextUrl + "/" + repo;
                    }
                }
            }
            return StringUtils.removeEnd(value, "/");
        }

        public String getRepoKey() {
            return getStringValue(REPO_KEY);
        }

        public void setRepoKey(String repoKey) {
            setStringValue(REPO_KEY, repoKey);
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
            return getBooleanValue(MAVEN, null);
        }

        public void setIvy(Boolean enabled) {
            setBooleanValue(IVY, enabled);
        }

        public Boolean isIvy() {
            return getBooleanValue(IVY, null);
        }

        public boolean isM2Compatible() {
            return getBooleanValue(IVY_M2_COMPATIBLE, true);
        }

        public void setM2Compatible(Boolean enabled) {
            setBooleanValue(IVY_M2_COMPATIBLE, enabled);
        }

        public String getIvyArtifactPattern() {
            String value = getStringValue(IVY_ART_PATTERN);
            if (StringUtils.isBlank(value)) {
                return LayoutPatterns.M2_PATTERN;
            }
            return value.trim();
        }

        public void setIvyArtifactPattern(String artPattern) {
            setStringValue(IVY_ART_PATTERN, artPattern);
        }

        public String getIvyPattern() {
            String value = getStringValue(IVY_IVY_PATTERN);
            if (StringUtils.isBlank(value)) {
                return LayoutPatterns.DEFAULT_IVY_PATTERN;
            }
            return value.trim();
        }

        public void setIvyPattern(String ivyPattern) {
            setStringValue(IVY_IVY_PATTERN, ivyPattern);
        }

        public abstract String getMatrixParamPrefix();

        public abstract String getDeprecatedMatrixParamPrefix();

        public abstract String getContextUrl();

        public void setMatrixParam(String key, String value) {
            putMatrixParam(key, value, true);
        }

        public void addMatrixParam(String key, String value) {
            putMatrixParam(key, value, false);
        }

        private void putMatrixParam(String key, String value, boolean override) {
            if (StringUtils.isBlank(key) || StringUtils.isBlank(value)) {
                return;
            }
            String matrixParamPrefix = getMatrixParamPrefix();
            if (!key.startsWith(matrixParamPrefix)) {
                key = matrixParamPrefix + key;
            }
            if (!override && props.get(key) != null) {
                value = props.get(key) + "," + value;
            }
            props.put(key, value);
        }

        public String getMatrixParam(String key) {
            if (StringUtils.isBlank(key)) {
                return null;
            }
            String matrixParamPrefix = getMatrixParamPrefix();
            if (!key.startsWith(matrixParamPrefix)) {
                key = matrixParamPrefix + key;
            }
            return props.get(key);
        }

        // INTERNAL METHOD
        public void addMatrixParams(Map<String, String> vars) {
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                addMatrixParam(entry.getKey(), entry.getValue());
            }
        }

        public void addMatrixParams(ArrayListMultimap<String, String> vars) {
            for (Map.Entry<String, String> entry : vars.entries()) {
                addMatrixParam(entry.getKey(), entry.getValue());
            }
        }

        public ImmutableMap<String, String> getMatrixParams() {
            if (calculatedMatrixParams != null) {
                return calculatedMatrixParams;
            }
            // First, get value using deprecated key.
            // This check must be first, otherwise, build.gradle properties will override the CI (e.g Jenkins / teamcity) properties.
            Map<String, String> result = getResolveMatrixParams(getDeprecatedMatrixParamPrefix());
            if (result.size() == 0) {
                // Fallback to none deprecated key.
                result = getResolveMatrixParams(getMatrixParamPrefix());
            }
            this.calculatedMatrixParams = ImmutableMap.copyOf(result);
            return calculatedMatrixParams;
        }

        private Map<String, String> getResolveMatrixParams(String matrixPrefix) {
            Map<String, String> result = new HashMap<>();
            for (Map.Entry<String, String> entry : props.entrySet()) {
                if (entry.getKey().startsWith(matrixPrefix)) {
                    result.put(entry.getKey().substring(matrixPrefix.length()), entry.getValue());
                }
            }
            return result;
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
            return getBooleanValue(AGGREGATE_BUILD_ISSUES, Boolean.FALSE);
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

    public class BuildInfoHandler extends PrefixPropertyHandler {
        public final IssuesTrackerHandler issues = new IssuesTrackerHandler();

        private final Predicate<String> buildVariablesPredicate;
        private final Predicate<String> buildRunParametersPredicate;

        public BuildInfoHandler() {
            super(root, BUILD_INFO_PREFIX);
            buildVariablesPredicate = input -> input.startsWith(BUILD_INFO_PREFIX + ENVIRONMENT_PREFIX);
            buildRunParametersPredicate = input -> input.startsWith(BUILD_INFO_PREFIX + RUN_PARAMETERS);
        }

        public String getProject() {
            return getStringValue(BUILD_PROJECT);
        }

        public void setProject(String project) {
            setStringValue(BUILD_PROJECT, project);
        }

        public String getBuildName() {
            return getStringValue(BUILD_NAME);
        }

        public void setBuildName(String buildName) {
            setStringValue(BUILD_NAME, buildName);
        }

        public String getBuildNumber() {
            return getStringValue(BUILD_NUMBER);
        }

        public void setBuildNumber(String buildNumber) {
            setStringValue(BUILD_NUMBER, buildNumber);
        }

        public String getBuildTimestamp() {
            return getStringValue(BUILD_TIMESTAMP);
        }

        public void setBuildTimestamp(String timestamp) {
            setStringValue(BUILD_TIMESTAMP, timestamp);
        }

        public void setBuildStarted(String isoStarted) {
            setStringValue(BUILD_STARTED, isoStarted);
        }

        public String getBuildStarted() {
            return getStringValue(BUILD_STARTED);
        }

        public void setBuildStarted(long timestamp) {
            setBuildStarted(BuildInfo.formatBuildStarted(timestamp));
        }

        public String getPrincipal() {
            return getStringValue(PRINCIPAL);
        }

        public void setPrincipal(String principal) {
            setStringValue(PRINCIPAL, principal);
        }

        public String getArtifactoryPluginVersion() {
            return getStringValue(ARTIFACTORY_PLUGIN_VERSION);
        }

        public void setArtifactoryPluginVersion(String artifactoryPluginVersion) {
            setStringValue(ARTIFACTORY_PLUGIN_VERSION, artifactoryPluginVersion);
        }

        public String getBuildUrl() {
            return getStringValue(BUILD_URL);
        }

        public void setBuildUrl(String buildUrl) {
            setStringValue(BUILD_URL, buildUrl);
        }

        public String getVcsRevision() {
            return getStringValue(VCS_REVISION);
        }

        public void setVcsRevision(String vcsRevision) {
            setStringValue(VCS_REVISION, vcsRevision);
        }

        public String getVcsUrl() {
            return getStringValue(VCS_URL);
        }

        public void setVcsUrl(String vcsUrl) {
            setStringValue(VCS_URL, vcsUrl);
        }

        public String getVcsBranch() {
            return getStringValue(VCS_BRANCH);
        }

        public void setVcsBranch(String vcsBranch) {
            setStringValue(VCS_BRANCH, vcsBranch);
        }

        public String getVcsMessage() {
            return getStringValue(VCS_MESSAGE);
        }

        public void setVcsMessage(String vcsMessage) {
            setStringValue(VCS_MESSAGE, vcsMessage);
        }

        public String getAgentName() {
            return getStringValue(AGENT_NAME);
        }

        public void setAgentName(String agentName) {
            setStringValue(AGENT_NAME, agentName);
        }

        public String getAgentVersion() {
            return getStringValue(AGENT_VERSION);
        }

        public void setAgentVersion(String agentVersion) {
            setStringValue(AGENT_VERSION, agentVersion);
        }

        public String getBuildAgentName() {
            return getStringValue(BUILD_AGENT_NAME);
        }

        public void setBuildAgentName(String buildAgentName) {
            setStringValue(BUILD_AGENT_NAME, buildAgentName);
        }

        public String getBuildAgentVersion() {
            return getStringValue(BUILD_AGENT_VERSION);
        }

        public void setBuildAgentVersion(String buildAgentVersion) {
            setStringValue(BUILD_AGENT_VERSION, buildAgentVersion);
        }

        public String getParentBuildName() {
            return getStringValue(BUILD_PARENT_NAME);
        }

        public void setParentBuildName(String parentBuildName) {
            setStringValue(BUILD_PARENT_NAME, parentBuildName);
        }

        public String getParentBuildNumber() {
            return getStringValue(BUILD_PARENT_NUMBER);
        }

        public void setParentBuildNumber(String parentBuildNumber) {
            setStringValue(BUILD_PARENT_NUMBER, parentBuildNumber);
        }

        public void setDeleteBuildArtifacts(Boolean deleteBuildArtifacts) {
            setBooleanValue(DELETE_BUILD_ARTIFACTS, deleteBuildArtifacts);
        }

        public Boolean isDeleteBuildArtifacts() {
            return getBooleanValue(DELETE_BUILD_ARTIFACTS, true);
        }

        public void setAsyncBuildRetention(Boolean asyncBuildRetention) {
            setBooleanValue(BUILD_RETENTION_ASYNC, asyncBuildRetention);
        }

        public Boolean isAsyncBuildRetention() {
            return getBooleanValue(BUILD_RETENTION_ASYNC, false);
        }

        public void setBuildRetentionMaxDays(Integer daysToKeep) {
            setBuildRetentionDays(daysToKeep);
        }

        public Integer getBuildRetentionDays() {
            return getIntegerValue(BUILD_RETENTION_DAYS);
        }

        public void setBuildRetentionDays(Integer daysToKeep) {
            setIntegerValue(BUILD_RETENTION_DAYS, daysToKeep);
        }

        public Integer getBuildRetentionCount() {
            return getIntegerValue(BUILD_RETENTION_COUNT);
        }

        public void setBuildRetentionCount(Integer buildsToKeep) {
            setIntegerValue(BUILD_RETENTION_COUNT, buildsToKeep);
        }

        public String getBuildRetentionMinimumDate() {
            return getStringValue(BUILD_RETENTION_MINIMUM_DATE);
        }

        public void setBuildRetentionMinimumDate(String date) {
            setStringValue(BUILD_RETENTION_MINIMUM_DATE, date);
        }

        public String[] getBuildNumbersNotToDelete() {
            String value = getStringValue(BUILD_NUMBERS_NOT_TO_DELETE);
            if (StringUtils.isNotBlank(value)) {
                return StringUtils.split(value, ",");
            }
            return new String[0];
        }

        public void setBuildNumbersNotToDelete(String buildNumbersNotToDelete) {
            setStringValue(BUILD_NUMBERS_NOT_TO_DELETE, buildNumbersNotToDelete);
        }

        public String getReleaseComment() {
            return getStringValue(RELEASE_COMMENT);
        }

        public void setReleaseComment(String comment) {
            setStringValue(RELEASE_COMMENT, comment);
        }

        public void setReleaseEnabled(Boolean enabled) {
            setBooleanValue(RELEASE_ENABLED, enabled);
        }

        public Boolean isReleaseEnabled() {
            return getBooleanValue(RELEASE_ENABLED, false);
        }

        public String getBuildRoot() {
            return getStringValue(BUILD_ROOT);
        }

        public void setBuildRoot(String buildRoot) throws UnsupportedEncodingException {
            publisher.setBuildRoot(buildRoot);
            resolver.setBuildRoot(URLEncoder.encode(buildRoot, "UTF-8"));
            setStringValue(BUILD_ROOT, buildRoot);
        }

        public void setGeneratedBuildInfoFilePath(String generatedBuildInfo) {
            setStringValue(GENERATED_BUILD_INFO, generatedBuildInfo);
        }

        public String getGeneratedBuildInfoFilePath() {
            return getStringValue(GENERATED_BUILD_INFO);
        }

        public void setDeployableArtifactsFilePath(String deployableArtifacts) {
            setStringValue(DEPLOYABLE_ARTIFACTS, deployableArtifacts);
        }

        @Deprecated
        public void setBackwardCompatibleDeployableArtifactsFilePath(String deployableArtifacts) {
            setStringValue(BACKWARD_COMPATIBLE_DEPLOYABLE_ARTIFACTS, deployableArtifacts);
        }

        public String getDeployableArtifactsFilePath() {
            String path = getStringValue(DEPLOYABLE_ARTIFACTS);
            return StringUtils.isNotEmpty(path) ? path : getStringValue(BACKWARD_COMPATIBLE_DEPLOYABLE_ARTIFACTS);
        }

        public boolean isBackwardCompatibleDeployableArtifacts() {
            return StringUtils.isEmpty(getStringValue(DEPLOYABLE_ARTIFACTS));
        }

        public void addBuildVariables(Map<String, String> buildVariables, IncludeExcludePatterns patterns) {
            for (Map.Entry<String, String> entry : buildVariables.entrySet()) {
                String varKey = entry.getKey();
                if (PatternMatcher.pathConflicts(varKey, patterns)) {
                    continue;
                }
                addEnvironmentProperty(varKey, entry.getValue());
            }
        }

        public void addEnvironmentProperty(String key, String value) {
            setStringValue(ENVIRONMENT_PREFIX + key, value);
        }

        /*
         * Use for Multi-configuration/Matrix builds
         */
        public void addRunParameters(String key, String value) {
            setStringValue(RUN_PARAMETERS + key, value);
        }

        public Map<String, String> getRunParameters() {
            Map<String, String> tempMap = CommonUtils.filterMapKeys(props, buildRunParametersPredicate);
            Map<String, String> runParameters = new HashMap<>();
            for (Map.Entry<String, String> param : tempMap.entrySet()) {
                runParameters.put(param.getKey().replace(BUILD_INFO_PREFIX + RUN_PARAMETERS, StringUtils.EMPTY),
                        param.getValue());
            }

            return runParameters;
        }

        public Boolean isIncremental() {
            return getBooleanValue(INCREMENTAL, Boolean.FALSE);
        }

        public void setIncremental(Boolean incremental) {
            setBooleanValue(INCREMENTAL, incremental);
        }
    }

    /**
     * Add the default publisher attributes to ArtifactoryClientConfiguration.
     *
     * @param config              - Global configuration of the current build.
     * @param defaultProjectName  - Default project.
     * @param defaultAgentName    - Default agent name.
     * @param defaultAgentVersion - Default agent version.
     */
    public static void addDefaultPublisherAttributes(ArtifactoryClientConfiguration config, String defaultProjectName, String defaultAgentName, String defaultAgentVersion) {
        // Build name
        String buildName = config.info.getBuildName();
        if (StringUtils.isBlank(buildName)) {
            buildName = defaultProjectName;
            config.info.setBuildName(buildName);
        }
        config.publisher.setMatrixParam(BuildInfoFields.BUILD_NAME, buildName);

        // Build number
        String buildNumber = config.info.getBuildNumber();
        if (StringUtils.isBlank(buildNumber)) {
            buildNumber = new Date().getTime() + "";
            config.info.setBuildNumber(buildNumber);
        }
        config.publisher.setMatrixParam(BuildInfoFields.BUILD_NUMBER, buildNumber);

        // Build start (was set by the plugin - no need to make up a fallback val)
        String buildTimestamp = config.info.getBuildTimestamp();
        if (StringUtils.isBlank(buildTimestamp)) {
            String buildStartedIso = config.info.getBuildStarted();
            Date buildStartDate;
            try {
                buildStartDate = new SimpleDateFormat(BuildInfo.STARTED_FORMAT).parse(buildStartedIso);
            } catch (ParseException e) {
                throw new RuntimeException("Build start date format error: " + buildStartedIso, e);
            }
            buildTimestamp = String.valueOf(buildStartDate.getTime());
            config.info.setBuildTimestamp(buildTimestamp);
        }
        config.publisher.setMatrixParam(BuildInfoFields.BUILD_TIMESTAMP, buildTimestamp);

        // Build agent
        String buildAgentName = config.info.getBuildAgentName();
        String buildAgentVersion = config.info.getBuildAgentVersion();
        if (StringUtils.isBlank(buildAgentName) && StringUtils.isBlank(buildAgentVersion)) {
            config.info.setBuildAgentName(defaultAgentName);
            config.info.setBuildAgentVersion(defaultAgentVersion);
        }
    }
}
