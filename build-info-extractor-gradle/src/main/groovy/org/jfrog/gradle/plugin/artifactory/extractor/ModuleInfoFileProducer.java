package org.jfrog.gradle.plugin.artifactory.extractor;

import org.gradle.api.file.FileCollection;

import java.io.File;

/**
 * Represents a producer of ModuleInfo files
 */
public interface ModuleInfoFileProducer {
    /**
     * Whether the module info file will actually contain modules or not.
     *
     * @return true if the module info file will contain modules
     */
    boolean hasModules();

    /**
     * Get the module info file.
     *
     * @return the module info file
     */
    FileCollection getModuleInfoFiles();
}
