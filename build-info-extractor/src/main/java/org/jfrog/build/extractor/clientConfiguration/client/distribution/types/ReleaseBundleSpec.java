package org.jfrog.build.extractor.clientConfiguration.client.distribution.types;

import java.util.List;

/**
 * @author yahavi
 **/
public class ReleaseBundleSpec {
    private List<ReleaseBundleQuery> queries;

    public List<ReleaseBundleQuery> getQueries() {
        return queries;
    }

    public void setQueries(List<ReleaseBundleQuery> queries) {
        this.queries = queries;
    }
}
