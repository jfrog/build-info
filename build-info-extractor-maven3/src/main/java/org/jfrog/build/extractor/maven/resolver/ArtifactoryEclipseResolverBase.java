package org.jfrog.build.extractor.maven.resolver;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import java.util.Collections;
import java.util.List;

/**
 * Base class of the Artifactory resolvers, which decorate Maven's own resolvers.
 * <p>
 * The decorators implement the stable Aether interfaces, instead of extending Maven's internal resolver classes,
 * since the constructors of those classes change between Maven versions (for example, Maven 3.10 does not have a default constructor).
 *
 * @param <T> the decorated Aether interface
 */
abstract class ArtifactoryEclipseResolverBase<T> {

    @Requirement
    private PlexusContainer container;

    @Requirement
    private Logger logger;

    @Requirement
    ArtifactoryEclipseResolversHelper helper;

    private final Class<T> role;

    private volatile T delegate;

    ArtifactoryEclipseResolverBase(Class<T> role) {
        this.role = role;
    }

    /**
     * Get the Artifactory repositories configured in the Artifactory plugin.
     * The list is empty in case this build is not running from a CI server. In that case, we do not want to override Maven's configured repositories.
     */
    List<RemoteRepository> getResolutionRepositories(RepositorySystemSession session) {
        List<RemoteRepository> repositories = helper.getResolutionRepositories(session);
        return repositories == null ? Collections.<RemoteRepository>emptyList() : repositories;
    }

    /**
     * Get Maven's own implementation of the decorated role. The decorator is registered with the same role as Maven's implementation, so the
     * look-up can't be a plain requirement - it would resolve to the decorator. The look-up is lazy to avoid circular dependencies,
     * and idempotent, so two threads that look it up at the same time get the same delegate. The field is volatile to publish it safely.
     */
    T getDelegate() {
        if (delegate == null) {
            delegate = lookupDelegate();
        }
        return delegate;
    }

    private T lookupDelegate() {
        try {
            for (T candidate : container.lookupList(role)) {
                if (!(candidate instanceof ArtifactoryEclipseResolverBase)) {
                    // Shows whether the delegate is Maven's own resolver, or one of another extension
                    logger.debug("Decorating " + role.getSimpleName() + ": " + candidate.getClass().getName());
                    return candidate;
                }
            }
        } catch (ComponentLookupException e) {
            throw new IllegalStateException("Failed to look up Maven's " + role.getSimpleName(), e);
        }
        throw new IllegalStateException("Could not find Maven's " + role.getSimpleName());
    }
}
