package org.jfrog.build.extractor.maven.resolver;

import org.codehaus.plexus.util.StringUtils;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.maven.Maven3BuildInfoLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Properties;

/**
 * Created by liorh on 4/24/14.
 */
@Singleton
@Named
public class ResolutionHelper {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ArtifactoryClientConfiguration internalConfiguration;
    private boolean initialized = false;

    public void init(Properties allMavenProps) {
        if (internalConfiguration != null) {
            return;
        }

        Maven3BuildInfoLogger log = new Maven3BuildInfoLogger(logger);
        Properties allProps = BuildInfoExtractorUtils.mergePropertiesWithSystemAndPropertyFile(allMavenProps, log);
        internalConfiguration = new ArtifactoryClientConfiguration(log);
        internalConfiguration.fillFromProperties(allProps);
        initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Determines a deployed artifact's scope (either "project" or "build") according to the maven's request context sent as an argument.
     * @param requestContext    The deployed artifact's request context.
     * @return                  Scope value for the request context.
     */
    public String getScopeByRequestContext(String requestContext) {
        if (requestContext == null) {
            return "project";
        }
        if ("plugin".equals(requestContext)) {
            return "build";
        }
        return "project";
    }

    public String getRepoReleaseUrl() {
        return internalConfiguration.resolver.getUrl(internalConfiguration.resolver.getRepoKey());
    }

    public String getRepoSnapshotUrl() {
        return internalConfiguration.resolver.getUrl(internalConfiguration.resolver.getDownloadSnapshotRepoKey());
    }

    /**
     * Checks whether Artifactory resolution repositories have been configured.
     * @return  true if at least one Artifactory resolution repository has been configured.
     */
    public boolean resolutionRepositoriesConfigured() {
        return StringUtils.isNotBlank(getRepoReleaseUrl()) || StringUtils.isNotBlank(getRepoSnapshotUrl());
    }

    public String getRepoUsername() {
        return internalConfiguration.resolver.getUsername();
    }

    public String getRepoPassword() {
        return internalConfiguration.resolver.getPassword();
    }

    public String getHttpProxyHost() {
        return internalConfiguration.proxy.getHost();
    }

    public String getNoProxy() {
        return internalConfiguration.proxy.getNoProxy();
    }

    public Integer getHttpProxyPort() {
        return internalConfiguration.proxy.getPort();
    }

    public String getHttpProxyUsername() {
        return internalConfiguration.proxy.getUsername();
    }

    public String getHttpProxyPassword() {
        return internalConfiguration.proxy.getPassword();
    }

    public String getHttpsProxyHost() {
        return internalConfiguration.httpsProxy.getHost();
    }

    public Integer getHttpsProxyPort() {
        return internalConfiguration.httpsProxy.getPort();
    }

    public String getHttpsProxyUsername() {
        return internalConfiguration.httpsProxy.getUsername();
    }

    public String getHttpsProxyPassword() {
        return internalConfiguration.httpsProxy.getPassword();
    }

    public boolean isSnapshotDisabled() { return internalConfiguration.resolver.isSnapshotDisabled(); }

    public String getSnapshotUpdatePolicy() { return internalConfiguration.resolver.getSnapshotUpdatePolicy(); }

    public Logger getLogger() {
        return logger;
    }
}
