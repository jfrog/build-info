package org.jfrog.build.extractor.maven.resolver;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.MetadataResolver;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.MetadataRequest;
import org.eclipse.aether.resolution.MetadataResult;

import javax.inject.Named;
import java.util.Collection;
import java.util.List;

/**
 * Decorates Maven's {@link MetadataResolver}, and enforces the Artifactory resolution repositories before delegating to it.
 * <p>
 * The class implements the {@link MetadataResolver} interface, instead of extending Maven's internal DefaultMetadataResolver,
 * since the constructors of the internal Maven classes change between Maven versions (for example, Maven 3.10 does not have a default constructor).
 * The {@link MetadataResolver} interface is the same for all supported Maven versions.
 */
@Named
@Component( role = MetadataResolver.class )
public class ArtifactoryEclipseMetadataResolver implements MetadataResolver {

    @Requirement
    private PlexusContainer container;

    @Requirement
    private Logger logger;

    @Requirement
    private ArtifactoryEclipseResolversHelper helper;

    private volatile MetadataResolver delegate;

    private void enforceResolutionRepositories(RepositorySystemSession session, MetadataRequest request) {
        // Get the Artifactory repositories configured in the Artifactory plugin:
        List<RemoteRepository> repositories = helper.getResolutionRepositories(session);

        // The repositories list can be empty, in case this build is not running from a CI server.
        // In that case, we do not want to override Maven's configured repositories:
        if (repositories != null && !repositories.isEmpty() && request.getRepository() != null && request.getMetadata() != null) {
            if (request.getMetadata().getNature() == Metadata.Nature.SNAPSHOT) {
                request.setRepository(getSnapshotRepository(session));
            } else {
                request.setRepository(getReleaseRepository(session));
            }
        }
    }

    @Override
    public List<MetadataResult> resolveMetadata(RepositorySystemSession session, Collection<? extends MetadataRequest> requests ) {
        for (MetadataRequest request : requests) {
            enforceResolutionRepositories(session, request);
        }
        // Now we let Maven resolve the metadata:
        return getDelegate().resolveMetadata(session, requests);
    }

    public RemoteRepository getSnapshotRepository(RepositorySystemSession session) {
        return helper.getSnapshotRepository(session);
    }

    public RemoteRepository getReleaseRepository(RepositorySystemSession session) {
        return helper.getReleaseRepository(session);
    }

    /**
     * Lazily look up Maven's own metadata resolver. This class is registered with the same role as Maven's resolver, so the
     * look-up can't be a plain requirement - it would resolve to this class. The look-up is lazy to avoid circular dependencies.
     */
    private MetadataResolver getDelegate() {
        if (delegate == null) {
            synchronized (this) {
                if (delegate == null) {
                    delegate = ResolverDelegateLookup.lookupDelegate(container, MetadataResolver.class, this);
                }
            }
        }
        return delegate;
    }
}
