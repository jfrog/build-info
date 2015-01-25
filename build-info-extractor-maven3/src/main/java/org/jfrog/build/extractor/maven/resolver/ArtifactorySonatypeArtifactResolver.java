package org.jfrog.build.extractor.maven.resolver;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.impl.internal.DefaultArtifactResolver;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.util.DefaultRepositorySystemSession;

import javax.inject.Named;
import java.util.Collection;
import java.util.List;

/**
 * The class extends Maven's DefaultArtifactResolver and adds to it code that enforces Artifactory's configured resolution repositories.
 */
@Named
@Component( role = ArtifactorySonatypeArtifactResolver.class )
public class ArtifactorySonatypeArtifactResolver extends DefaultArtifactResolver {

    @Requirement
    private ResolutionHelper resolutionHelper;

    @Requirement
    private Logger logger;

    @Requirement
    private ArtifactorySonatypeResolversHelper helper;

    public void initResolutionRepositories(RepositorySystemSession session) {
        helper.getResolutionRepositories(session);
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

    public List<ArtifactResult> resolveArtifacts( RepositorySystemSession session, Collection<? extends ArtifactRequest> requests )
            throws ArtifactResolutionException {

        if (session instanceof DefaultRepositorySystemSession) {
            DefaultRepositorySystemSession defRepoSession = (DefaultRepositorySystemSession)session;
            defRepoSession.setNotFoundCachingEnabled(false);
            defRepoSession.setTransferErrorCachingEnabled(false);
        }

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
