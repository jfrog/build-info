package org.jfrog.build.extractor.maven.resolver;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.ExtensionRealmCache;
import org.apache.maven.plugin.PluginIncompatibleException;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.internal.DefaultMavenPluginManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.aether.RepositorySystemSession;

import javax.inject.Named;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

/**
 * @author yahavi
 */
@Named
@Component(role = DefaultMavenPluginManager.class)
public class ArtifactoryEclipsePluginManager extends DefaultMavenPluginManager {

    // Non-virtual handle to DefaultMavenPluginManager#checkPrerequisites if and only if the
    // parent class declares it (Maven >= 3.9.12). On older Maven runtimes the method does not
    // exist on the parent, the handle stays null, and the override below is a safe no-op.
    private static final MethodHandle SUPER_CHECK_PREREQUISITES = resolveSuperCheckPrerequisites();

    private static MethodHandle resolveSuperCheckPrerequisites() {
        try {
            return MethodHandles.lookup().findSpecial(
                    DefaultMavenPluginManager.class,
                    "checkPrerequisites",
                    MethodType.methodType(void.class, PluginDescriptor.class),
                    ArtifactoryEclipsePluginManager.class);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            return null;
        }
    }

    @Requirement
    private ArtifactoryEclipseResolversHelper helper;

    @Override
    public ExtensionRealmCache.CacheRecord setupExtensionsRealm(MavenProject project, Plugin plugin, RepositorySystemSession session) throws PluginManagerException {
        enforceResolutionRepositories(project, session);
        return super.setupExtensionsRealm(project, plugin, session);
    }

    // Declared so Class.getDeclaredMethod on this subclass finds it —
    // getDeclaredMethod does not walk the superclass chain. Delegates to the parent via a
    // MethodHandle resolved at class-load time so the bytecode does not embed a hard reference
    // to a method that may not exist on older Maven runtimes (3.8.x / 3.9.0 - 3.9.11).
    @Override
    public void checkPrerequisites(PluginDescriptor pluginDescriptor) throws PluginIncompatibleException {
        MethodHandle handle = SUPER_CHECK_PREREQUISITES;
        if (handle == null) {
            return;
        }
        try {
            handle.invoke(this, pluginDescriptor);
        } catch (PluginIncompatibleException | RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
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
