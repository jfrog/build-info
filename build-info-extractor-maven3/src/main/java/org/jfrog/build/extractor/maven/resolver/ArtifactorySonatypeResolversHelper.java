package org.jfrog.build.extractor.maven.resolver;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.jfrog.build.extractor.ProxySelector;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.repository.RemoteRepository;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Named
@Component(role = ArtifactorySonatypeResolversHelper.class)
public class ArtifactorySonatypeResolversHelper {

    @Requirement
    private ResolutionHelper resolutionHelper;

    @Requirement
    private Logger logger;

    private final List<RemoteRepository> resolutionRepositories = new ArrayList<>();
    private RemoteRepository releaseRepository = null;
    private RemoteRepository snapshotRepository = null;

    public void initResolutionRepositories(RepositorySystemSession session) {
        getResolutionRepositories(session);
    }

    /**
     * Create a list containing one release and one snapshot resolution repositories, according to the configuration in the Artifactory plugin.
     * The list is used to override Maven's default or configured repositories, so that the build dependencies are resolved from Artifactory.
     * The list is saved and reused for further invocations to this method.
     */
    public List<RemoteRepository> getResolutionRepositories(RepositorySystemSession session) {
        if (resolutionRepositories.isEmpty()) {
            Properties allMavenProps = new Properties();
            allMavenProps.putAll(session.getSystemProperties());
            allMavenProps.putAll(session.getUserProperties());
            resolutionHelper.init(allMavenProps);
            ProxySelector proxySelector = new ProxySelector(resolutionHelper.getHttpProxyHost(), resolutionHelper.getHttpProxyPort(), resolutionHelper.getHttpProxyUsername(), resolutionHelper.getHttpProxyPassword(), resolutionHelper.getHttpsProxyHost(), resolutionHelper.getHttpsProxyPort(), resolutionHelper.getHttpsProxyUsername(), resolutionHelper.getHttpsProxyPassword(), resolutionHelper.getNoProxy());

            ArtifactorySonatypeResolution artifactoryResolution = new ArtifactorySonatypeResolution(resolutionHelper.getRepoReleaseUrl(), resolutionHelper.getRepoSnapshotUrl(), resolutionHelper.getRepoUsername(), resolutionHelper.getRepoPassword(), proxySelector, logger);

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

    public RemoteRepository getSnapshotRepository(RepositorySystemSession session) {
        // Init repositories configured in the Artifactory plugin:
        initResolutionRepositories(session);

        if (snapshotRepository != null) {
            return snapshotRepository;
        }
        return releaseRepository;
    }

    public RemoteRepository getReleaseRepository(RepositorySystemSession session) {
        // Init repositories configured in the Artifactory plugin:
        initResolutionRepositories(session);

        return releaseRepository;
    }
}