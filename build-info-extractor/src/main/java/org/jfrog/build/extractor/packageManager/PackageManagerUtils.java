package org.jfrog.build.extractor.packageManager;

import org.apache.http.client.utils.URIBuilder;
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

    public static void filterBuildInfoProperties(ArtifactoryClientConfiguration clientConfiguration, BuildInfo buildInfo) {
        String include = clientConfiguration.getEnvVarsIncludePatterns();
        String exclude = clientConfiguration.getEnvVarsExcludePatterns();
        IncludeExcludePatterns includeExcludePatterns = new IncludeExcludePatterns(include, exclude);
        filterExcludeIncludeProperties(includeExcludePatterns, buildInfo);
    }

    private static void filterExcludeIncludeProperties(IncludeExcludePatterns includePattern, BuildInfo buildInfo) {
        // Filter envs/global properties
        Properties props = buildInfo.getProperties();
        if (props != null && props.size() > 0) {
            Properties filteredProps = getExcludeIncludeProperties(includePattern, props);
            buildInfo.setProperties(filteredProps);
        }

        // Filter modules properties
        List<Module> modules = buildInfo.getModules();
        if (modules != null && modules.size() > 0) {
            return;
        }
        for (Module module : modules) {
            Properties moduleProps = module.getProperties();
            if (moduleProps != null && moduleProps.size() > 0) {
                module.setProperties(getExcludeIncludeProperties(includePattern, moduleProps));
            }
        }
    }


    private static Properties getExcludeIncludeProperties(IncludeExcludePatterns patterns, Properties properties) {
        Properties props = new Properties();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            if (!isExcludedByKey(patterns, entry) && !isExcludedByValue(patterns, entry)) {
                props.put(entry.getKey(), entry.getValue());
            }
        }
        return props;
    }

    private static boolean isExcludedByKey(IncludeExcludePatterns patterns, Map.Entry<Object, Object> entry) {
        return PatternMatcher.pathConflicts(entry.getKey().toString(), patterns);
    }

    private static boolean isExcludedByValue(IncludeExcludePatterns patterns, Map.Entry<Object, Object> entry) {
        return PatternMatcher.isPathExcluded(entry.getValue().toString(), patterns);
    }
}
