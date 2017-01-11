package org.jfrog.build.client.artifactoryXrayResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.List;

/**
 * Used for serialization of Xray scanning results
 */
public class License implements Serializable {
    private final static long serialVersionUID = -4743362963485868205L;
    private String name;
    private List<String> components = null;
    @JsonProperty("full_name")
    private String fullName;
    @JsonProperty("more_info_url")
    private List<String> moreInfoUrl = null;

    /**
     * No args constructor for use in serialization
     */
    public License() {
    }

    public License(String name, String fullName, List<String> components, List<String> moreInfoUrl) {
        this.name = name;
        this.fullName = fullName;
        this.components = components;
        this.moreInfoUrl = moreInfoUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("full_name")
    public String getFullName() {
        return fullName;
    }

    @JsonProperty("full_name")
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public List<String> getComponents() {
        return components;
    }

    public void setComponents(List<String> components) {
        this.components = components;
    }

    @JsonProperty("more_info_url")
    public List<String> getMoreInfoUrl() {
        return moreInfoUrl;
    }

    @JsonProperty("more_info_url")
    public void setMoreInfoUrl(List<String> moreInfoUrl) {
        this.moreInfoUrl = moreInfoUrl;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}

