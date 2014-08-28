package org.jfrog.build.extractor.maven.resolver;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.*;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.jfrog.build.extractor.maven.BuildInfoRecorder;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * Repository listener when running in Maven 3.1.x
 * The listener performs the following:
 * 1. Enforces artifacts resolution from Artifactory.
 * 2. Updates the BuildInfoRecorder with each resolved artifact.
 *
 * @author Shay Yaakov
 */
@Component(role = RepositoryListener.class)
public class ArtifactoryEclipseRepositoryListener extends AbstractRepositoryListener implements Contextualizable {

    @Requirement
    private Logger logger;

    @Requirement
    private ResolutionHelper resolutionHelper;

    BuildInfoRecorder buildInfoRecorder = null;

    private PlexusContainer plexusContainer;

    private BuildInfoRecorder getBuildInfoRecorder() {
        if (buildInfoRecorder == null) {
            try {
                buildInfoRecorder = (BuildInfoRecorder)plexusContainer.lookup(BuildInfoRecorder.class.getName());
            } catch (ComponentLookupException e) {
                logger.error("Failed while trying to fetch BuildInfoRecorder from the container in " + this.getClass().getName(), e);
            }
            if (buildInfoRecorder == null) {
                logger.error("Could not fetch BuildInfoRecorder from the container in " + this.getClass().getName() + ". Artifacts resolution cannot be recorded.");
            } else {
                logger.debug("BuildInfoRecorder fetched from the container in " + this.getClass().getName());
            }
        }
        return buildInfoRecorder;
    }

    /**
     * Intercepts resolved artifacts and updates the BuildInfoRecorder, so that build-info includes all resolved artifacts.
     */
    @Override
    public void artifactResolved(RepositoryEvent event) {
        if (getBuildInfoRecorder() != null) {
            getBuildInfoRecorder().artifactResolved(convert(event.getArtifact()));
        }
        super.artifactResolved(event);
    }

    /**
     * Intercepts downloaded artifacts and enforces their resolution from Artifactory.
     */
    @Override
    public void artifactDownloading(RepositoryEvent event) {
        logger.debug("Intercepted Eclipse repository artifact downloading event: " + event);
        enforceRepository(event);
        super.artifactDownloading(event);
    }

    /**
     * Intercepts downloaded metadata objects and enforces their resolution from Artifactory.
     */
    @Override
    public void metadataDownloading(RepositoryEvent event) {
        logger.debug("Intercepted Eclipse repository metadata downloading event: " + event);
        enforceRepository(event);
        super.metadataDownloading(event);
    }

    private void enforceRepository(RepositoryEvent event) {
        ArtifactRepository repository = event.getRepository();
        if (repository == null) {
            logger.warn("Received null repository, perhaps your maven installation is missing a settings.xml file?");
        }
        if (repository instanceof RemoteRepository) {
            Properties allMavenProps = new Properties();
            allMavenProps.putAll(event.getSession().getSystemProperties());
            allMavenProps.putAll(event.getSession().getUserProperties());

            resolutionHelper.resolve(allMavenProps, logger);

            ResolutionHelper.Nature resolutionType = resolutionType(event);
            String enforcingRepository = resolutionHelper.getEnforceRepository(resolutionType);
            if (StringUtils.isBlank(enforcingRepository)) {
                return;
            }

            logger.debug("Enforcing repository URL: " + enforcingRepository + " for event: " + event);
            RemoteRepository remoteRepository = (RemoteRepository) repository;

            try {
                Field url = RemoteRepository.class.getDeclaredField("url");
                url.setAccessible(true);
                url.set(remoteRepository, enforcingRepository);

                if (StringUtils.isNotBlank(resolutionHelper.getRepoUsername())) {
                    Authentication authentication = new AuthenticationBuilder()
                            .addString("username", resolutionHelper.getRepoUsername()).addSecret("password", resolutionHelper.getRepoPassword()).build();
                    logger.debug("Enforcing repository authentication: " + authentication + " for event: " + event);
                    Field authenticationField = RemoteRepository.class.getDeclaredField("authentication");
                    authenticationField.setAccessible(true);
                    authenticationField.set(remoteRepository, authentication);
                }

                logger.debug("Enforcing snapshot and release policy for event: " + event);

                boolean isSnapshot = resolutionType.equals(ResolutionHelper.Nature.SNAPSHOT);
                RepositoryPolicy releasePolicy = new RepositoryPolicy(!isSnapshot, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN);
                RepositoryPolicy snapshotPolicy = new RepositoryPolicy(isSnapshot, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN);

                Field releasePolicyField = RemoteRepository.class.getDeclaredField("releasePolicy");
                releasePolicyField.setAccessible(true);
                releasePolicyField.set(remoteRepository, releasePolicy);
                Field snapshotPolicyField = RemoteRepository.class.getDeclaredField("snapshotPolicy");
                snapshotPolicyField.setAccessible(true);
                snapshotPolicyField.set(remoteRepository, snapshotPolicy);

                if (StringUtils.isNotBlank(resolutionHelper.getProxyHost())) {
                    Authentication authentication = new AuthenticationBuilder()
                            .addString("username", resolutionHelper.getProxyUsername()).addSecret("password", resolutionHelper.getProxyPassword()).build();
                    Proxy proxy = new Proxy(null, resolutionHelper.getProxyHost(), resolutionHelper.getProxyPort(), authentication);
                    logger.debug("Enforcing repository proxy: " + proxy + " for event: " + event);
                    Field proxyField = RemoteRepository.class.getDeclaredField("proxy");
                    proxyField.setAccessible(true);
                    proxyField.set(remoteRepository, proxy);
                }
            } catch (Exception e) {
                logger.error("Failed enforcing repository: " + e.getMessage(), e);
            }
        }
    }

    public ResolutionHelper.Nature resolutionType(RepositoryEvent event) {
        /*Check if we are downloading metadata or artifact*/
        if (event.getArtifact() == null) {
            boolean isSnapshot = event.getMetadata().getNature().compareTo(Metadata.Nature.SNAPSHOT) == 0;
            if (isSnapshot) {
                return ResolutionHelper.Nature.SNAPSHOT;
            }

            return ResolutionHelper.Nature.RELEASE;
        }

        /*Check if we are downloading metadata or artifact*/
        if (event.getMetadata() == null) {
            Artifact currentArtifact = event.getArtifact();
            if (currentArtifact.isSnapshot()) {
                return ResolutionHelper.Nature.SNAPSHOT;
            }

            return ResolutionHelper.Nature.RELEASE;
        }

        return null;
    }

    /**
     * Converts org.eclipse.aether.artifact.Artifact objects into org.apache.maven.artifact.Artifact objects.
     */
    private org.apache.maven.artifact.Artifact convert(final org.eclipse.aether.artifact.Artifact art) {
        if (art == null) {
            return null;
        }
        return new org.apache.maven.artifact.Artifact() {
            @Override
            public String getGroupId() {
                return art.getGroupId();
            }

            @Override
            public String getArtifactId() {
                return art.getArtifactId();
            }

            @Override
            public String getVersion() {
                return art.getVersion();
            }

            @Override
            public String getClassifier() {
                return art.getClassifier();
            }

            @Override
            public boolean hasClassifier() {
                return !StringUtils.isBlank(getClassifier());
            }

            @Override
            public File getFile() {
                return art.getFile();
            }

            @Override
            public String getType() {
                return art.getExtension();
            }

            @Override
            public String getBaseVersion() {
                return art.getBaseVersion();
            }

            @Override
            public String getId() {
                return art.getArtifactId();
            }

            @Override
            public boolean isSnapshot() {
                return art.isSnapshot();
            }

            @Override
            public boolean isRelease() {
                return !art.isSnapshot();
            }

            // Methods that are not implemented:

            @Override
            public String getDependencyConflictId() {
                return null;
            }

            @Override
            public Collection<ArtifactMetadata> getMetadataList() {
                return null;
            }

            @Override
            public org.apache.maven.artifact.repository.ArtifactRepository getRepository() {
                return null;
            }

            @Override
            public String getDownloadUrl() {
                return null;
            }

            @Override
            public ArtifactFilter getDependencyFilter() {
                return null;
            }

            @Override
            public ArtifactHandler getArtifactHandler() {
                return null;
            }

            @Override
            public List<String> getDependencyTrail() {
                return null;
            }

            @Override
            public VersionRange getVersionRange() {
                return null;
            }

            @Override
            public boolean isResolved() {
                return false;
            }

            @Override
            public List<ArtifactVersion> getAvailableVersions() {
                return null;
            }

            @Override
            public String getScope() {
                return null;
            }

            @Override
            public boolean isOptional() {
                return false;
            }

            @Override
            public ArtifactVersion getSelectedVersion() throws OverConstrainedVersionException {
                return null;
            }

            @Override
            public boolean isSelectedVersionKnown() throws OverConstrainedVersionException {
                return false;
            }

            @Override
            public int compareTo(org.apache.maven.artifact.Artifact o) {
                return 0;
            }

            @Override
            public void setDependencyFilter(ArtifactFilter artifactFilter) {
            }

            @Override
            public void setDownloadUrl(String downloadUrl) {
            }

            @Override
            public void setDependencyTrail(List<String> dependencyTrail) {
            }

            @Override
            public void setScope(String scope) {
            }

            @Override
            public void setVersionRange(VersionRange newRange) {
            }

            @Override
            public void selectVersion(String version) {
            }

            @Override
            public void setGroupId(String groupId) {
            }

            @Override
            public void setArtifactId(String artifactId) {
            }

            @Override
            public void setResolved(boolean resolved) {
            }

            @Override
            public void setResolvedVersion(String version) {
            }

            @Override
            public void setArtifactHandler(ArtifactHandler handler) {
            }

            @Override
            public void setRelease(boolean release) {
            }

            @Override
            public void setAvailableVersions(List<ArtifactVersion> versions) {
            }

            @Override
            public void setOptional(boolean optional) {
            }

            @Override
            public void setVersion(String version) {
            }

            @Override
            public void setFile(File destination) {
            }

            @Override
            public void setBaseVersion(String baseVersion) {
            }

            @Override
            public void addMetadata(ArtifactMetadata metadata) {
            }

            @Override
            public void setRepository(org.apache.maven.artifact.repository.ArtifactRepository remoteRepository) {
            }

            @Override
            public void updateVersion(String version, org.apache.maven.artifact.repository.ArtifactRepository localRepository) {
            }
        };
    }

    @Override
    public void contextualize(Context context) throws ContextException {
        plexusContainer = (PlexusContainer)context.get(PlexusConstants.PLEXUS_KEY);
    }
}
