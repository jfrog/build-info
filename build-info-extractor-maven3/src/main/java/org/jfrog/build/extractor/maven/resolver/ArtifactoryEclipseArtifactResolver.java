package org.jfrog.build.extractor.maven.resolver;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import javax.inject.Named;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Decorates Maven's {@link ArtifactResolver}, and enforces the Artifactory resolution repositories before delegating to it.
 */
@Named
@Component( role = ArtifactResolver.class )
public class ArtifactoryEclipseArtifactResolver extends ArtifactoryEclipseResolverBase<ArtifactResolver> implements ArtifactResolver {

    private void enforceResolutionRepositories(RepositorySystemSession session, Collection<? extends ArtifactRequest> requests) {
        List<RemoteRepository> repositories = getResolutionRepositories(session);
        if (!repositories.isEmpty()) {
            for (ArtifactRequest request : requests) {
                request.setRepositories(repositories);
            }
        }
    }

    /**
     * Before letting Maven resolve the artifact, this method enforces the configured Artifactory resolution repositories.
     */
    @Override
    public ArtifactResult resolveArtifact(RepositorySystemSession session, ArtifactRequest request) throws ArtifactResolutionException {
        enforceResolutionRepositories(session, Collections.singletonList(request));
        return getDelegate(ArtifactResolver.class).resolveArtifact(session, request);
    }

    /**
     * Before letting Maven resolve the artifacts, this method enforces the configured Artifactory resolution repositories.
     */
    @Override
    public List<ArtifactResult> resolveArtifacts(RepositorySystemSession session, Collection<? extends ArtifactRequest> requests)
            throws ArtifactResolutionException {
        enforceResolutionRepositories(session, requests);
        return getDelegate(ArtifactResolver.class).resolveArtifacts(session, requests);
    }
}
