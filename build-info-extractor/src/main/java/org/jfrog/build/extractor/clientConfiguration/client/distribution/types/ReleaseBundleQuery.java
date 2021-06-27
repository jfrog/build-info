package org.jfrog.build.extractor.clientConfiguration.client.distribution.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jfrog.filespecs.distribution.PathMapping;
import org.jfrog.filespecs.properties.Property;

import java.util.List;

/**
 * @author yahavi
 **/
@SuppressWarnings("unused")
public class ReleaseBundleQuery {
    private List<PathMapping> mappings;
    @JsonProperty("added_props")
    private List<Property> addedProps;
    @JsonProperty("query_name")
    private String queryName;
    private String aql;

    public List<PathMapping> getMappings() {
        return mappings;
    }

    public void setMappings(List<PathMapping> mappings) {
        this.mappings = mappings;
    }

    public List<Property> getAddedProps() {
        return addedProps;
    }

    public void setAddedProps(List<Property> addedProps) {
        this.addedProps = addedProps;
    }

    public String getQueryName() {
        return queryName;
    }

    public void setQueryName(String queryName) {
        this.queryName = queryName;
    }

    public String getAql() {
        return aql;
    }

    public void setAql(String aql) {
        this.aql = aql;
    }
}