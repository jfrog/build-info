package org.jfrog.build.extractor.clientConfiguration.util;

import org.jfrog.build.api.Dependency;

import java.io.IOException;
import java.util.List;

/**
 * Created by Tamirh on 25/04/2016.
 */
public interface DependenciesHelper {

    List<Dependency> retrievePublishedDependencies(String resolvePattern, String[] excludePatterns, boolean explode) throws IOException, InterruptedException;

    void setFlatDownload(boolean flat);
}
