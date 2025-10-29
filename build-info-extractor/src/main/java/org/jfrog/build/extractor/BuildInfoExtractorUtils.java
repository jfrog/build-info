package org.jfrog.build.extractor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.CommonUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.extractor.ci.BuildInfoConfigProperties;
import org.jfrog.build.extractor.ci.BuildInfoProperties;
import org.jfrog.build.extractor.clientConfiguration.ClientProperties;
import org.jfrog.build.extractor.clientConfiguration.IncludeExcludePatterns;
import org.jfrog.build.extractor.clientConfiguration.PatternMatcher;
import org.jfrog.build.extractor.clientConfiguration.util.encryption.EncryptionKeyPair;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;

import static org.apache.commons.lang3.StringUtils.*;
import static org.jfrog.build.api.BuildInfoConfigProperties.ENV_PROPERTIES_FILE_KEY;
import static org.jfrog.build.api.BuildInfoConfigProperties.ENV_PROPERTIES_FILE_KEY_IV;
import static org.jfrog.build.extractor.UrlUtils.encodeUrl;
import static org.jfrog.build.extractor.UrlUtils.encodeUrlPathPart;
import static org.jfrog.build.extractor.clientConfiguration.util.encryption.PropertyEncryptor.decryptPropertiesFromFile;

/**
 * @author Noam Y. Tenne
 */
public abstract class BuildInfoExtractorUtils {

    public static final String BUILD_BROWSE_PLATFORM_URL = "/ui/builds";
    public static final String BUILD_BROWSE_URL = "/webapp/builds";
    private static final String BUILD_REPO_PARAM_PATTERN = "?buildRepo=%s-build-info&projectKey=%s";
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

    public static Properties mergePropertiesWithSystemAndPropertyFile(Properties existingProps, Log log) {
        Properties mergedProps = new Properties();
        // Add properties from file.
        mergedProps.putAll(searchAdditionalPropertiesFile(mergedProps, log));
        // Merge properties from system properties - should be second to override any properties defined in the file.
        mergedProps.putAll(addSystemProperties(existingProps));
        return mergedProps;
    }


    private static Properties addSystemProperties(Properties existingProps) {
        Properties props = new Properties();
        props.putAll(existingProps);
        props.putAll(System.getProperties());
        return props;
    }

    /**
     * Retrieves additional properties from a specified build info properties file path.
     *
     * @param existingProps Existing properties object.
     * @param log           Logger instance for logging debug information.
     * @return Properties object containing additional properties if found; otherwise, an empty properties object.
     */
    private static Properties searchAdditionalPropertiesFile(Properties existingProps, Log log) {
        Properties props = new Properties();
        String propsFilePath = getAdditionalPropertiesFile(existingProps, log);

        if (StringUtils.isBlank(propsFilePath)) {
            log.debug("[buildinfo] BuildInfo properties file path is not found.");
            return props;
        }

        File propertiesFile = new File(propsFilePath);
        if (!propertiesFile.exists()) {
            log.debug("[buildinfo] BuildInfo properties file is not exists.");
            return props;
        }

        try {
            EncryptionKeyPair keyPair = new EncryptionKeyPair(getPropertiesFileEncryptionKey(), getPropertiesFileEncryptionKeyIv());
            if (!keyPair.isEmpty()) {
                log.debug("[buildinfo] Found an encryption for buildInfo properties file for this build.");
                props.putAll(decryptPropertiesFromFile(propertiesFile.getPath(), keyPair));
            } else {
                try (InputStream inputStream = Files.newInputStream(propertiesFile.toPath())) {
                    props.load(inputStream);
                } catch (IllegalArgumentException e) {
                    log.warn("[buildinfo] Properties file contains malformed Unicode encoding. Attempting to load with UTF-8 encoding: " + e.getMessage());
                    try (BufferedReader reader = Files.newBufferedReader(propertiesFile.toPath(), StandardCharsets.UTF_8)) {
                        props.load(reader);
                    } catch (Exception fallbackException) {
                        log.error("[buildinfo] Failed to load properties file even with UTF-8 fallback: " + fallbackException.getMessage());
                    }
                }
            }
        } catch (IOException | InvalidAlgorithmParameterException | IllegalBlockSizeException | NoSuchPaddingException |
                 BadPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Unable to load build info properties from file: " + propertiesFile.getAbsolutePath(), e);
        }
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
            if (isBuildInfoProperty((String) startEntry.getKey())) {
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

        props.putAll(filterDynamicProperties(searchAdditionalPropertiesFile(startProps, log), ENV_PREDICATE));
        return props;
    }

    private static boolean isBuildInfoProperty(String propertyKey) {
        return StringUtils.startsWithAny(propertyKey,
                BuildInfoConfigProperties.PROP_ENV_VARS_EXCLUDE_PATTERNS,
                BuildInfoProperties.BUILD_INFO_ENVIRONMENT_PREFIX,
                BuildInfoConfigProperties.PROP_ENV_VARS_INCLUDE_PATTERNS);
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

        try (StringWriter writer = new StringWriter();
             JsonGenerator jsonGenerator = jsonFactory.createGenerator(writer)) {
            jsonGenerator.useDefaultPrettyPrinter();
            jsonGenerator.writeObject(buildInfo);
            return writer.getBuffer().toString();
        }
    }

    public static BuildInfo jsonStringToBuildInfo(String json) throws IOException {
        JsonFactory jsonFactory = createJsonFactory();
        try (JsonParser parser = jsonFactory.createParser(new StringReader(json))) {
            return jsonFactory.getCodec().readValue(parser, BuildInfo.class);
        }
    }

    public static <T extends Serializable> String buildInfoToJsonString(T buildComponent) throws IOException {
        JsonFactory jsonFactory = createJsonFactory();

        try (StringWriter writer = new StringWriter();
             JsonGenerator jsonGenerator = jsonFactory.createGenerator(writer)) {
            jsonGenerator.useDefaultPrettyPrinter();
            jsonGenerator.writeObject(buildComponent);

            return writer.getBuffer().toString();
        }
    }

    public static <T extends Serializable> T jsonStringToGeneric(String json, Class<T> clazz) throws IOException {
        JsonFactory jsonFactory = createJsonFactory();
        try (JsonParser parser = jsonFactory.createParser(new StringReader(json))) {
            return jsonFactory.getCodec().readValue(parser, clazz);
        }
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

    /**
     * @return The encryption key obtained from system properties or additional properties.
     */
    private static String getPropertiesFileEncryptionKey() {
        return StringUtils.isNotBlank(System.getenv(ENV_PROPERTIES_FILE_KEY)) ? System.getenv(ENV_PROPERTIES_FILE_KEY) : System.getProperty(ENV_PROPERTIES_FILE_KEY);
    }

    /**
     * @return The encryption IV obtained from system properties or additional properties.
     */
    private static String getPropertiesFileEncryptionKeyIv() {
        return StringUtils.isNotBlank(System.getenv(ENV_PROPERTIES_FILE_KEY_IV)) ? System.getenv(ENV_PROPERTIES_FILE_KEY_IV) : System.getProperty(ENV_PROPERTIES_FILE_KEY_IV);
    }

    private static String getAdditionalPropertiesFile(Properties additionalProps, Log log) {
        String key = BuildInfoConfigProperties.PROP_PROPS_FILE;
        String filePath = System.getProperty(key);
        String propFoundPath = "System.getProperty(" + key + ")";
        if (StringUtils.isBlank(filePath)) {
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
                result = classifier + (StringUtils.isNotBlank(extension) && !classifier.endsWith(extension) ? "-" + extension : "");
            }
        }
        // Add extension if not jar, ivy or pom type
        // and current type does not end with the extension (avoid war-war, zip-zip, source-jar-jar, ...)
        if (!"jar".equals(result) && !"pom".equals(type) && !"ivy".equals(type)) {
            if (StringUtils.isNotBlank(extension) && !result.endsWith(extension)) {
                result = result + "-" + extension;
            }
        }
        // Artifactory limit for type length is 64
        return result.length() > ARTIFACT_TYPE_LENGTH_LIMIT ? type : result;
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

    /**
     * Creates a build info link to the published build. This method is in use also in the Jenkins Artifactory plugin.
     *
     * @param url         JFrog Platform or Artifactory URL
     * @param buildName   Build name of the published build
     * @param buildNumber Build number of the published build
     * @param timeStamp   Timestamp (started date time in milliseconds) of the published build
     * @param project     Project of the published build
     * @param encode      True if should encode build name and build number
     * @param platformUrl True if the input url is platform's URL, false if it is Artifactory's URL
     * @return Link to the published build.
     */
    public static String createBuildInfoUrl(String url, String buildName, String buildNumber, String timeStamp,
                                            String project, boolean encode, boolean platformUrl) {
        if (platformUrl) {
            // Platform URL provided. We have everything we need to create the build info URL.
            return createBuildInfoUrl(url, buildName, buildNumber, timeStamp, project, encode);
        }
        if (isNotBlank(project)) {
            if (endsWith(url, "/artifactory")) {
                // If the project parameter provided, try to guess the platform URL.
                return createBuildInfoUrl(removeEnd(url, "/artifactory"), buildName,
                        buildNumber, timeStamp, project, encode);
            }
            // If Artifactory's URL doesn't end with "/artifactory", it is impossible to create the URL.
            return "";
        }
        // Platform URL and project was not provided - use Artifactory 6 style URL
        return createBuildInfoUrl(url, buildName, buildNumber, encode);
    }

    /**
     * Creates a build info link to the published build in JFrog platform (Artifactory V7)
     *
     * @param platformUrl - Base platform URL
     * @param buildName   - Build name of the published build
     * @param buildNumber - Build number of the published build
     * @param timeStamp   - Timestamp (started date time in milliseconds) of the published build
     * @param project     - Build project of the published build
     * @param encode      - True if should encode build name and build number
     * @return Link to the published build in JFrog platform e.g. https://myartifactory.com/ui/builds/gradle-cli/1/1619429119501/published?buildRepo=ecosys-build-info&projectKey=ecosys
     */
    private static String createBuildInfoUrl(String platformUrl, String buildName, String buildNumber, String timeStamp, String project, boolean encode) {
        if (encode) {
            buildName = encodeUrlPathPart(buildName);
            buildNumber = encodeUrlPathPart(buildNumber);
        }
        String timestampUrlPart = isBlank(timeStamp) ? "" : "/" + timeStamp;
        return String.format("%s/%s/%s%s/%s", platformUrl + BUILD_BROWSE_PLATFORM_URL, buildName, buildNumber,
                timestampUrlPart, "published" + getBuildRepoQueryParam(project));
    }

    /**
     * Return "?buildRepo=<project>-build-info" if project provided or an empty string otherwise.
     *
     * @param project - Project of the published build
     * @return build repo query param or empty string.
     */
    private static String getBuildRepoQueryParam(String project) {
        if (isEmpty(project)) {
            return "";
        }
        String encodedProject = encodeUrl(project);
        return String.format(BUILD_REPO_PARAM_PATTERN, encodedProject, encodedProject);
    }

    /**
     * Creates a build info link to the published build in Artifactory (Artifactory V6 or below)
     *
     * @param artifactoryUrl - Base Artifactory URL
     * @param buildName      - Build name of the published build
     * @param buildNumber    - Build number of the published build
     * @param encode         - True if should encode build name and build number
     * @return Link to the published build in Artifactory e.g. https://myartifactory.com/artifactory/webapp/builds/gradle-cli/1
     */
    private static String createBuildInfoUrl(String artifactoryUrl, String buildName, String buildNumber, boolean encode) {
        if (encode) {
            buildName = encodeUrlPathPart(buildName);
            buildNumber = encodeUrlPathPart(buildNumber);
        }
        return String.format("%s/%s/%s", artifactoryUrl + BUILD_BROWSE_URL, buildName, buildNumber);
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
