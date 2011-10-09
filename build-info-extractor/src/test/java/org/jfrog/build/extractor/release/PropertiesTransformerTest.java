package org.jfrog.build.extractor.release;

import com.google.common.collect.Maps;
import com.google.common.io.Files;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Map;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Noam Y. Tenne
 */
public class PropertiesTransformerTest {

    @Test
    public void testTransformNoEols() throws Exception {
        File properties = File.createTempFile("temp", "properties");
        Files.write("momo=1.0popo=2.0", properties, Charset.forName("utf-8"));
        Map<String, String> versions = Maps.newHashMap();
        versions.put("momo", "2.0");
        versions.put("popo", "3.0");
        boolean transformed = new PropertiesTransformer(properties, versions).transform();
        assertTrue(transformed);
        String transformedProps = Files.toString(properties, Charset.forName("utf-8"));
        assertFalse(transformedProps.contains("\n"), "Unexpected EOL.");
        assertFalse(transformedProps.contains("\r"), "Unexpected EOL.");
    }

    @Test
    public void testTransformCrEols() throws Exception {
        File properties = File.createTempFile("temp", "properties");
        Files.write("momo=1.0\rpopo=2.0\r", properties, Charset.forName("utf-8"));
        Map<String, String> versions = Maps.newHashMap();
        versions.put("momo", "2.0");
        versions.put("popo", "3.0");
        boolean transformed = new PropertiesTransformer(properties, versions).transform();
        assertTrue(transformed);
        String transformedProps = Files.toString(properties, Charset.forName("utf-8"));
        assertFalse(transformedProps.contains("\n"), "Unexpected EOL.");
        assertTrue(transformedProps.contains("\r"), "Expected a CR EOL.");
        assertTrue(transformedProps.endsWith("\r"), "Expected the properties to end with an EOL");
    }

    @Test
    public void testTransformCrEolsWithNonAtTheEnd() throws Exception {
        File properties = File.createTempFile("temp", "properties");
        Files.write("momo=1.0\rpopo=2.0", properties, Charset.forName("utf-8"));
        Map<String, String> versions = Maps.newHashMap();
        versions.put("momo", "2.0");
        versions.put("popo", "3.0");
        boolean transformed = new PropertiesTransformer(properties, versions).transform();
        assertTrue(transformed);
        String transformedProps = Files.toString(properties, Charset.forName("utf-8"));
        assertFalse(transformedProps.contains("\n"), "Unexpected EOL.");
        assertTrue(transformedProps.contains("\r"), "Expected a CR EOL.");
        assertFalse(transformedProps.endsWith("\r"), "Expected the properties to end without an EOL");
    }

    @Test
    public void testTransformLfEols() throws Exception {
        File properties = File.createTempFile("temp", "properties");
        Files.write("momo=1.0\npopo=2.0\n", properties, Charset.forName("utf-8"));
        Map<String, String> versions = Maps.newHashMap();
        versions.put("momo", "2.0");
        versions.put("popo", "3.0");
        boolean transformed = new PropertiesTransformer(properties, versions).transform();
        assertTrue(transformed);
        String transformedProps = Files.toString(properties, Charset.forName("utf-8"));
        assertTrue(transformedProps.contains("\n"), "Expected an LF EOL.");
        assertFalse(transformedProps.contains("\r"), "Unexpected EOL.");
        assertTrue(transformedProps.endsWith("\n"), "Expected the properties to end with an EOL");
    }

    @Test
    public void testTransformLfEolsWithNonAtTheEnd() throws Exception {
        File properties = File.createTempFile("temp", "properties");
        Files.write("momo=1.0\npopo=2.0", properties, Charset.forName("utf-8"));
        Map<String, String> versions = Maps.newHashMap();
        versions.put("momo", "2.0");
        versions.put("popo", "3.0");
        boolean transformed = new PropertiesTransformer(properties, versions).transform();
        assertTrue(transformed);
        String transformedProps = Files.toString(properties, Charset.forName("utf-8"));
        assertTrue(transformedProps.contains("\n"), "Expected an LF EOL.");
        assertFalse(transformedProps.contains("\r"), "Unexpected EOL.");
        assertFalse(transformedProps.endsWith("\n"), "Expected the properties to end without an EOL");
    }

    @Test
    public void testTransformBothEols() throws Exception {
        File properties = File.createTempFile("temp", "properties");
        Files.write("momo=1.0\r\npopo=2.0\r\n", properties, Charset.forName("utf-8"));
        Map<String, String> versions = Maps.newHashMap();
        versions.put("momo", "2.0");
        versions.put("popo", "3.0");
        boolean transformed = new PropertiesTransformer(properties, versions).transform();
        assertTrue(transformed);
        String transformedProps = Files.toString(properties, Charset.forName("utf-8"));
        assertTrue(transformedProps.contains("\n"), "Expected an LF EOL.");
        assertTrue(transformedProps.contains("\r"), "Expected a CR EOL.");
        assertTrue(transformedProps.endsWith("\r\n"), "Expected the properties to end with an EOL");
    }

    @Test
    public void testTransformBothEolsWithNonAtTheEnd() throws Exception {
        File properties = File.createTempFile("temp", "properties");
        Files.write("momo=1.0\r\npopo=2.0", properties, Charset.forName("utf-8"));
        Map<String, String> versions = Maps.newHashMap();
        versions.put("momo", "2.0");
        versions.put("popo", "3.0");
        boolean transformed = new PropertiesTransformer(properties, versions).transform();
        assertTrue(transformed);
        String transformedProps = Files.toString(properties, Charset.forName("utf-8"));
        assertTrue(transformedProps.contains("\n"), "Expected an LF EOL.");
        assertTrue(transformedProps.contains("\r"), "Expected a CR EOL.");
        assertFalse(transformedProps.endsWith("\r\n"), "Expected the properties to end without an EOL");
    }
}
