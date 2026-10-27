package org.jfrog.build.extractor.maven.resolver;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.MetadataResolver;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.MetadataRequest;
import org.eclipse.aether.resolution.MetadataResult;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

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
            if (request.getRepository() != null && !repositories.contains(request.getRepository()) && isMavenMetadata(request)) {
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
        List<RemoteRepository> repositories = getResolutionRepositories(session);
        if (repositories.isEmpty()) {
            return getDelegate().resolveMetadata(session, requests);
        }

        // Maven requests the version metadata from each repository of the project. After the redirection, several of these requests are the same
        // request to the same Artifactory repository. Each of them is resolved only once, and its result is shared by the duplicate requests.
        List<MetadataRequest> uniqueRequests = new ArrayList<>();
        Map<MetadataRequest, MetadataRequest> duplicateOf = new IdentityHashMap<>();
        Map<List<Object>, MetadataRequest> requestsByKey = new HashMap<>();
        for (MetadataRequest request : requests) {
            if (repositories.contains(request.getRepository()) && isMavenMetadata(request)) {
                MetadataRequest original = requestsByKey.putIfAbsent(getKey(request), request);
                if (original != null) {
                    duplicateOf.put(request, original);
                    continue;
                }
            }
            uniqueRequests.add(request);
        }
        if (duplicateOf.isEmpty()) {
            return getDelegate().resolveMetadata(session, requests);
        }

        // The results are in the order of the requests
        List<MetadataResult> uniqueResults = getDelegate().resolveMetadata(session, uniqueRequests);
        Map<MetadataRequest, MetadataResult> resultsByRequest = new IdentityHashMap<>();
        for (int i = 0; i < uniqueRequests.size(); i++) {
            resultsByRequest.put(uniqueRequests.get(i), uniqueResults.get(i));
        }
        List<MetadataResult> results = new ArrayList<>();
        for (MetadataRequest request : requests) {
            MetadataRequest original = duplicateOf.get(request);
            if (original == null) {
                results.add(resultsByRequest.get(request));
                continue;
            }
            MetadataResult originalResult = resultsByRequest.get(original);
            results.add(new MetadataResult(request)
                    .setMetadata(originalResult.getMetadata())
                    .setException(originalResult.getException())
                    .setUpdated(originalResult.isUpdated()));
        }
        return results;
    }

    private static boolean isMavenMetadata(MetadataRequest request) {
        return request.getMetadata() != null && MAVEN_METADATA_FILE.equals(request.getMetadata().getType());
    }

    /**
     * Requests with the same key download the same file, from the same repository, to the same location.
     */
    private static List<Object> getKey(MetadataRequest request) {
        Metadata metadata = request.getMetadata();
        return Arrays.asList(metadata.getGroupId(), metadata.getArtifactId(), metadata.getVersion(), metadata.getType(), metadata.getNature(),
                request.getRepository(), request.getRequestContext(), request.isDeleteLocalCopyIfMissing(), request.isFavorLocalRepository());
    }
}
