package org.jfrog.build.extractor.maven.resolver;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.jfrog.build.client.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.maven.Maven3BuildInfoLogger;

import java.util.Properties;

/**
 * Created by liorh on 4/24/14.
 */

@Component(role = ResolutionHelper.class)
public class ResolutionHelper {

    @Requirement
    private Logger logger;

    private ArtifactoryClientConfiguration internalConfiguration;

    // public ResolutionHelper(){}

    public void resolve(Properties allMavenProps, Logger logger) {
        if (internalConfiguration != null) {
            return;
        }

        Maven3BuildInfoLogger log = new Maven3BuildInfoLogger(logger);
        Properties allProps = BuildInfoExtractorUtils.mergePropertiesWithSystemAndPropertyFile(allMavenProps, log);
        internalConfiguration = new ArtifactoryClientConfiguration(log);
        internalConfiguration.fillFromProperties(allProps);
    }

    /*For aether event*/
    public String getEnforceRepository(Nature nature) {
        if (nature.equals(Nature.SNAPSHOT) && getRepoSnapshotUrl() != null && !getRepoSnapshotUrl().equals(""))
            return getRepoSnapshotUrl();
        else if (getRepoReleaseUrl() != null && !getRepoReleaseUrl().equals(""))
            return getRepoReleaseUrl();

        return null;
    }

    public String getRepoReleaseUrl() {
        return internalConfiguration.resolver.getUrl(internalConfiguration.resolver.getRepoKey());
    }

    public String getRepoSnapshotUrl() {
        return internalConfiguration.resolver.getUrl(internalConfiguration.resolver.getDownloadSnapshotRepoKey());
    }

    public String getRepoUsername() {
        return internalConfiguration.resolver.getUsername();
    }

    public String getRepoPassword() {
        return internalConfiguration.resolver.getPassword();
    }

    public String getProxyHost() {
        return internalConfiguration.proxy.getHost();
    }

    public Integer getProxyPort() {
        return internalConfiguration.proxy.getPort();
    }

    public String getProxyUsername() {
        return internalConfiguration.proxy.getUsername();
    }

    public String getProxyPassword() {
        return internalConfiguration.proxy.getPassword();
    }

    public enum Nature {
        /**
         * Refers to release artifacts only.
         */
        RELEASE,

        /**
         * Refers to snapshot artifacts only.
         */
        SNAPSHOT
    }
}
