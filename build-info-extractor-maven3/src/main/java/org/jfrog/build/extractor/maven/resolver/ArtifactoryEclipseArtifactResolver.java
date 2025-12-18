package org.jfrog.build.extractor.maven.resolver;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.internal.impl.DefaultArtifactResolver;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.List;

@Singleton
@Named
public class ArtifactoryEclipseArtifactResolver extends DefaultArtifactResolver {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ResolutionHelper resolutionHelper;
    private final ArtifactoryEclipseResolversHelper helper;

    @Inject
    public ArtifactoryEclipseArtifactResolver(ResolutionHelper resolutionHelper, ArtifactoryEclipseResolversHelper helper) {
        this.resolutionHelper = resolutionHelper;
        this.helper = helper;
    }

    public void initResolutionRepositories(RepositorySystemSession session) {
        helper.initResolutionRepositories(session);
    }

    private void enforceResolutionRepositories(RepositorySystemSession session, ArtifactRequest request) {
        // Get the Artifactory repositories configured in the Artifactory plugin:
        List<RemoteRepository> repositories = helper.getResolutionRepositories(session);

        // The repositories list can be empty, in case this build is not running from a CI server.
        // In that case, we do not want to override Maven's configured repositories:
        if (repositories != null && !repositories.isEmpty()) {
            request.setRepositories(repositories);
        }
    }

    /**
     * Before letting Maven resolve the artifact, this method enforces the configured Artifactory resolution repositories.
     */
    @Override
    public List<ArtifactResult> resolveArtifacts(RepositorySystemSession session, Collection<? extends ArtifactRequest> requests)
            throws ArtifactResolutionException {

        for(ArtifactRequest request : requests) {
            enforceResolutionRepositories(session, request);
        }
        // Now we let Maven resolve the artifacts:
        return super.resolveArtifacts(session, requests);
    }

    public RemoteRepository getSnapshotRepository(RepositorySystemSession session) {
        return helper.getSnapshotRepository(session);
    }

    public RemoteRepository getReleaseRepository(RepositorySystemSession session) {
        return helper.getReleaseRepository(session);
    }
}
