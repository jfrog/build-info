package org.jfrog.build.extractor.maven.resolver;

import org.apache.commons.lang3.exception.ExceptionUtils;
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

    // Non-virtual handle to the parent's prerequisites-check method, whichever name it
    // currently uses. Maven 3.9.12 renamed checkRequiredMavenVersion -> checkPrerequisites
    // and kept the old name as a deprecated default method on the MavenPluginManager interface
    // that internally delegates to the new name. We must therefore look up the new name first:
    // on 3.9.12+ that hits the real implementation directly. Falling through to the old name
    // is only used on Maven < 3.9.12, where the new name does not exist and the old name is
    // a real method on DefaultMavenPluginManager - so the check still runs.
    // If neither exists, the handle stays null and the override below is a safe no-op.
    private static final MethodHandle SUPER_PREREQUISITES_CHECK = resolveSuperPrerequisitesCheck();

    private static MethodHandle resolveSuperPrerequisitesCheck() {
        MethodHandle handle = findSuperMethod("checkPrerequisites");
        if (handle != null) {
            return handle;
        }
        return findSuperMethod("checkRequiredMavenVersion");
    }

    private static MethodHandle findSuperMethod(String name) {
        try {
            return MethodHandles.lookup().findSpecial(
                    DefaultMavenPluginManager.class,
                    name,
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

    // Declared so Class.getDeclaredMethod on this subclass finds it -
    // getDeclaredMethod does not walk the superclass chain. Delegates to the parent's
    // prerequisites-check method via a MethodHandle resolved at class-load time, falling back
    // to the older name (checkRequiredMavenVersion) on Maven < 3.9.12 so the check still runs.
    @Override
    public void checkPrerequisites(PluginDescriptor pluginDescriptor) throws PluginIncompatibleException {
        if (SUPER_PREREQUISITES_CHECK == null) {
            return;
        }
        try {
            SUPER_PREREQUISITES_CHECK.invoke(this, pluginDescriptor);
        } catch (Throwable t) {
            ExceptionUtils.rethrow(t);
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
