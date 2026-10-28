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
import org.apache.commons.lang3.reflect.FieldUtils;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.AbstractLogger;
import org.codehaus.plexus.logging.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

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
        FieldUtils.writeField(artifactResolver, "helper", helper, true);
        FieldUtils.writeField(artifactResolver, "delegate", artifactDelegate, true);

        metadataResolver = new ArtifactoryEclipseMetadataResolver();
        FieldUtils.writeField(metadataResolver, "helper", helper, true);
        FieldUtils.writeField(metadataResolver, "delegate", metadataDelegate, true);
    }

    @Test
    public void resolversDoNotExtendMavenInternalClasses() {
        assertSame(ArtifactoryEclipseResolverBase.class, ArtifactoryEclipseArtifactResolver.class.getSuperclass());
        assertSame(ArtifactoryEclipseResolverBase.class, ArtifactoryEclipseMetadataResolver.class.getSuperclass());
        assertSame(Object.class, ArtifactoryEclipseResolverBase.class.getSuperclass());
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
        FieldUtils.writeField(artifactResolver, "helper", helper, true);
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

        metadataResolver.resolveMetadata(null, Arrays.asList(release, snapshot));

        assertSame(ARTIFACTORY_RELEASE, release.getRepository());
        assertSame(ARTIFACTORY_SNAPSHOT, snapshot.getRepository());
        assertEquals(2, metadataDelegate.requests.size());
    }

    @Test
    public void metadataOfArtifactoryRepositoriesIsNotRedirected() throws Exception {
        // Maven skips the release repository for snapshot metadata, according to its policy. Redirecting it would download the metadata again.
        MetadataRequest snapshotOnRelease = metadataRequest("maven-metadata.xml", Metadata.Nature.SNAPSHOT, ARTIFACTORY_RELEASE);
        MetadataRequest releaseOnSnapshot = metadataRequest("maven-metadata.xml", Metadata.Nature.RELEASE, ARTIFACTORY_SNAPSHOT);

        metadataResolver.resolveMetadata(null, Arrays.asList(snapshotOnRelease, releaseOnSnapshot));

        assertSame(ARTIFACTORY_RELEASE, snapshotOnRelease.getRepository());
        assertSame(ARTIFACTORY_SNAPSHOT, releaseOnSnapshot.getRepository());
    }

    @Test
    public void metadataRepositoryIsKeptWithoutArtifactoryResolution() throws Exception {
        helper = new StubHelper(false);
        FieldUtils.writeField(metadataResolver, "helper", helper, true);
        MetadataRequest request = metadataRequest(Metadata.Nature.RELEASE);

        metadataResolver.resolveMetadata(null, Collections.singletonList(request));

        assertSame(MAVEN_CENTRAL, request.getRepository());
    }

    @Test
    public void onlyVersionMetadataIsRedirected() throws Exception {
        // Maven Resolver 2.x requests the prefixes file of a specific repository. It must not be redirected to another repository.
        MetadataRequest prefixes = metadataRequest("prefixes.txt", Metadata.Nature.RELEASE_OR_SNAPSHOT);

        metadataResolver.resolveMetadata(null, Collections.singletonList(prefixes));

        assertSame(MAVEN_CENTRAL, prefixes.getRepository());
        assertEquals(1, metadataDelegate.requests.size());
    }

    @Test
    public void duplicateVersionMetadataIsResolvedOnce() throws Exception {
        // Maven requests the metadata from each repository of the project. After the redirection, all of them are the same request.
        MetadataRequest fromCentral = metadataRequest("maven-metadata.xml", Metadata.Nature.SNAPSHOT, MAVEN_CENTRAL);
        MetadataRequest fromOtherRepo = metadataRequest("maven-metadata.xml", Metadata.Nature.SNAPSHOT, repo("other", "https://other/repo"));
        MetadataRequest fromArtifactory = metadataRequest("maven-metadata.xml", Metadata.Nature.SNAPSHOT, ARTIFACTORY_SNAPSHOT);
        MetadataRequest release = metadataRequest(Metadata.Nature.RELEASE);
        metadataDelegate.exception = new Exception("metadata not found");

        List<MetadataResult> results = metadataResolver.resolveMetadata(null, Arrays.asList(fromCentral, fromOtherRepo, fromArtifactory, release));

        assertEquals(Arrays.asList(fromCentral, release), metadataDelegate.requests);
        assertEquals(4, results.size());
        assertSame(fromCentral, results.get(0).getRequest());
        assertSame(fromOtherRepo, results.get(1).getRequest());
        assertSame(fromArtifactory, results.get(2).getRequest());
        assertSame(release, results.get(3).getRequest());
        for (MetadataResult duplicate : results.subList(1, 3)) {
            assertSame(results.get(0).getMetadata(), duplicate.getMetadata());
            assertSame(metadataDelegate.exception, duplicate.getException());
            assertEquals(results.get(0).isUpdated(), duplicate.isUpdated());
            assertSame(ARTIFACTORY_SNAPSHOT, duplicate.getRequest().getRepository());
        }
    }

    @Test
    public void differentMetadataRequestsAreNotDeduplicated() throws Exception {
        // All the requests are on the same Artifactory repository, and each differs from the first one in a single field of the key
        MetadataRequest request = artifactoryMetadataRequest("org.test", "test", "1.0", Metadata.Nature.RELEASE);
        List<MetadataRequest> requests = Arrays.asList(
                request,
                artifactoryMetadataRequest("org.other", "test", "1.0", Metadata.Nature.RELEASE),
                artifactoryMetadataRequest("org.test", "other", "1.0", Metadata.Nature.RELEASE),
                artifactoryMetadataRequest("org.test", "test", "2.0", Metadata.Nature.RELEASE),
                artifactoryMetadataRequest("org.test", "test", "1.0", Metadata.Nature.SNAPSHOT),
                artifactoryMetadataRequest("org.test", "test", "1.0", Metadata.Nature.RELEASE).setRequestContext("plugin"),
                artifactoryMetadataRequest("org.test", "test", "1.0", Metadata.Nature.RELEASE).setDeleteLocalCopyIfMissing(true),
                artifactoryMetadataRequest("org.test", "test", "1.0", Metadata.Nature.RELEASE).setFavorLocalRepository(true));

        List<MetadataResult> results = metadataResolver.resolveMetadata(null, requests);

        assertEquals(requests, metadataDelegate.requests);
        assertEquals(requests.size(), results.size());
    }

    @Test
    public void requestsWithoutMetadataAreNotDeduplicated() throws Exception {
        MetadataRequest withoutMetadata = new MetadataRequest(null, ARTIFACTORY_RELEASE, null);
        MetadataRequest request = artifactoryMetadataRequest("org.test", "test", "1.0", Metadata.Nature.RELEASE);
        MetadataRequest duplicate = artifactoryMetadataRequest("org.test", "test", "1.0", Metadata.Nature.RELEASE);

        List<MetadataResult> results = metadataResolver.resolveMetadata(null, Arrays.asList(withoutMetadata, request, duplicate));

        assertEquals(Arrays.asList(withoutMetadata, request), metadataDelegate.requests);
        assertEquals(3, results.size());
        assertSame(withoutMetadata, results.get(0).getRequest());
        assertSame(duplicate, results.get(2).getRequest());
    }

    @Test
    public void metadataOfOtherRepositoriesIsNotDeduplicated() throws Exception {
        MetadataRequest prefixes = metadataRequest("prefixes.txt", Metadata.Nature.RELEASE_OR_SNAPSHOT);
        MetadataRequest samePrefixes = metadataRequest("prefixes.txt", Metadata.Nature.RELEASE_OR_SNAPSHOT);

        metadataResolver.resolveMetadata(null, Arrays.asList(prefixes, samePrefixes));

        assertEquals(Arrays.asList(prefixes, samePrefixes), metadataDelegate.requests);
    }

    @Test
    public void metadataIsNotDeduplicatedWithoutArtifactoryResolution() throws Exception {
        helper = new StubHelper(false);
        FieldUtils.writeField(metadataResolver, "helper", helper, true);
        MetadataRequest request = metadataRequest(Metadata.Nature.SNAPSHOT);
        MetadataRequest sameRequest = metadataRequest(Metadata.Nature.SNAPSHOT);

        metadataResolver.resolveMetadata(null, Arrays.asList(request, sameRequest));

        assertEquals(Arrays.asList(request, sameRequest), metadataDelegate.requests);
    }

    @Test
    public void delegateIsMavenResolverAndIsLookedUpOnce() throws Exception {
        ArtifactoryEclipseArtifactResolver decorator = new ArtifactoryEclipseArtifactResolver();
        RecordingArtifactResolver mavenResolver = new RecordingArtifactResolver();
        ContainerStub container = new ContainerStub(ArtifactResolver.class, decorator, mavenResolver);
        FieldUtils.writeField(decorator, "container", container.proxy(), true);
        FieldUtils.writeField(decorator, "helper", new StubHelper(true), true);
        RecordingLogger logger = new RecordingLogger();
        FieldUtils.writeField(decorator, "logger", logger, true);

        decorator.resolveArtifact(null, artifactRequest());
        decorator.resolveArtifact(null, artifactRequest());

        assertEquals(2, mavenResolver.requests.size());
        assertEquals(1, container.lookups);
        assertEquals(Collections.singletonList("Decorating ArtifactResolver: " + RecordingArtifactResolver.class.getName()), logger.debugMessages);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void missingDelegateFailsWithClearError() throws Exception {
        ArtifactoryEclipseMetadataResolver decorator = new ArtifactoryEclipseMetadataResolver();
        // Only the decorator itself is registered under the role
        ContainerStub container = new ContainerStub(MetadataResolver.class, decorator);
        FieldUtils.writeField(decorator, "container", container.proxy(), true);
        FieldUtils.writeField(decorator, "helper", new StubHelper(true), true);

        decorator.resolveMetadata(null, Collections.singletonList(metadataRequest(Metadata.Nature.RELEASE)));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void failedLookupIsReported() throws Exception {
        ArtifactoryEclipseMetadataResolver decorator = new ArtifactoryEclipseMetadataResolver();
        ContainerStub container = new ContainerStub(MetadataResolver.class);
        container.failure = new ComponentLookupException("lookup failed", MetadataResolver.class.getName(), "default");
        FieldUtils.writeField(decorator, "container", container.proxy(), true);
        FieldUtils.writeField(decorator, "helper", new StubHelper(true), true);

        decorator.resolveMetadata(null, Collections.singletonList(metadataRequest(Metadata.Nature.RELEASE)));
    }

    private static ArtifactRequest artifactRequest() {
        return new ArtifactRequest(new DefaultArtifact("org.test:test:1.0"), Collections.singletonList(MAVEN_CENTRAL), null);
    }

    private static MetadataRequest metadataRequest(Metadata.Nature nature) {
        return metadataRequest("maven-metadata.xml", nature);
    }

    private static MetadataRequest metadataRequest(String type, Metadata.Nature nature) {
        return metadataRequest(type, nature, MAVEN_CENTRAL);
    }

    private static MetadataRequest metadataRequest(String type, Metadata.Nature nature, RemoteRepository repository) {
        return new MetadataRequest(new DefaultMetadata("org.test", "test", "1.0", type, nature), repository, null);
    }

    private static MetadataRequest artifactoryMetadataRequest(String groupId, String artifactId, String version, Metadata.Nature nature) {
        return new MetadataRequest(new DefaultMetadata(groupId, artifactId, version, "maven-metadata.xml", nature), ARTIFACTORY_RELEASE, null);
    }

    private static RemoteRepository repo(String id, String url) {
        return new RemoteRepository.Builder(id, "default", url).build();
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

    /**
     * Records the debug messages.
     */
    private static class RecordingLogger extends AbstractLogger {
        private final List<String> debugMessages = new ArrayList<>();

        RecordingLogger() {
            super(LEVEL_DEBUG, "test");
        }

        @Override
        public void debug(String message, Throwable throwable) {
            debugMessages.add(message);
        }

        @Override
        public void info(String message, Throwable throwable) {
        }

        @Override
        public void warn(String message, Throwable throwable) {
        }

        @Override
        public void error(String message, Throwable throwable) {
        }

        @Override
        public void fatalError(String message, Throwable throwable) {
        }

        @Override
        public Logger getChildLogger(String name) {
            return this;
        }
    }

    /**
     * A minimal PlexusContainer, which answers only lookupList(role).
     */
    private static class ContainerStub implements InvocationHandler {
        private final Class<?> role;
        private final List<Object> registered;
        private ComponentLookupException failure;
        private int lookups;

        ContainerStub(Class<?> role, Object... registered) {
            this.role = role;
            this.registered = Arrays.asList(registered);
        }

        PlexusContainer proxy() {
            return (PlexusContainer) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{PlexusContainer.class}, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (!"lookupList".equals(method.getName()) || args.length != 1 || args[0] != role) {
                throw new UnsupportedOperationException(method.getName());
            }
            lookups++;
            if (failure != null) {
                throw failure;
            }
            return registered;
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
        private Exception exception;

        @Override
        public List<MetadataResult> resolveMetadata(RepositorySystemSession session, Collection<? extends MetadataRequest> requests) {
            List<MetadataResult> results = new ArrayList<>();
            for (MetadataRequest request : requests) {
                this.requests.add(request);
                results.add(new MetadataResult(request).setMetadata(request.getMetadata()).setException(exception).setUpdated(true));
            }
            return results;
        }
    }
}
