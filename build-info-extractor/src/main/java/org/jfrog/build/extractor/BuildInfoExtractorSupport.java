package org.jfrog.build.extractor;

/**
 * @author Noam Y. Tenne
 */
public abstract class BuildInfoExtractorSupport<C, O> implements BuildInfoExtractor<C, O> {

    public O extract(C context) {
        throw new UnsupportedOperationException("Implement me");
    }
}