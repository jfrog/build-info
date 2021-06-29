package org.jfrog.build.extractor.clientConfiguration.client.distribution.types;

import java.io.Serializable;
import java.util.List;

/**
 * @author yahavi
 **/
public class ReleaseBundleSpec implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<ReleaseBundleQuery> queries;

    public List<ReleaseBundleQuery> getQueries() {
        return queries;
    }

    public void setQueries(List<ReleaseBundleQuery> queries) {
        this.queries = queries;
    }
}
