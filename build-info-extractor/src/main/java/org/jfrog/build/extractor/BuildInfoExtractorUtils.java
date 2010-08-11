/*
 * Copyright (C) 2010 JFrog Ltd.
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

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.build.api.BuildInfoProperties;
import org.jfrog.build.client.ClientProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
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
                IOUtils.closeQuietly(inputStream);
            }
        }

        props.putAll(existingProps);
        props.putAll(System.getProperties());

        return props;
    }


    public static Properties filterDynamicProperties(Properties source, Predicate<Object> filter) {
        Properties properties = new Properties();
        properties.putAll(Maps.filterKeys(source, filter));
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
        Properties props = new Properties();
        boolean includeEnvVars = false;
        String includeVars = System.getProperty(BuildInfoConfigProperties.PROP_INCLUDE_ENV_VARS);
        if (StringUtils.isBlank(includeVars)) {
            includeVars = System.getenv(BuildInfoConfigProperties.PROP_INCLUDE_ENV_VARS);
        }
        if (StringUtils.isBlank(includeVars)) {
            includeVars = startProps.getProperty(BuildInfoConfigProperties.PROP_INCLUDE_ENV_VARS);
        }
        if (StringUtils.isNotBlank(includeVars)) {
            includeEnvVars = Boolean.parseBoolean(includeVars);
        }
        if (includeEnvVars) {
            Map<String, String> envMap = System.getenv();
            for (Map.Entry<String, String> entry : envMap.entrySet()) {
                props.put(BuildInfoProperties.BUILD_INFO_ENVIRONMENT_PREFIX + entry.getKey(), entry.getValue());
            }
        }
        String propertiesFilePath = getAdditionalPropertiesFile(startProps);
        if (StringUtils.isNotBlank(propertiesFilePath)) {
            File propertiesFile = new File(propertiesFilePath);
            InputStream inputStream = null;
            try {
                if (propertiesFile.exists()) {
                    inputStream = new FileInputStream(propertiesFile);
                    props.load(inputStream);
                    props = filterDynamicProperties(props, ENV_PREDICATE);
                }
            } catch (IOException e) {
                throw new RuntimeException(
                        "Unable to load build info properties from file: " + propertiesFile.getAbsolutePath(), e);
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }
        Properties filteredSystemProperties = filterDynamicProperties(System.getProperties(), ENV_PREDICATE);
        filteredSystemProperties.putAll(System.getenv());
        filteredSystemProperties = filterDynamicProperties(filteredSystemProperties, ENV_PREDICATE);
        for (Map.Entry<Object, Object> entry : filteredSystemProperties.entrySet()) {
            props.put(entry.getKey(), entry.getValue());
        }
        props = stripPrefixFromProperties(props, BuildInfoProperties.BUILD_INFO_ENVIRONMENT_PREFIX);
        return props;
    }

    //TODO: [by YS] duplicates ArtifactoryBuildInfoClient. The client should depend on this module
    //TODO: [by yl] introduce a commons module for common impl and also move PropertyUtils there


    private static JsonFactory createJsonFactory() {
        JsonFactory jsonFactory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(jsonFactory);
        mapper.getSerializationConfig().setAnnotationIntrospector(new JacksonAnnotationIntrospector());
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
        JsonFactory jsonFactory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(jsonFactory);
        mapper.getSerializationConfig().setAnnotationIntrospector(new JacksonAnnotationIntrospector());
        jsonFactory.setCodec(mapper);
        JsonParser parser = jsonFactory.createJsonParser(new StringReader(json));
        return mapper.readValue(parser, Build.class);
    }

    public static void saveBuildInfoToFile(Build build, File toFile) throws IOException {
        String buildInfoJson = buildInfoToJsonString(build);
        FileUtils.writeStringToFile(toFile, buildInfoJson, "UTF-8");
    }

    private static String getAdditionalPropertiesFile(Properties additionalProps) {
        String propertiesFilePath = System.getProperty(BuildInfoConfigProperties.PROP_PROPS_FILE);
        if (StringUtils.isBlank(propertiesFilePath) && additionalProps != null) {
            propertiesFilePath = additionalProps.getProperty(BuildInfoConfigProperties.PROP_PROPS_FILE);
        }
        return propertiesFilePath;
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