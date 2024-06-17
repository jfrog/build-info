package org.jfrog.build.api.multiMap;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class MultimapTest {

    @DataProvider
    private Object[][] testCases() {
        return new Object[][]{
                {new ListMultimap<String, String>()},
                {new SetMultimap<String, String>()}
        };
    }

    @Test(dataProvider = "testCases")
    public void testPut(Multimap<String, String> multimap) {
        // Populate multimap with values
        multimap.put("key1", "value1");
        multimap.put("key2", "value2");
        multimap.put("key2", "value3");

        // Assert that the values were added
        assertTrue(multimap.get("key1").contains("value1"));
        assertTrue(multimap.get("key2").contains("value2"));
        assertTrue(multimap.get("key2").contains("value3"));
    }

    @Test(dataProvider = "testCases")
    public void testPutAllMultimap(Multimap<String, String> multimap) {
        // Populate multimap with values
        Multimap<String, String> otherMultimap = new ListMultimap<>();
        otherMultimap.put("key1", "value1");
        otherMultimap.put("key2", "value2");
        otherMultimap.put("key2", "value3");

        // Add the values to the multimap
        multimap.putAll(otherMultimap);

        // Assert that the values were added
        assertTrue(multimap.get("key1").contains("value1"));
        assertTrue(multimap.get("key2").contains("value2"));
        assertTrue(multimap.get("key2").contains("value3"));
    }

    @Test(dataProvider = "testCases")
    public void testPutAllCollection(Multimap<String, String> multimap) {
        // Populate multimap with values
        List<String> otherCollection = new ArrayList<>();
        otherCollection.add("value1");
        otherCollection.add("value2");

        // Add the values to the multimap
        multimap.putAll("key", otherCollection);

        // Assert that the values were added
        assertTrue(multimap.get("key").contains("value1"));
        assertTrue(multimap.get("key").contains("value2"));
    }

    @Test(dataProvider = "testCases")
    public void testPutAllMap(Multimap<String, String> multimap) {
        // Populate multimap with values
        Map<String, String> otherMap = new HashMap<>();
        otherMap.put("key1", "value1");
        otherMap.put("key2", "value2");

        // Add the values to the multimap
        multimap.putAll(otherMap);

        // Assert that the values were added
        assertTrue(multimap.get("key1").contains("value1"));
        assertTrue(multimap.get("key2").contains("value2"));
    }

    @Test(dataProvider = "testCases")
    public void testEntries(Multimap<String, String> multimap) {
        // Populate multimap with values
        multimap.put("key1", "value1");
        multimap.put("key2", "value2");

        // Assert that the entries were added
        assertTrue(multimap.entries().contains(new HashMap.SimpleEntry<>("key1", "value1")));
        assertTrue(multimap.entries().contains(new HashMap.SimpleEntry<>("key2", "value2")));
    }

    @Test(dataProvider = "testCases")
    public void testAsMap(Multimap<String, String> multimap) {
        // Populate multimap with values
        multimap.put("key1", "value1");
        multimap.put("key2", "value2");

        // Assert that the map contains the keys
        assertTrue(multimap.asMap().containsKey("key1"));
        assertTrue(multimap.asMap().containsKey("key2"));
    }

    @Test(dataProvider = "testCases")
    public void testKeySet(Multimap<String, String> multimap) {
        // Populate multimap with values
        multimap.put("key1", "value1");
        multimap.put("key2", "value2");

        // Assert that the key set contains the keys
        assertTrue(multimap.keySet().contains("key1"));
        assertTrue(multimap.keySet().contains("key2"));
    }

    @Test(dataProvider = "testCases")
    public void testContainsValue(Multimap<String, String> multimap) {
        // Populate multimap with values
        multimap.put("key1", "value1");
        multimap.put("key2", "value2");

        // Assert that the multimap contains the values
        assertTrue(multimap.containsValue("value1"));
        assertTrue(multimap.containsValue("value2"));
    }

    @Test(dataProvider = "testCases")
    public void testSize(Multimap<String, String> multimap) {
        // Populate multimap with values
        multimap.put("key1", "value1");
        multimap.put("key2", "value2");

        // Assert that the size is correct
        assertEquals(multimap.size(), 2);
    }

    @Test(dataProvider = "testCases")
    public void testIsEmpty(Multimap<String, String> multimap) {
        assertTrue(multimap.isEmpty());
    }

    @Test(dataProvider = "testCases")
    public void testClear(Multimap<String, String> multimap) {
        // Populate multimap with values
        multimap.put("key1", "value1");
        multimap.put("key2", "value2");

        // Clear the multimap
        multimap.clear();

        // Assert that the multimap is empty
        assertTrue(multimap.isEmpty());
    }
}
