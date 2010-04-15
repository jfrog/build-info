package org.jfrog.build.extractor;

/**
 * -DbuildInfo.property.name=value -DbuildInfo.deploy.property.name=value (becomes a matrix param in the current impl)
 * and - -DbuildInfo.propertiesFile=/path/to/file (contains the above properties without the buildInfo prefix)
 *
 * @author Noam Y. Tenne
 */
public interface BuildInfoExtractor<C, O> {

    /**
     * <ol> <li>Collect the props (from -D props and the props supplied in the {@link
     * org.jfrog.build.api.BuildInfoConfigProperties#PROP_PROPS_FILE} file.</li>
     * <p/>
     * <li>Collect published artifacts and dependency artifacts produced/used by the underlying build technology, based
     * on the context.</li>
     * <p/>
     * <li>Export the collected buildInfo according to the type of output O supplied (typically to a file)</li> </ol>
     *
     * @param context The build context (build technology specific)
     * @return A handle for the exported buildInfo (typically, the path of the exported file)
     */
    O extract(C context);
}
