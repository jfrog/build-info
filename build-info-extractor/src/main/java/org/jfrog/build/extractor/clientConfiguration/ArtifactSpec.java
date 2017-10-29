/*
 * Copyright (C) 2011 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.build.extractor.clientConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Container for artifact with module information + custom properties
 *
 * @author Yoav Landman
 */
public class ArtifactSpec {

    public static final String CONFIG_ALL = "all";
    public static final String WILDCARD = "*";
    private static final Pattern ARTIFACT_NOTATION =
            Pattern.compile("^([^:]+):([^:]+):([^:]+):([^:]+?)(?:\\@([^:]+$)){0,1}$");
    private final Map<String, CharSequence> properties = new HashMap<String, CharSequence>();
    private String group;
    private String name;
    private String version;
    private String classifier;
    private String type;
    private String configuration;

    private ArtifactSpec() {
    }

    /**
     * Create a full artifact spec from string notation in the format of:
     *
     * [configName] artifactNotation key1:val1, key2:val2, key3:val3
     *
     * artifactNotation is in the format of group:artifact:version:classifier@ext every<br> Any element in
     * artifactNotation can contain the * and ? wildcards, for example:
     *
     * org.acme:*:1.0.?_*:*@tgz
     *
     * @param notation
     */
    public static ArtifactSpec newSpec(String notation) {
        String[] splits = notation.split("\\s+", 3);

        ArtifactSpec spec = null;
        if (splits.length >= 2) {
            Builder builder = builder();
            if (splits[1].contains(",")) {
                //No configuration supplied
                builder.configuration(WILDCARD);
                builder.artifactNotation(splits[0]);
                builder.properties(notation.substring(notation.indexOf(splits[1])));
                spec = builder.build();
            } else if (splits.length == 3) {
                //Convert 'any' config to a wild card
                builder.configuration(splits[0].equalsIgnoreCase(CONFIG_ALL) ? WILDCARD : splits[0]);
                builder.artifactNotation(splits[1]);
                builder.properties(splits[2]);
                spec = builder.build();
            }
        }
        if (spec == null) {
            throw new IllegalArgumentException(
                    "Artifact spec notation '" + notation + "' is wrong.\nExpecting notation in the format of: " +
                            "'[configName] artifactNotation key1:val1, key2:val2, key3:val3 ...'");
        }
        return spec;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean matches(ArtifactSpec spec) {
        return specValueMatch(this.configuration, spec.configuration) &&
                specValueMatch(this.group, spec.group) &&
                specValueMatch(this.name, spec.name) &&
                specValueMatch(this.version, spec.version) &&
                specValueMatch(this.classifier, spec.classifier) &&
                specValueMatch(this.type, spec.type);
    }

    private boolean specValueMatch(String pattern, String str) {
        // If pattern null or * always valid
        if (pattern == null || WILDCARD.equals(pattern)) {
            return true;
        }
        if (str == null || WILDCARD.equals(str)) {
            // Only null or wildcard matches, so should have return true above
            return false;
        }
        return PatternMatcher.match(pattern, str, false);
    }

    public String getConfiguration() {
        return configuration;
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getClassifier() {
        return classifier;
    }

    public String getType() {
        return type;
    }

    public Map<String, CharSequence> getProperties() {
        return properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ArtifactSpec)) {
            return false;
        }

        ArtifactSpec spec = (ArtifactSpec) o;

        if (classifier != null ? !classifier.equals(spec.classifier) : spec.classifier != null) {
            return false;
        }
        if (group != null ? !group.equals(spec.group) : spec.group != null) {
            return false;
        }
        if (!name.equals(spec.name)) {
            return false;
        }
        if (type != null ? !type.equals(spec.type) : spec.type != null) {
            return false;
        }
        if (!version.equals(spec.version)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = group != null ? group.hashCode() : 0;
        result = 31 * result + name.hashCode();
        result = 31 * result + version.hashCode();
        result = 31 * result + (classifier != null ? classifier.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    public static class Builder {
        private ArtifactSpec spec = new ArtifactSpec();

        private Builder() {
        }

        public ArtifactSpec build() {
            return spec;
        }

        public Builder artifactNotation(String artifactNotation) {
            Matcher matcher = ARTIFACT_NOTATION.matcher(artifactNotation);
            int matchGroups = matcher.groupCount();
            if (!matcher.matches() || matchGroups != 5) {
                throw new IllegalArgumentException("Invalid module notation: " + artifactNotation +
                        ". Expected: group:artifact:version:classifier[@ext].");
            }
            group(matcher.group(1));
            name(matcher.group(2));
            version(matcher.group(3));
            classifier(matcher.group(4));
            type(matcher.group(5));
            return this;
        }

        public Builder group(String group) {
            spec.group = group != null ? group : WILDCARD;
            return this;
        }

        public Builder name(String name) {
            spec.name = name != null ? name : WILDCARD;
            return this;
        }

        public Builder version(String version) {
            spec.version = version != null ? version : WILDCARD;
            return this;
        }

        public Builder classifier(String classifier) {
            spec.classifier = classifier != null ? classifier : WILDCARD;
            return this;
        }

        public Builder type(String type) {
            spec.type = type != null ? type : WILDCARD;
            return this;
        }

        public Builder configuration(String configuration) {
            spec.configuration =
                    configuration != null && !configuration.equalsIgnoreCase(CONFIG_ALL) ? configuration : WILDCARD;
            return this;
        }

        public Builder properties(Map<String, String> properties) {
            if (properties == null) {
                throw new IllegalArgumentException("Properties cannot be null");
            }
            spec.properties.putAll(properties);
            return this;
        }

        /**
         * Accepts a string in the format key1:val1, key2:val2, ...
         *
         * @param propsString a string list of properties values
         */
        public void properties(String propsString) {
            String[] keyVals = propsString.split(",");
            Map<String, String> props = new HashMap<String, String>(keyVals.length);
            for (String keyVal : keyVals) {
                String[] kv = keyVal.split(":");
                if (kv.length != 2) {
                    throw new IllegalArgumentException("Illegal key-vals format: " + propsString + "(" + keyVal +
                            "). Expected: key1:val1, key2:val2, ...");
                }
                props.put(kv[0].trim(), kv[1].trim());
            }
            spec.properties.putAll(props);
        }
    }
}
