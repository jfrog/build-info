package org.jfrog.build.extractor.packageManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.extractor.ci.Module;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.IncludeExcludePatterns;
import org.jfrog.build.extractor.clientConfiguration.PatternMatcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by Bar Belity on 12/07/2020.
 */
public class PackageManagerUtils {
    private static final String apiKeySecretPrefix = "AKCp8";
    private static final int apiKeySecretMinimalLength = 73;
    private static final String referenceTokenSecretPrefix = "cmVmdGtuOjAxOj";
    private static final int referenceTokenSecretMinimalLength = 64;
    private static final String accessTokenSecretPrefix = "eyJ2ZXIiOiIyIiwidHlwIjoiSldUIiwiYWxnIjoiUlMyNTYiLCJraWQiOiJ";
    private static final int accessTokenSecretMinimalLength = 0;

    /**
     * Create a new client configuration from the 'buildInfoConfig.propertiesFile' and environment variables.
     *
     * @return a new client configuration
     */
    public static ArtifactoryClientConfiguration createArtifactoryClientConfiguration() {
        PackageManagerLogger log = new PackageManagerLogger();
        ArtifactoryClientConfiguration clientConfiguration = new ArtifactoryClientConfiguration(log);
        Properties allBuildProps = new Properties();
        allBuildProps.putAll(System.getenv());
        Properties allProps = BuildInfoExtractorUtils.mergePropertiesWithSystemAndPropertyFile(allBuildProps, log);
        clientConfiguration.fillFromProperties(allProps);
        return clientConfiguration;
    }

    public static String createArtifactoryUrlWithCredentials(String url, String username, String password, String path) throws MalformedURLException, URISyntaxException {
        URL rtUrl = new URL(url);
        URIBuilder proxyUrlBuilder = new URIBuilder()
                .setScheme(rtUrl.getProtocol())
                .setUserInfo(username, password)
                .setHost(rtUrl.getHost())
                .setPort(rtUrl.getPort())
                .setPath(rtUrl.getPath() + path);
        return proxyUrlBuilder.build().toURL().toString();
    }

    /**
     * Collect environment variables according to the env include-exclude patterns.
     *
     * @param clientConfiguration - Artifactory client configuration
     * @param buildInfo           - The target build-info
     */
    public static void collectEnvIfNeeded(ArtifactoryClientConfiguration clientConfiguration, BuildInfo buildInfo) {
        if (!clientConfiguration.isIncludeEnvVars()) {
            return;
        }
        // Create initial environment variables properties
        Properties envProperties = new Properties();
        envProperties.putAll(clientConfiguration.getAllProperties());

        envProperties = BuildInfoExtractorUtils.getEnvProperties(envProperties, clientConfiguration.getLog());

        // Add results to the buildInfo
        if (buildInfo.getProperties() != null) {
            buildInfo.getProperties().putAll(envProperties);
            return;
        }
        buildInfo.setProperties(envProperties);
    }

    public static void filterBuildInfoProperties(ArtifactoryClientConfiguration clientConfiguration, BuildInfo buildInfo, Log log) {
        String include = clientConfiguration.getEnvVarsIncludePatterns();
        String exclude = clientConfiguration.getEnvVarsExcludePatterns();
        IncludeExcludePatterns includeExcludePatterns = new IncludeExcludePatterns(include, exclude);
        filterExcludeIncludeProperties(includeExcludePatterns, buildInfo, log);
    }

    private static void filterExcludeIncludeProperties(IncludeExcludePatterns includePattern, BuildInfo buildInfo, Log log) {
        // Filter envs/global properties
        Properties props = buildInfo.getProperties();
        if (props != null && props.size() > 0) {
            Properties filteredProps = getExcludeIncludeProperties(includePattern, props, log);
            buildInfo.setProperties(filteredProps);
        }

        // Filter modules properties
        List<Module> modules = buildInfo.getModules();
        if (modules == null || modules.size() == 0) {
            return;
        }
        for (Module module : modules) {
            Properties moduleProps = module.getProperties();
            if (moduleProps != null && moduleProps.size() > 0) {
                module.setProperties(getExcludeIncludeProperties(includePattern, moduleProps, log));
            }
        }
    }


    private static Properties getExcludeIncludeProperties(IncludeExcludePatterns patterns, Properties properties, Log log) {
        Properties props = new Properties();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            if (!isExcludedByKey(patterns, entry) && !containsSuspectedSecrets(entry.getValue().toString())) {
                props.put(entry.getKey(), entry.getValue());
            } else {
                log.debug("[buildinfo] Property '" + entry.getKey() + "' has been excluded'");
            }
        }
        return props;
    }

    private static boolean isExcludedByKey(IncludeExcludePatterns patterns, Map.Entry<Object, Object> entry) {
        return PatternMatcher.pathConflicts(entry.getKey().toString(), patterns);
    }

    public static boolean containsSuspectedSecrets(String value) {
        if (StringUtils.isBlank(value)) {
            return false;
        }
        return containsSuspectedSecret(value, apiKeySecretPrefix, apiKeySecretMinimalLength) ||
                containsSuspectedSecret(value, referenceTokenSecretPrefix, referenceTokenSecretMinimalLength) ||
                containsSuspectedSecret(value, accessTokenSecretPrefix, accessTokenSecretMinimalLength);
    }

    /**
     * Checks whether the value of a variable contains a suspected secret.
     * Done by searching for a known constant prefix of the secret and verifying the length of the substring is sufficient to include the expected length of the secret.
     *
     * @param variableValue       - string to search in
     * @param secretPrefix        - secret constant prefix
     * @param secretMinimalLength - secret minimal expected length
     * @return whether a secret is suspected
     */
    private static boolean containsSuspectedSecret(String variableValue, String secretPrefix, int secretMinimalLength) {
        return variableValue.startsWith(secretPrefix) && variableValue.length() >= secretMinimalLength;
    }
}
