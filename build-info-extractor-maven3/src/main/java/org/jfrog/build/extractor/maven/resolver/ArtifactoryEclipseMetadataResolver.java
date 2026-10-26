package org.jfrog.build.extractor.maven.resolver;

import org.codehaus.plexus.component.annotations.Component;
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
 */
@Named
@Component( role = MetadataResolver.class )
public class ArtifactoryEclipseMetadataResolver extends ArtifactoryEclipseResolverBase<MetadataResolver> implements MetadataResolver {

    private static final String MAVEN_METADATA_FILE = "maven-metadata.xml";

    public ArtifactoryEclipseMetadataResolver() {
        super(MetadataResolver.class);
    }

    private void enforceResolutionRepositories(RepositorySystemSession session, Collection<? extends MetadataRequest> requests) {
        List<RemoteRepository> repositories = getResolutionRepositories(session);
        if (repositories.isEmpty()) {
            return;
        }
        for (MetadataRequest request : requests) {
            // Only Maven's version metadata is redirected to the Artifactory release or snapshot repository. Other metadata is specific to the
            // repository in the request and must stay there. For example, the '.meta/prefixes.txt' files that Maven Resolver 2.x requests for each repository.
            // Metadata already requested from an Artifactory repository is kept there, so Maven applies the policy of that repository.
            if (request.getRepository() != null && !repositories.contains(request.getRepository())
                    && request.getMetadata() != null && MAVEN_METADATA_FILE.equals(request.getMetadata().getType())) {
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
        return getDelegate().resolveMetadata(session, requests);
    }
}
