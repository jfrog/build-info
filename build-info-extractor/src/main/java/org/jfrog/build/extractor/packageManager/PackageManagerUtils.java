package org.jfrog.build.extractor.packageManager;

import org.apache.http.client.utils.URIBuilder;
import org.jfrog.build.api.ci.BuildInfo;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
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
                .setPath(rtUrl.getPath() + path);
        return proxyUrlBuilder.build().toURL().toString();
    }

    /**
     * Collect environment variables according to the env include-exclude patterns.
     *
     * @param clientConfiguration - Artifactory client configuration
     * @param buildInfo           - The target buildInfo
     */
    public static void collectEnvIfNeeded(ArtifactoryClientConfiguration clientConfiguration, BuildInfo buildInfo) {
        if (!clientConfiguration.isIncludeEnvVars()) {
            return;
        }
        // Create initial environment variables properties
        Properties envProperties = new Properties();
        envProperties.putAll(clientConfiguration.getAllProperties());

        // Filter env according to the include-exclude patterns
        envProperties = BuildInfoExtractorUtils.getEnvProperties(envProperties, clientConfiguration.getLog());

        // Add results to the buildInfo
        if (buildInfo.getProperties() != null) {
            buildInfo.getProperties().putAll(envProperties);
            return;
        }
        buildInfo.setProperties(envProperties);
    }
}
