package org.jfrog.build.extractor.maven.resolver;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.util.List;

/**
 * Finds Maven's own implementation of a component role that the Artifactory extractor decorates.
 */
final class ResolverDelegateLookup {

    private ResolverDelegateLookup() {
    }

    /**
     * @param container Plexus container
     * @param role      the decorated role
     * @param self      the decorator. It is registered with the same role, so it is excluded from the result
     * @return the first implementation of the role other than the decorator
     */
    static <T> T lookupDelegate(PlexusContainer container, Class<T> role, T self) {
        try {
            List<T> candidates = container.lookupList(role);
            for (T candidate : candidates) {
                if (candidate != self && !candidate.getClass().getName().startsWith("org.jfrog.build.extractor.maven")) {
                    return candidate;
                }
            }
        } catch (ComponentLookupException e) {
            throw new IllegalStateException("Failed to look up Maven's " + role.getSimpleName(), e);
        }
        throw new IllegalStateException("Could not find Maven's " + role.getSimpleName());
    }
}
