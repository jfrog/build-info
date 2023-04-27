package org.jfrog.build.extractor.maven.resolver;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Named
@Component(role = ArtifactoryEclipseResolversHelper.class)
public class ArtifactoryEclipseResolversHelper {

    @Requirement
    private ResolutionHelper resolutionHelper;

    @Requirement
    private Logger logger;

    private final List<ArtifactRepository> resolutionPluginRepositories = new ArrayList<>();
    private final List<RemoteRepository> resolutionRepositories = new ArrayList<>();
    private RemoteRepository releaseRepository = null;
    private RemoteRepository snapshotRepository = null;

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
            ArtifactoryResolution artifactoryResolution = new ArtifactoryResolution(resolutionHelper.getRepoReleaseUrl(), resolutionHelper.getRepoSnapshotUrl(), resolutionHelper.getRepoUsername(), resolutionHelper.getRepoPassword(), logger);
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
            ArtifactoryPluginResolution repositoryBuilder = new ArtifactoryPluginResolution(resolutionHelper.getRepoReleaseUrl(), resolutionHelper.getRepoSnapshotUrl(), resolutionHelper.getRepoUsername(), resolutionHelper.getRepoPassword(), logger);
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
}
