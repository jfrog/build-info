package org.jfrog.build.extractor.maven.resolver;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.jfrog.build.extractor.maven.BuildInfoRecorder;

import java.lang.reflect.Field;

/**
 * Repository listener when running in Maven 3.1.x
 * The listener is used for updating the BuildInfoRecorder with each resolved artifact.
 *
 * @author Shay Yaakov
 */
@Component(role = RepositoryListener.class)
public class ArtifactoryEclipseRepositoryListener extends AbstractRepositoryListener implements Contextualizable {

    @Requirement
    private Logger logger;

    @Requirement
    private ResolutionHelper resolutionHelper;

    BuildInfoRecorder buildInfoRecorder = null;

    private PlexusContainer plexusContainer;

    Boolean artifactoryRepositoriesEnforced = false;
    private ArtifactoryEclipseArtifactResolver artifactoryResolver = null;

    /**
     * The method replaces the DefaultArtifactResolver instance with an instance of ArtifactoryEclipseArtifactResolver.
     * The new class sets the configured Artifactory resolution repositories for each resolved artifact.
     *
     * @throws ComponentLookupException
     */
    private void enforceArtifactoryResolver() throws ComponentLookupException {
        logger.debug("Enforcing Artifactory artifact resolver");

        DefaultArtifactDescriptorReader descriptorReader = (DefaultArtifactDescriptorReader)plexusContainer.lookup("org.eclipse.aether.impl.ArtifactDescriptorReader");
        org.eclipse.aether.internal.impl.DefaultRepositorySystem repositorySystem = (org.eclipse.aether.internal.impl.DefaultRepositorySystem)plexusContainer.lookup("org.eclipse.aether.RepositorySystem");

        org.eclipse.aether.impl.ArtifactResolver artifactoryResolver = (org.eclipse.aether.impl.ArtifactResolver)plexusContainer.lookup("org.jfrog.build.extractor.maven.resolver.ArtifactoryEclipseArtifactResolver");

        this.artifactoryResolver = (ArtifactoryEclipseArtifactResolver)artifactoryResolver;

        descriptorReader.setArtifactResolver(artifactoryResolver);
        repositorySystem.setArtifactResolver(artifactoryResolver);

        artifactoryRepositoriesEnforced = true;
        synchronized (artifactoryRepositoriesEnforced) {
            artifactoryRepositoriesEnforced.notifyAll();
        }
    }

    private BuildInfoRecorder getBuildInfoRecorder() {
        if (buildInfoRecorder == null) {
            try {
                buildInfoRecorder = (BuildInfoRecorder)plexusContainer.lookup(BuildInfoRecorder.class.getName());
            } catch (ComponentLookupException e) {
                logger.error("Failed while trying to fetch BuildInfoRecorder from the container in " + this.getClass().getName(), e);
            }
            if (buildInfoRecorder == null) {
                logger.error("Could not fetch BuildInfoRecorder from the container in " + this.getClass().getName() + ". Artifacts resolution cannot be recorded.");
            }
        }
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

    /**
     * The enforceArtifactoryResolver() method replaces the default artifact resolver instance with a resolver that enforces Artifactory
     * resolution repositories. However, since there's a chance that Maven started resolving a few artifacts before the instance replacement,
     * thsi method makes sure those artifacts will be resolved from Artifactory as well.
     * @param event
     */
    private void verifyArtifactoryResolutionEnforced(RepositoryEvent event) {
        if (!(event.getRepository() instanceof RemoteRepository)) {
            return;
        }
        RemoteRepository repo = (RemoteRepository)event.getRepository();

        // In case the Artifactory resolver is not yet set, we wait for it first:
        if (!artifactoryRepositoriesEnforced) {
            synchronized (artifactoryRepositoriesEnforced) {
                if (!artifactoryRepositoriesEnforced) {
                    try {
                        artifactoryRepositoriesEnforced.wait();
                    } catch (InterruptedException e) {
                        logger.error("Failed while waiting for Artifactory repositories enforcement", e);
                    }
                }
            }
        }

        // Now that the resolver enforcement is done, we make sure that the Artifactory resolution repositories in the resolver are initialized:
        artifactoryResolver.initResolutionRepositories(event.getSession());

        // Take the Artifactory resolution repositories from the Artifactory resolver:
        RemoteRepository artifactorySnapshotRepo = artifactoryResolver.getSnapshotRepository(event.getSession());
        RemoteRepository artifactoryReleaseRepo = artifactoryResolver.getReleaseRepository(event.getSession());

        // If the artifact about to be downloaded was not handled by the Artifactory resolution resolver, but by the default resolver (before
        // it had been replaced), modify the repository URL:
        try {
            if (event.getArtifact().isSnapshot() && repo != artifactorySnapshotRepo) {
                logger.debug("Replacing resolution repository URL: " + repo + " with: " + artifactorySnapshotRepo.getUrl());
                Field url = RemoteRepository.class.getDeclaredField("url");
                url.setAccessible(true);
                url.set(repo, artifactorySnapshotRepo.getUrl());
                setRepositoryPolicy(repo);
            } else
            if (!event.getArtifact().isSnapshot() && repo != artifactoryReleaseRepo) {
                logger.debug("Replacing resolution repository URL: " + repo + " with: " + artifactoryReleaseRepo.getUrl());
                Field url = RemoteRepository.class.getDeclaredField("url");
                url.setAccessible(true);
                url.set(repo, artifactoryReleaseRepo.getUrl());
                setRepositoryPolicy(repo);
            }
        } catch (Exception e) {
            logger.error("Failed while replacing resolution repository URL", e);
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
        String requestContext = ((ArtifactRequest)event.getTrace().getData()).getRequestContext();
        String scope = resolutionHelper.getScopeByRequestContext(requestContext);
        org.apache.maven.artifact.Artifact artifact = toMavenArtifact(event.getArtifact(), scope);
        if (event.getRepository() != null) {
            logger.debug("[buildinfo] Resolved artifact: " + artifact + " from: " + event.getRepository() + " Context is: " + requestContext);

            if (getBuildInfoRecorder() != null) {
                getBuildInfoRecorder().artifactResolved(artifact);
            }
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
        DefaultArtifact artifact = new DefaultArtifact(art.getGroupId(), art.getArtifactId(), art.getVersion(), scope, art.getExtension(), "", null) {
            public boolean equals( Object o ) {
                if (o == this) {
                    return true;
                }
                if (!(o instanceof org.apache.maven.artifact.Artifact)) {
                    return false;
                }
                return hashCode() == o.hashCode();
            }
        };
        artifact.setFile(art.getFile());
        return artifact;
    }

    @Override
    public void contextualize(Context context) throws ContextException {
        plexusContainer = (PlexusContainer)context.get(PlexusConstants.PLEXUS_KEY);
        try {
            enforceArtifactoryResolver();
        } catch (Exception e) {
            logger.error("Failed while enforcing Artifactory artifact resolver", e);
        }
    }
}
