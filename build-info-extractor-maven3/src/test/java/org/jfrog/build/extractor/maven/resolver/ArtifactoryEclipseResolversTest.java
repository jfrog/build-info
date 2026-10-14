package org.jfrog.build.extractor.maven.resolver;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.impl.MetadataResolver;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.MetadataRequest;
import org.eclipse.aether.resolution.MetadataResult;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The Artifactory resolvers decorate Maven's {@link ArtifactResolver} and {@link MetadataResolver}.
 * They must not extend Maven's internal resolver classes: the constructors of those classes are different in each Maven version
 * (for example, Maven 3.10 has no default constructor), which makes the extractor fail on startup.
 */
@Test
public class ArtifactoryEclipseResolversTest {

    private static final RemoteRepository MAVEN_CENTRAL = repo("central", "https://repo.maven.apache.org/maven2");
    private static final RemoteRepository ARTIFACTORY_RELEASE = repo("artifactory-release", "https://art/artifactory/libs-release");
    private static final RemoteRepository ARTIFACTORY_SNAPSHOT = repo("artifactory-snapshot", "https://art/artifactory/libs-snapshot");

    private StubHelper helper;
    private RecordingArtifactResolver artifactDelegate;
    private RecordingMetadataResolver metadataDelegate;
    private ArtifactoryEclipseArtifactResolver artifactResolver;
    private ArtifactoryEclipseMetadataResolver metadataResolver;

    @BeforeMethod
    public void setUp() throws Exception {
        helper = new StubHelper(true);
        artifactDelegate = new RecordingArtifactResolver();
        metadataDelegate = new RecordingMetadataResolver();

        artifactResolver = new ArtifactoryEclipseArtifactResolver();
        set(artifactResolver, "helper", helper);
        set(artifactResolver, "delegate", artifactDelegate);

        metadataResolver = new ArtifactoryEclipseMetadataResolver();
        set(metadataResolver, "helper", helper);
        set(metadataResolver, "delegate", metadataDelegate);
    }

    @Test
    public void resolversDoNotExtendMavenInternalClasses() {
        assertSame(Object.class, ArtifactoryEclipseArtifactResolver.class.getSuperclass());
        assertSame(Object.class, ArtifactoryEclipseMetadataResolver.class.getSuperclass());
        assertTrue(ArtifactResolver.class.isAssignableFrom(ArtifactoryEclipseArtifactResolver.class));
        assertTrue(MetadataResolver.class.isAssignableFrom(ArtifactoryEclipseMetadataResolver.class));
    }

    @Test
    public void resolveArtifactEnforcesArtifactoryRepositories() throws Exception {
        ArtifactRequest request = artifactRequest();

        artifactResolver.resolveArtifact(null, request);

        assertEquals(helper.resolutionRepositories, artifactDelegate.requests.get(0).getRepositories());
    }

    @Test
    public void resolveArtifactsEnforcesArtifactoryRepositories() throws Exception {
        List<ArtifactRequest> requests = new ArrayList<>();
        requests.add(artifactRequest());
        requests.add(artifactRequest());

        artifactResolver.resolveArtifacts(null, requests);

        assertEquals(2, artifactDelegate.requests.size());
        for (ArtifactRequest request : artifactDelegate.requests) {
            assertEquals(helper.resolutionRepositories, request.getRepositories());
        }
    }

    @Test
    public void artifactRepositoriesAreKeptWithoutArtifactoryResolution() throws Exception {
        helper = new StubHelper(false);
        set(artifactResolver, "helper", helper);
        ArtifactRequest request = artifactRequest();

        artifactResolver.resolveArtifact(null, request);
        artifactResolver.resolveArtifacts(null, Collections.singletonList(artifactRequest()));

        for (ArtifactRequest delegated : artifactDelegate.requests) {
            assertEquals(Collections.singletonList(MAVEN_CENTRAL), delegated.getRepositories());
        }
    }

    @Test
    public void metadataRepositoryIsChosenByNature() throws Exception {
        MetadataRequest release = metadataRequest(Metadata.Nature.RELEASE);
        MetadataRequest snapshot = metadataRequest(Metadata.Nature.SNAPSHOT);

        metadataResolver.resolveMetadata(null, java.util.Arrays.asList(release, snapshot));

        assertSame(ARTIFACTORY_RELEASE, release.getRepository());
        assertSame(ARTIFACTORY_SNAPSHOT, snapshot.getRepository());
        assertEquals(2, metadataDelegate.requests.size());
    }

    @Test
    public void metadataRepositoryIsKeptWithoutArtifactoryResolution() throws Exception {
        helper = new StubHelper(false);
        set(metadataResolver, "helper", helper);
        MetadataRequest request = metadataRequest(Metadata.Nature.RELEASE);

        metadataResolver.resolveMetadata(null, Collections.singletonList(request));

        assertSame(MAVEN_CENTRAL, request.getRepository());
    }

    private static ArtifactRequest artifactRequest() {
        return new ArtifactRequest(new DefaultArtifact("org.test:test:1.0"), Collections.singletonList(MAVEN_CENTRAL), null);
    }

    private static MetadataRequest metadataRequest(Metadata.Nature nature) {
        return new MetadataRequest(new DefaultMetadata("org.test", "test", "1.0", "maven-metadata.xml", nature), MAVEN_CENTRAL, null);
    }

    private static RemoteRepository repo(String id, String url) {
        return new RemoteRepository.Builder(id, "default", url).build();
    }

    private static void set(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * Returns Artifactory repositories without reading the build configuration.
     */
    private static class StubHelper extends ArtifactoryEclipseResolversHelper {
        private final List<RemoteRepository> resolutionRepositories = new ArrayList<>();

        StubHelper(boolean configured) {
            if (configured) {
                resolutionRepositories.add(ARTIFACTORY_SNAPSHOT);
                resolutionRepositories.add(ARTIFACTORY_RELEASE);
            }
        }

        @Override
        List<RemoteRepository> getResolutionRepositories(RepositorySystemSession session) {
            return resolutionRepositories;
        }

        @Override
        RemoteRepository getSnapshotRepository(RepositorySystemSession session) {
            return ARTIFACTORY_SNAPSHOT;
        }

        @Override
        RemoteRepository getReleaseRepository(RepositorySystemSession session) {
            return ARTIFACTORY_RELEASE;
        }
    }

    private static class RecordingArtifactResolver implements ArtifactResolver {
        private final List<ArtifactRequest> requests = new ArrayList<>();

        @Override
        public ArtifactResult resolveArtifact(RepositorySystemSession session, ArtifactRequest request) throws ArtifactResolutionException {
            requests.add(request);
            return new ArtifactResult(request);
        }

        @Override
        public List<ArtifactResult> resolveArtifacts(RepositorySystemSession session, Collection<? extends ArtifactRequest> requests) throws ArtifactResolutionException {
            List<ArtifactResult> results = new ArrayList<>();
            for (ArtifactRequest request : requests) {
                this.requests.add(request);
                results.add(new ArtifactResult(request));
            }
            return results;
        }
    }

    private static class RecordingMetadataResolver implements MetadataResolver {
        private final List<MetadataRequest> requests = new ArrayList<>();

        @Override
        public List<MetadataResult> resolveMetadata(RepositorySystemSession session, Collection<? extends MetadataRequest> requests) {
            List<MetadataResult> results = new ArrayList<>();
            for (MetadataRequest request : requests) {
                this.requests.add(request);
                results.add(new MetadataResult(request));
            }
            return results;
        }
    }
}
