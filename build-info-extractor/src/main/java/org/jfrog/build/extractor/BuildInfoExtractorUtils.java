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

package org.jfrog.build.extractor;

import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.sun.istack.internal.Nullable;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.build.api.BuildInfoProperties;
import org.jfrog.build.client.ClientProperties;
import org.jfrog.build.client.IncludeExcludePatterns;
import org.jfrog.build.client.PatternMatcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Noam Y. Tenne
 */
public abstract class BuildInfoExtractorUtils {

    public static final Predicate<Object> BUILD_INFO_PREDICATE =
            new PrefixPredicate(BuildInfoProperties.BUILD_INFO_PREFIX);

    public static final Predicate<Object> BUILD_INFO_PROP_PREDICATE =
            new PrefixPredicate(BuildInfoProperties.BUILD_INFO_PROP_PREFIX);

    public static final Predicate<Object> ENV_PREDICATE =
            new PrefixPredicate(BuildInfoProperties.BUILD_INFO_ENVIRONMENT_PREFIX);

    public static final Predicate<Object> CLIENT_PREDICATE = new PrefixPredicate(ClientProperties.ARTIFACTORY_PREFIX);

    public static final Predicate<Object> MATRIX_PARAM_PREDICATE =
            new PrefixPredicate(ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX);

    public static Properties mergePropertiesWithSystemAndPropertyFile(Properties existingProps) {
        Properties props = new Properties();
        String propertiesFilePath = getAdditionalPropertiesFile(existingProps);
        if (StringUtils.isNotBlank(propertiesFilePath)) {
            File propertiesFile = new File(propertiesFilePath);
            InputStream inputStream = null;
            try {
                if (propertiesFile.exists()) {
                    inputStream = new FileInputStream(propertiesFile);
                    props.load(inputStream);
                }
            } catch (IOException e) {
                throw new RuntimeException(
                        "Unable to load build info properties from file: " + propertiesFile.getAbsolutePath(), e);
            } finally {
                Closeables.closeQuietly(inputStream);
            }
        }

        props.putAll(existingProps);
        props.putAll(System.getProperties());

        return props;
    }

    public static Map<String, ?> filterStringEntries(Map<String, ?> map) {
        return Maps.filterValues(map, new Predicate<Object>() {
            public boolean apply(@Nullable Object input) {
                return input != null && input instanceof String;
            }
        });
    }

    public static Properties filterDynamicProperties(Properties source, Predicate<Object> filter) {
        Properties properties = new Properties();
        if (source != null) {
            properties.putAll(Maps.filterKeys(source, filter));
        }
        return properties;
    }

    public static Properties stripPrefixFromProperties(Properties source, String prefix) {
        Properties props = new Properties();
        for (Map.Entry<Object, Object> entry : source.entrySet()) {
            String key = entry.getKey().toString();
            props.put(StringUtils.removeStart(key, prefix), entry.getValue());
        }
        return props;
    }

    public static Properties getEnvProperties(Properties startProps) {
        IncludeExcludePatterns patterns = new IncludeExcludePatterns(
                startProps.getProperty(BuildInfoConfigProperties.PROP_ENV_VARS_INCLUDE_PATTERNS),
                startProps.getProperty(BuildInfoConfigProperties.PROP_ENV_VARS_EXCLUDE_PATTERNS));

        Properties props = new Properties();

        // Add all the startProps that starts with BuildInfoProperties.BUILD_INFO_ENVIRONMENT_PREFIX
        for (Map.Entry<Object, Object> startEntry : startProps.entrySet()) {
            if (StringUtils.startsWith((String) startEntry.getKey(),
                    BuildInfoProperties.BUILD_INFO_ENVIRONMENT_PREFIX)) {
                props.put(startEntry.getKey(), startEntry.getValue());
            }
        }

        // Add all system environment that match the patterns
        Map<String, String> envMap = System.getenv();
        for (Map.Entry<String, String> entry : envMap.entrySet()) {
            String varKey = entry.getKey();
            if (PatternMatcher.pathConflicts(varKey, patterns)) {
                continue;
            }
            props.put(BuildInfoProperties.BUILD_INFO_ENVIRONMENT_PREFIX + varKey, entry.getValue());
        }

        Map<String, String> sysProps = new HashMap(System.getProperties());
        Map<String, String> filteredSysProps = Maps.difference(sysProps, System.getenv()).entriesOnlyOnLeft();
        for (Map.Entry<String, String> entry : filteredSysProps.entrySet()) {
            String varKey = entry.getKey();
            if (PatternMatcher.pathConflicts(varKey, patterns)) {
                continue;
            }
            props.put(varKey, entry.getValue());
        }

        // TODO: [by FSI] Test if this is needed! Since start props are used now
        String propertiesFilePath = getAdditionalPropertiesFile(startProps);
        if (StringUtils.isNotBlank(propertiesFilePath)) {
            File propertiesFile = new File(propertiesFilePath);
            InputStream inputStream = null;
            try {
                if (propertiesFile.exists()) {
                    inputStream = new FileInputStream(propertiesFile);
                    Properties propertiesFromFile = new Properties();
                    propertiesFromFile.load(inputStream);
                    props.putAll(filterDynamicProperties(propertiesFromFile, ENV_PREDICATE));
                }
            } catch (IOException e) {
                throw new RuntimeException(
                        "Unable to load build info properties from file: " + propertiesFile.getAbsolutePath(), e);
            } finally {
                Closeables.closeQuietly(inputStream);
            }
        }
        return props;
    }

    //TODO: [by YS] duplicates ArtifactoryBuildInfoClient. The client should depend on this module
    //TODO: [by yl] introduce a commons module for common impl and also move PropertyUtils there


    private static JsonFactory createJsonFactory() {
        JsonFactory jsonFactory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(jsonFactory);
        mapper.getSerializationConfig().setAnnotationIntrospector(new JacksonAnnotationIntrospector());
        mapper.getSerializationConfig().setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        mapper.getDeserializationConfig().disable(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES);
        jsonFactory.setCodec(mapper);
        return jsonFactory;
    }

    public static String buildInfoToJsonString(Build buildInfo) throws IOException {
        JsonFactory jsonFactory = createJsonFactory();

        StringWriter writer = new StringWriter();
        JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(writer);
        jsonGenerator.useDefaultPrettyPrinter();

        jsonGenerator.writeObject(buildInfo);
        String result = writer.getBuffer().toString();
        return result;
    }

    public static Build jsonStringToBuildInfo(String json) throws IOException {
        JsonFactory jsonFactory = createJsonFactory();
        JsonParser parser = jsonFactory.createJsonParser(new StringReader(json));
        return jsonFactory.getCodec().readValue(parser, Build.class);
    }

    public static void saveBuildInfoToFile(Build build, File toFile) throws IOException {
        String buildInfoJson = buildInfoToJsonString(build);
        if (!toFile.getParentFile().exists()) {
            toFile.getParentFile().mkdirs();
        }
        if (!toFile.exists()) {
            toFile.createNewFile();
        }
        Files.write(buildInfoJson, toFile, Charsets.UTF_8);
    }

    private static String getAdditionalPropertiesFile(Properties additionalProps) {
        String key = BuildInfoConfigProperties.PROP_PROPS_FILE;
        String propertiesFilePath = System.getProperty(key);
        if (StringUtils.isBlank(propertiesFilePath) && additionalProps != null) {
            propertiesFilePath = additionalProps.getProperty(key);
            if (StringUtils.isBlank(propertiesFilePath)) {
                // Jenkins prefixes these variables with "env." so let's try that
                propertiesFilePath = additionalProps.getProperty("env." + key);
                if (StringUtils.isBlank(propertiesFilePath)) {
                    propertiesFilePath = System.getenv(key);
                }
            }
        }
        return propertiesFilePath;
    }

    public static String getArtifactId(String moduleId, String artifactName) {
        return moduleId + ":" + artifactName;
    }

    public static String getTypeString(String type, String classifier, String extension) {
        String result = type;
        // Only use classifier if jar type
        if ("jar".equals(type)) {
            // add classifier if it exists
            if (StringUtils.isNotBlank(classifier)) {
                result = classifier;
            }
        }
        // Add extension if not jar, ivy or pom type
        // and current type does not end with the extension (avoid war-war, zip-zip, source-jar-jar, ...)
        if (!"jar".equals(result) && !"pom".equals(type) && !"ivy".equals(type)) {
            if (StringUtils.isNotBlank(extension) && !result.endsWith(extension)) {
                result = result + "-" + extension;
            }
        }
        return result;
    }

    public static String getModuleIdString(String organisation, String name, String version) {
        return organisation + ':' + name + ':' + version;
    }

    private static class PrefixPredicate implements Predicate<Object> {

        private String prefix;

        protected PrefixPredicate(String prefix) {
            this.prefix = prefix;
        }

        public boolean apply(Object o) {
            return o != null && ((String) o).startsWith(prefix);
        }
    }
}