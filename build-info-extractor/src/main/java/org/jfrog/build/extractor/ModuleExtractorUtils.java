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

    public static String moduleToJsonString(Module module) throws IOException {
        JsonFactory jsonFactory = createJsonFactory();

        StringWriter writer = new StringWriter();
        JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(writer);
        jsonGenerator.useDefaultPrettyPrinter();

        jsonGenerator.writeObject(module);
        return writer.getBuffer().toString();
    }

    public static Module jsonStringToModule(String json) throws IOException {
        JsonFactory jsonFactory = createJsonFactory();
        JsonParser parser = jsonFactory.createParser(new StringReader(json));
        return jsonFactory.getCodec().readValue(parser, Module.class);
    }

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

    public static Module readModuleFromFile(File fromFile) throws IOException {
        String moduleInfoJson = Files.asCharSource(fromFile, Charsets.UTF_8).read();
        return jsonStringToModule(moduleInfoJson);
    }
}
