package org.jfrog.build.extractor;

import org.jfrog.build.api.Build;

/**
 * -DbuildInfo.property.name=value -DbuildInfo.deploy.property.name=value (becomes a matrix param in the current impl)
 * and - -DbuildInfo.propertiesFile=/path/to/file (contains the above properties without the buildInfo prefix)
 *
 * @author Noam Y. Tenne
 */
public interface BuildInfoExtractor<C> {

    /**
     * Collect the props (from -D props and the props supplied in the {@link org.jfrog.build.api.constants.BuildInfoProperties.PROP_PROPS_FILE})
     * file.
     *
     * @param context
     */
    void collectProperties(C context);

    /**
     * Collect published artifacts and dependency artifacts produced/used by the underlying build technology.
     *
     * @param context
     */
    void collectModules(C context);

    /**
     * Export the collected buildInfo (typically to a file)
     *
     * @param build
     * @return A stringified handle of where to locate the exported buildInfo (typically, the path of the exported
     *         file)
     */
    void export(Build build);
}
