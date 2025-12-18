package org.jfrog.build.extractor.maven.resolver;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.jfrog.build.extractor.ProxySelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Singleton
@Named
public class ArtifactoryEclipseResolversHelper {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ResolutionHelper resolutionHelper;

    private final List<ArtifactRepository> resolutionPluginRepositories = new ArrayList<>();
    private final List<RemoteRepository> resolutionRepositories = new ArrayList<>();
    private RemoteRepository releaseRepository = null;
    private RemoteRepository snapshotRepository = null;

    @Inject
    public ArtifactoryEclipseResolversHelper(ResolutionHelper resolutionHelper) {
        this.resolutionHelper = resolutionHelper;
    }

    void initResolutionRepositories(RepositorySystemSession session) {
        getResolutionRepositories(session);
    }

    /**
     * Create a list containing one release and one snapshot resolution repositories, according to the configuration in the Artifactory plugin.
     * The list is used to override Maven's default or configured repositories, so that the build dependencies are resolved from Artifactory.
     * The list is saved and reused for further invocations to this method.
     *
     * @param session - Settings and components that control the repository system
     * @return Snapshot and release resolution repositories
     */
    List<RemoteRepository> getResolutionRepositories(RepositorySystemSession session) {
        if (resolutionRepositories.isEmpty()) {
            initResolutionHelper(session);
            ArtifactoryResolution artifactoryResolution = getArtifactoryResolution();
            snapshotRepository = artifactoryResolution.createSnapshotRepository();
            if (snapshotRepository != null) {
                resolutionRepositories.add(snapshotRepository);
            }
            releaseRepository = artifactoryResolution.createReleaseRepository();
            if (releaseRepository != null) {
                resolutionRepositories.add(releaseRepository);
            }
        }
        return resolutionRepositories;
    }

    private ArtifactoryResolution getArtifactoryResolution() {
        ProxySelector proxySelector = new ProxySelector(resolutionHelper.getHttpProxyHost(), resolutionHelper.getHttpProxyPort(), resolutionHelper.getHttpProxyUsername(), resolutionHelper.getHttpProxyPassword(), resolutionHelper.getHttpsProxyHost(), resolutionHelper.getHttpsProxyPort(), resolutionHelper.getHttpsProxyUsername(), resolutionHelper.getHttpsProxyPassword(), resolutionHelper.getNoProxy());
        return new ArtifactoryResolution(resolutionHelper.getRepoReleaseUrl(), resolutionHelper.getRepoSnapshotUrl(), resolutionHelper.getRepoUsername(), resolutionHelper.getRepoPassword(), proxySelector, logger)
                .setSnapshotEnabled(isSnapshotEnabled())
                .setSnapshotUpdatePolicy(resolutionHelper.getSnapshotUpdatePolicy());
    }

    private void initResolutionHelper(RepositorySystemSession session) {
        if (resolutionHelper.isInitialized()) {
            return;
        }
        Properties allMavenProps = new Properties();
        allMavenProps.putAll(session.getSystemProperties());
        allMavenProps.putAll(session.getUserProperties());
        resolutionHelper.init(allMavenProps);
    }

    List<ArtifactRepository> getResolutionPluginRepositories(RepositorySystemSession session) {
        if (resolutionPluginRepositories.isEmpty()) {
            initResolutionHelper(session);
            ArtifactoryPluginResolution repositoryBuilder = getArtifactoryPluginResolution();
            ArtifactRepository snapshotRepository = repositoryBuilder.createSnapshotRepository();
            if (snapshotRepository != null) {
                resolutionPluginRepositories.add(snapshotRepository);
            }
            ArtifactRepository releaseRepository = repositoryBuilder.createReleaseRepository();
            if (releaseRepository != null) {
                resolutionPluginRepositories.add(releaseRepository);
            }
        }
        return resolutionPluginRepositories;
    }

    private ArtifactoryPluginResolution getArtifactoryPluginResolution() {
        ProxySelector proxySelector = new ProxySelector(resolutionHelper.getHttpProxyHost(), resolutionHelper.getHttpProxyPort(), resolutionHelper.getHttpProxyUsername(), resolutionHelper.getHttpProxyPassword(), resolutionHelper.getHttpsProxyHost(), resolutionHelper.getHttpsProxyPort(), resolutionHelper.getHttpsProxyUsername(), resolutionHelper.getHttpsProxyPassword(), resolutionHelper.getNoProxy());
        return new ArtifactoryPluginResolution(resolutionHelper.getRepoReleaseUrl(), resolutionHelper.getRepoSnapshotUrl(), resolutionHelper.getRepoUsername(), resolutionHelper.getRepoPassword(), proxySelector, logger)
                .setSnapshotEnabled(isSnapshotEnabled())
                .setSnapshotUpdatePolicy(resolutionHelper.getSnapshotUpdatePolicy());
    }

    RemoteRepository getSnapshotRepository(RepositorySystemSession session) {
        // Init repositories configured in the Artifactory plugin:
        initResolutionRepositories(session);
        if (snapshotRepository != null) {
            return snapshotRepository;
        }
        return releaseRepository;
    }

    RemoteRepository getReleaseRepository(RepositorySystemSession session) {
        // Init repositories configured in the Artifactory plugin:
        initResolutionRepositories(session);

        return releaseRepository;
    }

    private boolean isSnapshotEnabled() {
        return !resolutionHelper.isSnapshotDisabled();
    }
}
