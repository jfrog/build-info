package org.jfrog.build.extractor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.jfrog.build.api.Module;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Utilities for serializing/deserializing Module info as json
 */
public class ModuleExtractorUtils {
    private static JsonFactory createJsonFactory() {
        JsonFactory jsonFactory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(jsonFactory);
        mapper.setAnnotationIntrospector(new JacksonAnnotationIntrospector());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        jsonFactory.setCodec(mapper);
        return jsonFactory;
    }

    /**
     * Given a Module object, serialize it as a json string.
     *
     * @param module The module to serialize
     * @return The json string representing the serialized module
     * @throws IOException
     */
    public static String moduleToJsonString(Module module) throws IOException {
        JsonFactory jsonFactory = createJsonFactory();

        StringWriter writer = new StringWriter();
        JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(writer);
        jsonGenerator.useDefaultPrettyPrinter();

        jsonGenerator.writeObject(module);
        return writer.getBuffer().toString();
    }

    /**
     * Given a serialized json module string, deserialize it into a Module object.
     *
     * @param json The serialized json module string
     * @return A Module object deserialized from the provided string
     * @throws IOException
     */
    public static Module jsonStringToModule(String json) throws IOException {
        JsonFactory jsonFactory = createJsonFactory();
        JsonParser parser = jsonFactory.createParser(new StringReader(json));
        return jsonFactory.getCodec().readValue(parser, Module.class);
    }

    /**
     * Given a Module object, serialize it to a json string and write it to the provided file.
     *
     * @param module The module object
     * @param toFile The file to write the serialized module to
     * @throws IOException
     */
    public static void saveModuleToFile(Module module, File toFile) throws IOException {
        String moduleInfoJson = moduleToJsonString(module);
        if (!toFile.getParentFile().exists()) {
            toFile.getParentFile().mkdirs();
        }
        if (!toFile.exists()) {
            toFile.createNewFile();
        }
        Files.asCharSink(toFile, Charsets.UTF_8).write(moduleInfoJson);
    }

    /**
     * Given a file, read its contents as a json string and deserialize it as a Module object.
     *
     * @param fromFile The file containing a serialized json string
     * @return The Module object deserialized from the content of the file
     * @throws IOException
     */
    public static Module readModuleFromFile(File fromFile) throws IOException {
        String moduleInfoJson = Files.asCharSource(fromFile, Charsets.UTF_8).read();
        return jsonStringToModule(moduleInfoJson);
    }
}
