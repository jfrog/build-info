package org.jfrog.build.extractor.clientConfiguration.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import static org.jfrog.build.extractor.BuildInfoExtractorUtils.createMapper;

public class JsonUtils {
    public static String toJsonString(Object object) throws IOException {
        JsonFactory jsonFactory = createJsonFactory();
        try (StringWriter writer = new StringWriter();
             JsonGenerator jsonGenerator = jsonFactory.createGenerator(writer)) {
            jsonGenerator.useDefaultPrettyPrinter();
            jsonGenerator.writeObject(object);

            return writer.getBuffer().toString();
        }
    }

    public static JsonParser createJsonParser(InputStream in) throws IOException {
        JsonFactory jsonFactory = createJsonFactory();
        return jsonFactory.createParser(in);
    }

    public static JsonParser createJsonParser(String content) throws IOException {
        JsonFactory jsonFactory = createJsonFactory();
        return jsonFactory.createParser(content);
    }

    public static JsonFactory createJsonFactory() {
        JsonFactory jsonFactory = new JsonFactory();
        ObjectMapper mapper = createMapper();
        mapper.setAnnotationIntrospector(new JacksonAnnotationIntrospector());
        jsonFactory.setCodec(mapper);
        return jsonFactory;
    }
}
