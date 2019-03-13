package org.jfrog.build.extractor.scan;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author yahavi
 */
public class License {
    private static String UNKNOWN_LICENCE_FULL_NAME = "Unknown license";
    @SuppressWarnings("FieldCanBeLocal")
    private static String UNKNOWN_LICENCE_NAME = "Unknown";
    private List<String> components = new ArrayList<>();
    private String fullName;
    private String name;
    private List<String> moreInfoUrl = new ArrayList<>();

    public License() {
        this.fullName = UNKNOWN_LICENCE_FULL_NAME;
        this.name = UNKNOWN_LICENCE_NAME;
    }

    public License(List<String> components, String fullName, String name, List<String> moreInfoUrl) {
        this.components = components;
        this.fullName = StringUtils.trim(fullName);
        this.name = StringUtils.trim(name);
        this.moreInfoUrl = moreInfoUrl;
    }

    public License(License other) {
        this.components = other.components;
        this.fullName = StringUtils.trim(other.fullName);
        this.name = StringUtils.trim(other.name);
        this.moreInfoUrl = other.moreInfoUrl;
    }

    @SuppressWarnings("unused")
    public List<String> getComponents() {
        return components;
    }

    @SuppressWarnings("unused")
    public String getFullName() {
        return fullName;
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings("unused")
    public List<String> getMoreInfoUrl() {
        return moreInfoUrl;
    }

    @JsonIgnore
    @SuppressWarnings("unused")
    public boolean isFullNameEmpty() {
        return StringUtils.isBlank(fullName) || fullName.equals(License.UNKNOWN_LICENCE_FULL_NAME);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        License otherLicense = (License) other;
        return StringUtils.equals(fullName, otherLicense.fullName) && StringUtils.equals(name, otherLicense.name);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(fullName);
        result += Objects.hashCode(name);
        return result * 31;
    }

    @Override
    public String toString() {
        return this.name;
    }

}