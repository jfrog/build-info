package org.jfrog.build.extractor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.extractor.ci.BuildInfoConfigProperties;
import org.jfrog.build.extractor.ci.BuildInfoProperties;
import org.jfrog.build.api.util.CommonUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ClientProperties;
import org.jfrog.build.extractor.clientConfiguration.IncludeExcludePatterns;
import org.jfrog.build.extractor.clientConfiguration.PatternMatcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;

/**
 * @author Noam Y. Tenne
 */
public abstract class BuildInfoExtractorUtils {

    private static final int ARTIFACT_TYPE_LENGTH_LIMIT = 64;

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
        return mergePropertiesWithSystemAndPropertyFile(existingProps, null);
    }

    public static Properties mergePropertiesWithSystemAndPropertyFile(Properties existingProps, Log log) {
        Properties props = new Properties();
        addPropsFromCommandSystemProp(existingProps, log);
        String propsFilePath = getAdditionalPropertiesFile(existingProps, log);
        if (StringUtils.isNotBlank(propsFilePath)) {
            File propertiesFile = new File(propsFilePath);
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

    public static Map<String, ?> filterStringEntries(Map<String, ?> map) {
        return CommonUtils.filterMapValues(map, value -> value instanceof String);
    }

    public static Properties filterDynamicProperties(Properties source, Predicate<Object> filter) {
        Properties properties = new Properties();
        if (source != null) {
            properties.putAll(CommonUtils.filterMapKeys(source, filter));
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

    public static Properties getEnvProperties(Properties startProps, Log log) {
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
        // Filter map to include entries with keys only in System Properties:
        Map<String, String> filteredSysProps = CommonUtils.entriesOnlyOnLeftMap(sysProps, System.getenv());
        for (Map.Entry<String, String> entry : filteredSysProps.entrySet()) {
            String varKey = entry.getKey();
            if (PatternMatcher.pathConflicts(varKey, patterns)) {
                continue;
            }
            props.put(varKey, entry.getValue());
        }

        // TODO: [by FSI] Test if this is needed! Since start props are used now
        String propsFilePath = getAdditionalPropertiesFile(startProps, log);
        if (StringUtils.isNotBlank(propsFilePath)) {
            File propertiesFile = new File(propsFilePath);
            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(propertiesFile);
                Properties propertiesFromFile = new Properties();
                propertiesFromFile.load(inputStream);
                props.putAll(filterDynamicProperties(propertiesFromFile, ENV_PREDICATE));
            } catch (IOException e) {
                throw new RuntimeException(
                        "Unable to load build info properties from file: " + propertiesFile.getAbsolutePath(), e);
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }
        return props;
    }

    //TODO: [by YS] duplicates ArtifactoryBuildInfoClient. The client should depend on this module
    //TODO: [by yl] introduce a commons module for common impl and also move PropertyUtils there


    private static JsonFactory createJsonFactory() {
        JsonFactory jsonFactory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(jsonFactory);
        mapper.setAnnotationIntrospector(new JacksonAnnotationIntrospector());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        jsonFactory.setCodec(mapper);
        return jsonFactory;
    }

    public static String buildInfoToJsonString(BuildInfo buildInfo) throws IOException {
        JsonFactory jsonFactory = createJsonFactory();

        StringWriter writer = new StringWriter();
        JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(writer);
        jsonGenerator.useDefaultPrettyPrinter();

        jsonGenerator.writeObject(buildInfo);
        String result = writer.getBuffer().toString();
        return result;
    }

    public static BuildInfo jsonStringToBuildInfo(String json) throws IOException {
        JsonFactory jsonFactory = createJsonFactory();
        JsonParser parser = jsonFactory.createParser(new StringReader(json));
        return jsonFactory.getCodec().readValue(parser, BuildInfo.class);
    }

    public static <T extends Serializable> String buildInfoToJsonString(T buildComponent) throws IOException {
        JsonFactory jsonFactory = createJsonFactory();

        StringWriter writer = new StringWriter();
        JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(writer);
        jsonGenerator.useDefaultPrettyPrinter();

        jsonGenerator.writeObject(buildComponent);
        String result = writer.getBuffer().toString();
        return result;
    }

    public static <T extends Serializable> T jsonStringToGeneric(String json, Class<T> clazz) throws IOException {
        JsonFactory jsonFactory = createJsonFactory();
        JsonParser parser = jsonFactory.createParser(new StringReader(json));
        return jsonFactory.getCodec().readValue(parser, clazz);
    }

    public static void saveBuildInfoToFile(BuildInfo buildInfo, File toFile) throws IOException {
        String buildInfoJson = buildInfoToJsonString(buildInfo);
        if (!toFile.getParentFile().exists()) {
            toFile.getParentFile().mkdirs();
        }
        if (!toFile.exists()) {
            toFile.createNewFile();
        }
        CommonUtils.writeByCharset(buildInfoJson, toFile, StandardCharsets.UTF_8);
    }

    private static String getAdditionalPropertiesFile(Properties additionalProps, Log log) {
        String key = BuildInfoConfigProperties.PROP_PROPS_FILE;
        String filePath = System.getProperty(key);
        String propFoundPath = "System.getProperty(" + key + ")";
        if (StringUtils.isBlank(filePath) && additionalProps != null) {
            filePath = additionalProps.getProperty(key);
            propFoundPath = "additionalProps.getProperty(" + key + ")";
        }
        if (StringUtils.isBlank(filePath)) {
            // Jenkins prefixes these variables with "env." so let's try that
            filePath = additionalProps.getProperty("env." + key);
            if (StringUtils.isBlank(filePath)) {
                filePath = System.getenv(key);
                propFoundPath = "System.getenv(" + key + ")";
            } else {
                propFoundPath = "additionalProps.getProperty(env." + key + ")";
            }
        }
        if (StringUtils.isBlank(filePath)) {
            // Jenkins prefixes these variables with "env." so let's try that
            key = BuildInfoConfigProperties.ENV_BUILDINFO_PROPFILE;
            filePath = additionalProps.getProperty("env." + key);
            if (StringUtils.isBlank(filePath)) {
                filePath = System.getenv(key);
                propFoundPath = "System.getenv(" + key + ")";
            } else {
                propFoundPath = "additionalProps.getProperty(env." + key + ")";
            }
        }
        if (log != null) {
            if (StringUtils.isBlank(filePath)) {
                log.debug("[buildinfo] Not using buildInfo properties file for this build.");
            } else {
                log.debug("[buildinfo] Properties file '" + filePath + "' retrieved from '" + propFoundPath + "'");
            }
        }
        return filePath;
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
         if (result.length() > ARTIFACT_TYPE_LENGTH_LIMIT) {
             //  Artifactory limit for type length is 64
            return type;
        }
        return result;
    }

    public static String getModuleIdString(String organisation, String name, String version) {
        return organisation + ':' + name + ':' + version;
    }

    /**
     * Create an object mapper for serialization/deserialization.
     * This mapper ignore unknown properties and null values.
     *
     * @return a new object mapper
     */
    public static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }

    private static class PrefixPredicate implements Predicate<Object> {

        private String prefix;

        protected PrefixPredicate(String prefix) {
            this.prefix = prefix;
        }

        public boolean test(Object o) {
            return o != null && ((String) o).startsWith(prefix);
        }
    }

    private static void addPropsFromCommandSystemProp(Properties additionalProps, Log log) {
        // Bamboo ivy should read password and props file location from system property named "sun.java.command"
        String commandKey = "sun.java.command";
        String[] keys = {BuildInfoConfigProperties.PROP_PROPS_FILE, "artifactory.publish.password"};
        String command = System.getProperty(commandKey);
        if (StringUtils.isNotBlank(command)) {
            String[] commandParts = StringUtils.split(command, " ");
            for (String commandPart : commandParts) {
                for (String key : keys) {
                    if (commandPart.startsWith("-D" + key)) {
                        additionalProps.put(key, StringUtils.split(commandPart, "=")[1].trim());
                        log.debug(String.format("Adding property %s from the command property: %s", key, commandKey));
                    }
                }
            }
        }
    }
}
