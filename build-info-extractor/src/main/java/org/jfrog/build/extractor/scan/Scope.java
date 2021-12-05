package org.jfrog.build.extractor.scan;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * @author yahavi
 */
public class Scope {
    private static final String NONE_SCOPE = "None";

    private final String name;

    @SuppressWarnings("unused")
    public Scope() {
        name = NONE_SCOPE;
    }

    public Scope(String name) {
        this.name = StringUtils.capitalize(StringUtils.trim(name));
    }

    public String getName() {
        return name;
    }

    @JsonIgnore
    @SuppressWarnings("unused")
    public boolean isEmpty() {
        return StringUtils.isBlank(name) || name.equals(Scope.NONE_SCOPE);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return StringUtils.equals(toString(), other.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return this.name;
    }

}
