package org.jfrog.build.extractor.maven.plugin

import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.sonatype.aether.RepositorySystemSession
import org.sonatype.aether.impl.ArtifactResolver
import org.sonatype.aether.repository.RemoteRepository
import org.sonatype.aether.resolution.ArtifactRequest
import org.sonatype.aether.resolution.ArtifactResolutionException
import org.sonatype.aether.resolution.ArtifactResult


/**
 * {@link ArtifactResolver} implementation using remote repository specified for all resolution requests.
 */
@SuppressWarnings([ 'GrFinalVariableAccess' ])
class RepositoryResolver implements ArtifactResolver
{
    private final ArtifactResolver       delegateResolver
    private final List<RemoteRepository> remoteRepositories


    @Requires({ delegateResolver && remoteRepository })
    @Ensures ({ this.delegateResolver && this.remoteRepositories })
    RepositoryResolver ( ArtifactResolver delegateResolver, String remoteRepository )
    {
        final repository = new RemoteRepository( remoteRepository, 'default', remoteRepository )
        repository.setPolicy( true,  null )
        repository.setPolicy( false, null )

        this.delegateResolver   = delegateResolver
        this.remoteRepositories = [ repository ].asImmutable()
    }


    @Override
    @Requires({ session && requests })
    List<ArtifactResult> resolveArtifacts ( RepositorySystemSession session, Collection<? extends ArtifactRequest> requests )
        throws ArtifactResolutionException
    {
        requests.collect{ resolveArtifact( session, it ) }
    }


    @Override
    @Requires({ session && request })
    ArtifactResult resolveArtifact ( RepositorySystemSession session, ArtifactRequest request )
        throws ArtifactResolutionException
    {
        final originalRepositories = request.repositories

        try
        {
            request.repositories = remoteRepositories
            delegateResolver.resolveArtifact( session, request )
        }
        catch ( Throwable ignored )
        {   /**
             * We get here for artifacts that were resolved *before* ExtractorMojo made all replacements
             * and are needed later. Re-resolving brings them from the local cache.
             */
            request.repositories = originalRepositories
            delegateResolver.resolveArtifact( session, request )
        }
    }
}
