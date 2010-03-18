package org.jfrog.build.extractor;

import org.jfrog.build.api.Build;


/**
 * @author Noam Y. Tenne
 */
public abstract class BuildInfoExtractorSupport<C> implements BuildInfoExtractor<C> {

    /**
     * {@inheritDoc}
     */
    public void collectProperties(C context) {
    }

    /**
     * {@inheritDoc}
     */
    public void export(Build build) {
        throw new UnsupportedOperationException("Implement me");
    }
}