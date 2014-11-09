package org.jfrog.build.extractor.maven.resolver;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.impl.internal.DefaultMetadataResolver;
import org.sonatype.aether.metadata.Metadata;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.MetadataRequest;
import org.sonatype.aether.resolution.MetadataResult;
import org.sonatype.aether.spi.log.Logger;

import javax.inject.Named;
import java.util.Collection;
import java.util.List;

@Named
@Component( role = ArtifactorySonatypeMetadataResolver.class )
public class ArtifactorySonatypeMetadataResolver extends DefaultMetadataResolver {

    @Requirement
    private ResolutionHelper resolutionHelper;

    @Requirement
    private Logger logger;

    @Requirement
    private ArtifactorySonatypeResolversHelper helper;

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
        for(MetadataRequest request : requests) {
            enforceResolutionRepositories(session, request);
        }
        // Now we let Maven resolve the artifacts:
        return super.resolveMetadata(session, requests);
    }

    public RemoteRepository getSnapshotRepository(RepositorySystemSession session) {
        return helper.getSnapshotRepository(session);
    }

    public RemoteRepository getReleaseRepository(RepositorySystemSession session) {
        return helper.getReleaseRepository(session);
    }
}
