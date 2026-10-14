package org.jfrog.build.extractor.maven.resolver;

import org.apache.maven.artifact.DefaultArtifact;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.jfrog.build.extractor.maven.BuildInfoRecorder;

/**
 * Repository listener that updates the BuildInfoRecorder with each resolved artifact.
 * <p>
 * The enforcement of the Artifactory resolution repositories is done by {@link ArtifactoryEclipseArtifactResolver} and
 * {@link ArtifactoryEclipseMetadataResolver}, which decorate Maven's own resolvers.
 *
 * @author Shay Yaakov
 */
@Component(role = RepositoryListener.class)
public class ArtifactoryEclipseRepositoryListener extends AbstractRepositoryListener {

    @Requirement
    private BuildInfoRecorder buildInfoRecorder;

    @Requirement
    private Logger logger;

    private BuildInfoRecorder getBuildInfoRecorder() {
        return buildInfoRecorder;
    }

    /**
     * Intercepts resolved artifacts and updates the BuildInfoRecorder, so that build-info includes all resolved artifacts.
     */
    @Override
    public void artifactResolved(RepositoryEvent event) {
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
