package org.jfrog.build.extractor.maven;

import java.util.Properties;

/**
 * @author Noam Y. Tenne
 */
public abstract class AbstractPropertyResolver<R> {

    public abstract R resolveProperties(Properties properties);
}
