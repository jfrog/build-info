package org.jfrog.build.extractor;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.constants.BuildInfoProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;

/**
 * @author Noam Y. Tenne
 */
public abstract class BuildInfoExtractorUtils {

    /**
     * Collect system properties and properties from the  {@link BuildInfoProperties#PROP_PROPS_FILE} file.
     * <p/>
     * The caller is supposed to inject the build properties into the output (e.g.: adding them to the Build object if
     * the output of the extractor is a {@link org.jfrog.build.api.Build} instance, or saving them into a generated
     * buildInfo xml output file, if the output is a path to this file.
     */
    public static Properties getBuildInfoProperties() {
        Properties props = new Properties();
        File propertiesFile = new File(BuildInfoProperties.PROP_PROPS_FILE);
        InputStream inputStream = null;
        try {
            if (propertiesFile.exists()) {
                inputStream = new FileInputStream(propertiesFile);
                props.load(inputStream);
                props = filterProperties(props);
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to load build info properties from file: " + propertiesFile.getAbsolutePath(), e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }

        // now add all the relevant system props.
        Properties filteredSystemProps = filterProperties(System.getProperties());
        props.putAll(filteredSystemProps);
        return props;
    }

    public static Properties filterProperties(Properties source) {
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

    public static void saveBuildInfoToFile(Build build, OutputStream outputStream) throws IOException {
        String buildInfoJson = buildInfoToJsonString(build);
        IOUtils.write(buildInfoJson, outputStream);
    }
}