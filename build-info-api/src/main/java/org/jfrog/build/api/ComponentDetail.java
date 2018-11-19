package org.jfrog.build.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by romang on 6/1/17.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComponentDetail {

    @JsonProperty("component_id")
    private String componentId;
    private List<String> scopes = new ArrayList<>();

    public ComponentDetail(String componentId) {
        this.componentId = componentId;
    }

    @JsonProperty("component_id")
    public String getComponentId() {
        return componentId;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void addScopes(List<String> scopes) {
        this.scopes.addAll(scopes);
    }

    @Override
    public String toString() {
        return componentId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        return StringUtils.equals(toString(), obj.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(componentId);
    }
}
