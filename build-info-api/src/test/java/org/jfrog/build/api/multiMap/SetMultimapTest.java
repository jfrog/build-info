package org.jfrog.build.api.multiMap;

import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Test
public class SetMultimapTest {

    public void testDefaultConstructor() {
        // Create a new multimap
        Multimap<String, String> multimap = new SetMultimap<>();

        // Assert that the multimap is empty
        assertEquals(multimap.size(), 0);
    }

    public void testConstructorWithMap() {
        // Create a new map
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");

        // Create a new multimap with the map
        Multimap<String, String> multimap = new SetMultimap<>(map);

        // Assert that the multimap contains the value
        assertTrue(multimap.containsValue("value"));
    }

    public void testPutDuplicated() {
        // Populate multimap with duplicated values
        Multimap<String, String> multimap = new SetMultimap<>();
        multimap.put("key", "value");
        multimap.put("key", "value");

        // Convert the collection to an array
        String[] values = multimap.get("key").toArray(new String[0]);

        // Assert that only one values was added
        assertEquals(values.length, 1);
        assertEquals(values[0], "value");
    }
}
