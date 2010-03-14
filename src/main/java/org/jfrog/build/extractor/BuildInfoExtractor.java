package org.jfrog.build.extractor;

import org.jfrog.build.api.Build;

/**
 * @author Noam Y. Tenne
 */
public interface BuildInfoExtractor<C> {

    void collectProperties(C context);

    void collectModules(C context);

    void export(Build build);
}
