package org.jfrog.build.extractor.maven.resolver;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.ExtensionRealmCache;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.internal.DefaultMavenPluginManager;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;

/**
 * @author yahavi
 */
@Singleton
@Named
public class ArtifactoryEclipsePluginManager extends DefaultMavenPluginManager {

    private final ArtifactoryEclipseResolversHelper helper;

    @Inject
    public ArtifactoryEclipsePluginManager(ArtifactoryEclipseResolversHelper helper) {
        this.helper = helper;
    }

    @Override
    public ExtensionRealmCache.CacheRecord setupExtensionsRealm(MavenProject project, Plugin plugin, RepositorySystemSession session) throws PluginManagerException {
        enforceResolutionRepositories(project, session);
        return super.setupExtensionsRealm(project, plugin, session);
    }

    private void enforceResolutionRepositories(MavenProject project, RepositorySystemSession session) {
        // Get the Artifactory repositories configured in the Artifactory plugin:
        List<ArtifactRepository> repositories = helper.getResolutionPluginRepositories(session);

        // The repositories list can be empty, in case this build is not running from a CI server.
        // In that case, we do not want to override Maven's configured repositories:
        if (repositories != null && !repositories.isEmpty()) {
            project.setPluginArtifactRepositories(repositories);
        }
    }
}
