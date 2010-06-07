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
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.build.api.BuildInfoProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;

/**
 * @author Noam Y. Tenne
 */
public abstract class BuildInfoExtractorUtils {

    /**
     * Collect system properties and properties from the  {@link org.jfrog.build.api.BuildInfoConfigProperties#PROP_PROPS_FILE}
     * file.
     * <p/>
     * The caller is supposed to inject the build properties into the output (e.g.: adding them to the Build object if
     * the output of the extractor is a {@link org.jfrog.build.api.Build} instance, or saving them into a generated
     * buildInfo xml output file, if the output is a path to this file.
     */
    public static Properties getBuildInfoProperties() {
        Properties props = new Properties();
        String propertiesFilePath = System.getProperty(BuildInfoConfigProperties.PROP_PROPS_FILE);

        if (StringUtils.isNotBlank(propertiesFilePath)) {
            File propertiesFile = new File(propertiesFilePath);
            InputStream inputStream = null;
            try {
                if (propertiesFile.exists()) {
                    inputStream = new FileInputStream(propertiesFile);
                    props.load(inputStream);
                    props = filterBuildInfoProperties(props);
                }
            } catch (IOException e) {
                throw new RuntimeException(
                        "Unable to load build info properties from file: " + propertiesFile.getAbsolutePath(), e);
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }

        // now add all the relevant system props.
        Properties filteredSystemProps = filterBuildInfoProperties(System.getProperties());
        props.putAll(filteredSystemProps);

        //TODO: [by yl] Add common system properties
        return props;
    }

    public static Properties filterBuildInfoProperties(Properties source) {
        Properties properties = new Properties();
        Map<Object, Object> filteredProperties = Maps.filterKeys(source, new Predicate<Object>() {
            public boolean apply(Object input) {
                String key = (String) input;
                return key.startsWith(BuildInfoProperties.BUILD_INFO_PROP_PREFIX);
            }
        });
        properties.putAll(filteredProperties);
        return properties;
    }

    public static Properties getEnvProperties(Properties startProps) {
        Properties props = new Properties();
        boolean includeEnvVars = false;
        String includeVars = System.getProperty(BuildInfoConfigProperties.PROP_INCLUDE_ENV_VARS);
        if (StringUtils.isNotBlank(includeVars)) {
            includeEnvVars = Boolean.parseBoolean(includeVars);
        } else {
            includeVars = startProps.getProperty(BuildInfoConfigProperties.PROP_INCLUDE_ENV_VARS);
            if (StringUtils.isNotBlank(includeVars)) {
                includeEnvVars = Boolean.parseBoolean(includeVars);
            }
        }
        if (includeEnvVars) {
            Map<String, String> envMap = System.getenv();
            props.putAll(envMap);
        }
        String propertiesFilePath = System.getProperty(BuildInfoConfigProperties.PROP_PROPS_FILE);
        if (StringUtils.isNotBlank(propertiesFilePath)) {
            File propertiesFile = new File(propertiesFilePath);
            InputStream inputStream = null;
            try {
                if (propertiesFile.exists()) {
                    inputStream = new FileInputStream(propertiesFile);
                    props.load(inputStream);
                    props = filterEnvProperties(props);
                }
            } catch (IOException e) {
                throw new RuntimeException(
                        "Unable to load build info properties from file: " + propertiesFile.getAbsolutePath(), e);
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }

        Properties filteredSystemProperties = filterEnvProperties(System.getProperties());
        props.putAll(filteredSystemProperties);
        return props;
    }

    public static Properties filterEnvProperties(Properties source) {
        Properties properties = new Properties();
        Map<Object, Object> filtered = Maps.filterKeys(source, new Predicate<Object>() {
            public boolean apply(Object input) {
                String key = input.toString();
                return key.startsWith(BuildInfoProperties.BUILD_INFO_ENVIRONMENT_PREFIX);
            }
        });
        properties.putAll(filtered);
        return properties;
    }


    //TODO: [by YS] duplicates ArtifactoryBuildInfoClient. The client should depend on this module

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

    public static void saveBuildInfoToFile(Build build, File toFile) throws IOException {
        String buildInfoJson = buildInfoToJsonString(build);
        FileUtils.writeStringToFile(toFile, buildInfoJson, "UTF-8");
    }
}