package org.jfrog.build.extractor.clientConfiguration.client.distribution.request;

import org.jfrog.build.extractor.clientConfiguration.client.distribution.types.ReleaseBundleQuery;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.types.ReleaseBundleSpec;
import org.jfrog.filespecs.DistributionHelper;
import org.jfrog.filespecs.FileSpec;
import org.jfrog.filespecs.distribution.DistributionSpecComponent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yahavi
 **/
public class Utils {

    /**
     * Create a release bundle spec from a File Spec string.
     *
     * @param fileSpec - File Spec
     * @return release bundle spec.
     * @throws IOException if the input File Spec is invalid.
     */
    public static ReleaseBundleSpec createReleaseBundleSpec(String fileSpec) throws IOException {
        return createReleaseBundleSpec(FileSpec.fromString(fileSpec));
    }

    /**
     * Create a release bundle spec from a File Spec object.
     *
     * @param fileSpec - File Spec
     * @return release bundle spec.
     * @throws IOException if the input File Spec is invalid.
     */
    public static ReleaseBundleSpec createReleaseBundleSpec(FileSpec fileSpec) throws IOException {
        List<ReleaseBundleQuery> queries = new ArrayList<>();
        List<DistributionSpecComponent> specComponents = DistributionHelper.toSpecComponents(fileSpec);
        for (DistributionSpecComponent specComponent : specComponents) {
            ReleaseBundleQuery query = new ReleaseBundleQuery();
            query.setAql(specComponent.getAql());
            query.setAddedProps(specComponent.getAddedProps());
            query.setMappings(specComponent.getMappings());
            queries.add(query);
        }
        ReleaseBundleSpec results = new ReleaseBundleSpec();
        results.setQueries(queries);
        return results;
    }
}
