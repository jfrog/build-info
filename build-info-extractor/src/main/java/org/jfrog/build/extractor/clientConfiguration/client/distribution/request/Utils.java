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

    public static ReleaseBundleSpec createSpec(String spec) throws IOException {
        return createSpec(FileSpec.fromString(spec));
    }

    public static ReleaseBundleSpec createSpec(FileSpec fileSpec) throws IOException {
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
