package org.jfrog.build.extractor.maven.resolver;

import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.logging.Logger;
import org.jfrog.build.client.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.maven.Maven3BuildInfoLogger;
import org.sonatype.aether.metadata.Metadata;

import java.util.Properties;

/**
 * Created by liorh on 4/24/14.
 */

//@Component(role = ResolutionHelper.class)
public class ResolutionHelper {

    //  @Requirement
    // private Logger logger;

    private ArtifactoryClientConfiguration internalConfiguration;

    // public ResolutionHelper(){}

    public void resolve(Properties allMavenProps, Logger logger) {
        if (internalConfiguration != null) {
            return;
        }

        Properties allProps = BuildInfoExtractorUtils.mergePropertiesWithSystemAndPropertyFile(allMavenProps);
        internalConfiguration = new ArtifactoryClientConfiguration(new Maven3BuildInfoLogger(logger));
        internalConfiguration.fillFromProperties(allProps);
    }

    /*For aether event*/
    public String getEnforceRepository(org.sonatype.aether.RepositoryEvent event) {

        /*Check if we are downloading metadata or artifact*/
        if (event.getArtifact() == null) {
            boolean isSnapshot = event.getMetadata().getNature().compareTo(Metadata.Nature.SNAPSHOT) == 0;
            if (isSnapshot && StringUtils.isNotBlank(getRepoSnapshotUrl()))
                return getRepoSnapshotUrl();
            else if (StringUtils.isNotBlank(getRepoReleaseUrl()))
                return getRepoReleaseUrl();
        }

        /*Check if we are downloading metadata or artifact*/
        if (event.getMetadata() == null) {
            org.sonatype.aether.artifact.Artifact currentArtifact = event.getArtifact();
            if (currentArtifact.isSnapshot() && StringUtils.isNotBlank(getRepoSnapshotUrl())) {
                return getRepoSnapshotUrl();
            } else if (StringUtils.isNotBlank(getRepoReleaseUrl())) {
                return getRepoReleaseUrl();
            }
        }

        return null;
    }

    /*For eclipse event*/
    public String getEnforceRepository(org.eclipse.aether.RepositoryEvent event) {
        /*Check if we are downloading metadata or artifact*/
        if (event.getArtifact() == null) {
            boolean isSnapshot = event.getMetadata().getNature().compareTo(org.eclipse.aether.metadata.Metadata.Nature.SNAPSHOT) == 0;
            if (isSnapshot && StringUtils.isNotBlank(getRepoSnapshotUrl()))
                return getRepoSnapshotUrl();
            else if (StringUtils.isNotBlank(getRepoReleaseUrl()))
                return getRepoReleaseUrl();
        }

        /*Check if we are downloading metadata or artifact*/
        if (event.getMetadata() == null) {
            org.eclipse.aether.artifact.Artifact currentArtifact = event.getArtifact();
            if (currentArtifact.isSnapshot() && StringUtils.isNotBlank(getRepoSnapshotUrl())) {
                return getRepoSnapshotUrl();
            } else if (StringUtils.isNotBlank(getRepoReleaseUrl())) {
                return getRepoReleaseUrl();
            }
        }

        return null;
    }

    public String getRepoSnapshotUrl() {
        return internalConfiguration.resolver.getUrl(internalConfiguration.resolver.getRepoKey());
    }

    public String getRepoReleaseUrl() {
        return internalConfiguration.resolver.getUrl(internalConfiguration.resolver.getRepoReleaseKey());
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
}
