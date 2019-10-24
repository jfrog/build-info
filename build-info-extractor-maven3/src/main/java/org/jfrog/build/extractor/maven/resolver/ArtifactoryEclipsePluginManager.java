package org.jfrog.build.extractor.maven.resolver;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.ExtensionRealmCache;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.internal.DefaultMavenPluginManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.aether.RepositorySystemSession;

import javax.inject.Named;
import java.util.List;

/**
 * @author yahavi
 */
@Named
@Component(role = DefaultMavenPluginManager.class)
public class ArtifactoryEclipsePluginManager extends DefaultMavenPluginManager {

    @Requirement
    private ArtifactoryEclipseResolversHelper helper;

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
