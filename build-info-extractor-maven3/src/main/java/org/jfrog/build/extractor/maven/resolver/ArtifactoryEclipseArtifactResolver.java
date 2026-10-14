package org.jfrog.build.extractor.maven.resolver;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import javax.inject.Named;
import java.util.Collection;
import java.util.List;

/**
 * Decorates Maven's {@link ArtifactResolver}, and enforces the Artifactory resolution repositories before delegating to it.
 * <p>
 * The class implements the {@link ArtifactResolver} interface, instead of extending Maven's internal DefaultArtifactResolver,
 * since the constructors of the internal Maven classes change between Maven versions (for example, Maven 3.10 does not have a default constructor).
 * The {@link ArtifactResolver} interface is the same for all supported Maven versions.
 */
@Named
@Component( role = ArtifactResolver.class )
public class ArtifactoryEclipseArtifactResolver implements ArtifactResolver {

    @Requirement
    private PlexusContainer container;

    @Requirement
    private Logger logger;

    @Requirement
    private ArtifactoryEclipseResolversHelper helper;

    private volatile ArtifactResolver delegate;

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
    public ArtifactResult resolveArtifact(RepositorySystemSession session, ArtifactRequest request) throws ArtifactResolutionException {
        enforceResolutionRepositories(session, request);
        return getDelegate().resolveArtifact(session, request);
    }

    /**
     * Before letting Maven resolve the artifacts, this method enforces the configured Artifactory resolution repositories.
     */
    @Override
    public List<ArtifactResult> resolveArtifacts(RepositorySystemSession session, Collection<? extends ArtifactRequest> requests)
            throws ArtifactResolutionException {
        for (ArtifactRequest request : requests) {
            enforceResolutionRepositories(session, request);
        }
        // Now we let Maven resolve the artifacts:
        return getDelegate().resolveArtifacts(session, requests);
    }

    public RemoteRepository getSnapshotRepository(RepositorySystemSession session) {
        return helper.getSnapshotRepository(session);
    }

    public RemoteRepository getReleaseRepository(RepositorySystemSession session) {
        return helper.getReleaseRepository(session);
    }

    /**
     * Lazily look up Maven's own artifact resolver. This class is registered with the same role as Maven's resolver, so the
     * look-up can't be a plain requirement - it would resolve to this class. The look-up is lazy to avoid circular dependencies.
     */
    private ArtifactResolver getDelegate() {
        if (delegate == null) {
            synchronized (this) {
                if (delegate == null) {
                    delegate = ResolverDelegateLookup.lookupDelegate(container, ArtifactResolver.class, this);
                }
            }
        }
        return delegate;
    }
}
