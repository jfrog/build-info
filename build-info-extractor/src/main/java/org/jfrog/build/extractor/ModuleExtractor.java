package org.jfrog.build.extractor;

import org.jfrog.build.api.Module;

/**
 * Extracts a single module from the underlying build technology.
 * @param <C> the source object from the underlying build
 */
public interface ModuleExtractor<C> {
    /**
     * Extract a module from the source object.
     *
     * @param source the source object
     * @return A handle for the exported module
     */
    Module extractModule(C source);
}
