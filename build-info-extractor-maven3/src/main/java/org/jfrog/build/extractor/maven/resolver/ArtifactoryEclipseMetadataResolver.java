package org.jfrog.build.extractor.maven.resolver;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.MetadataResolver;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.resolution.MetadataRequest;
import org.eclipse.aether.resolution.MetadataResult;

import javax.inject.Named;
import java.util.Collection;
import java.util.List;

/**
 * Decorates Maven's {@link MetadataResolver}, and enforces the Artifactory resolution repositories before delegating to it.
 */
@Named
@Component( role = MetadataResolver.class )
public class ArtifactoryEclipseMetadataResolver extends ArtifactoryEclipseResolverBase<MetadataResolver> implements MetadataResolver {

    private void enforceResolutionRepositories(RepositorySystemSession session, Collection<? extends MetadataRequest> requests) {
        if (getResolutionRepositories(session).isEmpty()) {
            return;
        }
        for (MetadataRequest request : requests) {
            if (request.getRepository() != null && request.getMetadata() != null) {
                if (request.getMetadata().getNature() == Metadata.Nature.SNAPSHOT) {
                    request.setRepository(helper.getSnapshotRepository(session));
                } else {
                    request.setRepository(helper.getReleaseRepository(session));
                }
            }
        }
    }

    @Override
    public List<MetadataResult> resolveMetadata(RepositorySystemSession session, Collection<? extends MetadataRequest> requests) {
        enforceResolutionRepositories(session, requests);
        return getDelegate(MetadataResolver.class).resolveMetadata(session, requests);
    }
}
