package org.jfrog.build.extractor;

import org.jfrog.build.extractor.ci.BuildInfo;

/**
 * -DbuildInfo.property.name=value -DbuildInfo.deploy.property.name=value (becomes a matrix param in the current impl)
 * and - -DbuildInfo.propertiesFile=/path/to/file (contains the above properties without the buildInfo prefix)
 *
 * @author Noam Y. Tenne
 */
public interface BuildInfoExtractor<C> {

    /**
     * <ol> <li>Collect the props (from -D props and the props supplied in the {@link
     * org.jfrog.build.api.BuildInfoConfigProperties#PROP_PROPS_FILE} file.</li>
     *
     * <li>Collect published artifacts and dependency artifacts produced/used by the underlying build technology, based
     * on the context.</li> </ol>
     *
     * @param buildInfoTask
     * @return A handle for the exported buildInfo
     */
    BuildInfo extract(C buildInfoTask) throws Exception;
}
