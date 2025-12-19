package org.jfrog.build.extractor.maven.resolver;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.plugin.internal.DefaultPluginDependenciesResolver;
import org.apache.maven.project.DefaultProjectDependenciesResolver;
import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.internal.impl.DefaultRepositorySystem;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.jfrog.build.extractor.maven.BuildInfoRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Repository listener when running in Maven 3.1.x
 * The listener is used for updating the BuildInfoRecorder with each resolved artifact.
 *
 * @author Shay Yaakov
 */
@Singleton
@Named
public class ArtifactoryEclipseRepositoryListener extends AbstractRepositoryListener {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DefaultProjectDependenciesResolver pojectDependenciesResolver;
    private final DefaultPluginDependenciesResolver pluginDependenciesResolver;
    private final ArtifactoryEclipseArtifactResolver artifactResolver;
    private final ArtifactoryEclipseMetadataResolver metadataResolver;
    private final DefaultArtifactDescriptorReader descriptorReader;
    private final DefaultRepositorySystem repositorySystem;
    private final BuildInfoRecorder buildInfoRecorder;

    private final AtomicBoolean artifactoryRepositoriesEnforced = new AtomicBoolean(false);

    @Inject
    public ArtifactoryEclipseRepositoryListener(DefaultProjectDependenciesResolver pojectDependenciesResolver,
                                                DefaultPluginDependenciesResolver pluginDependenciesResolver,
                                                ArtifactoryEclipseArtifactResolver artifactResolver,
                                                ArtifactoryEclipseMetadataResolver metadataResolver,
                                                DefaultArtifactDescriptorReader descriptorReader,
                                                DefaultRepositorySystem repositorySystem,
                                                BuildInfoRecorder buildInfoRecorder) {
        this.pojectDependenciesResolver = pojectDependenciesResolver;
        this.pluginDependenciesResolver = pluginDependenciesResolver;
        this.artifactResolver = artifactResolver;
        this.metadataResolver = metadataResolver;
        this.descriptorReader = descriptorReader;
        this.repositorySystem = repositorySystem;
        this.buildInfoRecorder = buildInfoRecorder;

        try {
            enforceArtifactoryResolver();
        } catch (Exception e) {
            logger.error("Failed while enforcing Artifactory artifact resolver", e);
        }
    }

    /**
     * The method replaces the DefaultArtifactResolver instance with an instance of ArtifactoryEclipseArtifactResolver.
     * The new class sets the configured Artifactory resolution repositories for each resolved artifact.
     */
    private void enforceArtifactoryResolver() throws NoSuchFieldException, IllegalAccessException {
        logger.debug("Enforcing Artifactory artifact resolver");

        descriptorReader.setArtifactResolver(artifactResolver);
        repositorySystem.setArtifactResolver(artifactResolver);
        repositorySystem.setMetadataResolver(metadataResolver);

        Field repoSystemProjectField = pojectDependenciesResolver.getClass().getDeclaredField("repoSystem");
        repoSystemProjectField.setAccessible(true);
        repoSystemProjectField.set(pojectDependenciesResolver, repositorySystem);

        Field repoSystemPluginField = pluginDependenciesResolver.getClass().getDeclaredField("repoSystem");
        repoSystemPluginField.setAccessible(true);
        repoSystemPluginField.set(pluginDependenciesResolver, repositorySystem);

        artifactoryRepositoriesEnforced.set(true);
        synchronized (artifactoryRepositoriesEnforced) {
            artifactoryRepositoriesEnforced.notifyAll();
        }
    }

    private BuildInfoRecorder getBuildInfoRecorder() {
        return buildInfoRecorder;
    }

    @Override
    public void metadataDownloading(RepositoryEvent event) {
        verifyArtifactoryResolutionEnforced(event);
    }

    @Override
    public void artifactDownloading(RepositoryEvent event) {
        verifyArtifactoryResolutionEnforced(event);
    }

    private void waitForResolutionToBeSet() {
        // In case the Artifactory resolver is not yet set, we wait for it first:
        if (!artifactoryRepositoriesEnforced.get()) {
            synchronized (artifactoryRepositoriesEnforced) {
                if (!artifactoryRepositoriesEnforced.get()) {
                    try {
                        artifactoryRepositoriesEnforced.wait();
                    } catch (InterruptedException e) {
                        logger.error("Failed while waiting for Artifactory repositories enforcement", e);
                    }
                }
            }
        }
    }

    /**
     * The enforceArtifactoryResolver() method replaces the default artifact resolver instance with a resolver that enforces Artifactory
     * resolution repositories. However, since there's a chance that Maven started resolving a few artifacts before the instance replacement,
     * this method makes sure those artifacts will be resolved from Artifactory as well.
     */
    private void verifyArtifactoryResolutionEnforced(RepositoryEvent event) {
        initResolutionHelper(event.getSession());
        if (!getBuildInfoRecorder().getResolutionHelper().resolutionRepositoriesConfigured()) {
            return;
        }
        if (event.getArtifact() == null && event.getMetadata() == null) {
            return;
        }
        if (!(event.getRepository() instanceof RemoteRepository)) {
            return;
        }

        RemoteRepository repo = (RemoteRepository) event.getRepository();

        waitForResolutionToBeSet();

        // Now that the resolver enforcement is done, we make sure that the Artifactory resolution repositories in the resolver are initialized:
        artifactResolver.initResolutionRepositories(event.getSession());

        // Take the Artifactory resolution repositories from the Artifactory resolver:
        RemoteRepository artifactorySnapshotRepo;
        RemoteRepository artifactoryReleaseRepo;
        boolean snapshot;
        if (event.getArtifact() != null) {
            artifactorySnapshotRepo = artifactResolver.getSnapshotRepository(event.getSession());
            artifactoryReleaseRepo = artifactResolver.getReleaseRepository(event.getSession());
            snapshot = event.getArtifact().isSnapshot();
        } else {
            artifactorySnapshotRepo = metadataResolver.getSnapshotRepository(event.getSession());
            artifactoryReleaseRepo = metadataResolver.getReleaseRepository(event.getSession());
            snapshot = event.getMetadata().getNature() == Metadata.Nature.SNAPSHOT;
        }

        // If the artifact about to be downloaded was not handled by the Artifactory resolution resolver, but by the default resolver (before
        // it had been replaced), modify the repository URL:
        try {
            if (snapshot && !repo.getUrl().equals(artifactorySnapshotRepo.getUrl()) && repo.getPolicy(true).isEnabled()) {
                logger.debug("Replacing resolution repository URL: " + repo + " with: " + artifactorySnapshotRepo.getUrl());
                copyRepositoryFields(artifactorySnapshotRepo, repo);
                setRepositoryPolicy(repo);
            } else if (!snapshot && !repo.getUrl().equals(artifactoryReleaseRepo.getUrl()) && repo.getPolicy(false).isEnabled()) {
                logger.debug("Replacing resolution repository URL: " + repo + " with: " + artifactoryReleaseRepo.getUrl());
                copyRepositoryFields(artifactoryReleaseRepo, repo);
                setRepositoryPolicy(repo);
            }
        } catch (Exception e) {
            logger.error("Failed while replacing resolution repository URL", e);
        }
    }

    private void initResolutionHelper(RepositorySystemSession session) {
        ResolutionHelper helper = getBuildInfoRecorder().getResolutionHelper();
        if (helper.isInitialized()) {
            return;
        }
        Properties allMavenProps = new Properties();
        allMavenProps.putAll(session.getSystemProperties());
        allMavenProps.putAll(session.getUserProperties());
        helper.init(allMavenProps);
    }

    private void copyRepositoryFields(RemoteRepository fromRepo, RemoteRepository toRepo)
            throws IllegalAccessException, NoSuchFieldException {
        Field url = RemoteRepository.class.getDeclaredField("url");
        url.setAccessible(true);
        url.set(toRepo, fromRepo.getUrl());
        if (fromRepo.getAuthentication() != null) {
            Field authentication = RemoteRepository.class.getDeclaredField("authentication");
            authentication.setAccessible(true);
            authentication.set(toRepo, fromRepo.getAuthentication());
        }
        if (fromRepo.getProxy() != null) {
            Field proxy = RemoteRepository.class.getDeclaredField("proxy");
            proxy.setAccessible(true);
            proxy.set(toRepo, fromRepo.getProxy());
        }
    }

    /**
     * Enables both snapshot and release polocies for a repository
     */
    private void setRepositoryPolicy(RemoteRepository repo) throws NoSuchFieldException, IllegalAccessException {
        RepositoryPolicy policy = new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN);

        Field releasePolicyField = RemoteRepository.class.getDeclaredField("releasePolicy");
        Field snapshotPolicyField = RemoteRepository.class.getDeclaredField("snapshotPolicy");
        releasePolicyField.setAccessible(true);
        snapshotPolicyField.setAccessible(true);
        releasePolicyField.set(repo, policy);
        snapshotPolicyField.set(repo, policy);
    }

    /**
     * Intercepts resolved artifacts and updates the BuildInfoRecorder, so that build-info includes all resolved artifacts.
     */
    @Override
    public void artifactResolved(RepositoryEvent event) {
        waitForResolutionToBeSet();

        String requestContext = ((ArtifactRequest) event.getTrace().getData()).getRequestContext();
        String scope = getBuildInfoRecorder().getResolutionHelper().getScopeByRequestContext(requestContext);
        org.apache.maven.artifact.Artifact artifact = toMavenArtifact(event.getArtifact(), scope);
        if (event.getRepository() != null) {
            logger.debug("[buildinfo] Resolved artifact: " + artifact + " from: " + event.getRepository() + " Context is: " + requestContext);
            getBuildInfoRecorder().artifactResolved(artifact);
        } else {
            logger.debug("[buildinfo] Could not resolve artifact: " + artifact);
        }
        super.artifactResolved(event);
    }

    /**
     * Converts org.eclipse.aether.artifact.Artifact objects into org.apache.maven.artifact.Artifact objects.
     */
    private org.apache.maven.artifact.Artifact toMavenArtifact(final org.eclipse.aether.artifact.Artifact art, String scope) {
        if (art == null) {
            return null;
        }
        String classifier = art.getClassifier();
        classifier = classifier == null ? "" : classifier;
        DefaultArtifact artifact = new DefaultArtifact(art.getGroupId(), art.getArtifactId(), art.getVersion(), scope, art.getExtension(), classifier, null);
        artifact.setFile(art.getFile());
        return artifact;
    }
}