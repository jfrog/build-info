package org.jfrog.build.api.util;

import org.testng.TestException;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import static org.testng.Assert.*;

/**
 * Tests the utility functions in the CommonUtils class
 */
@Test
public class CommonUtilsTest {

    public void testFilterMapValues() {
        Map<String, String> expectedMap = new HashMap<>();
        expectedMap.put("first", "good");
        expectedMap.put("second", "good");
        Map<String, String> filterMap = new HashMap<>(expectedMap);
        filterMap.put("third", "bad");
        filterMap.put("forth", "bad");

        assertNotEquals(filterMap, expectedMap, "Unexpected maps initializing.");
        filterMap = CommonUtils.filterMapValues(filterMap, value -> value.equals("good"));
        assertEquals(filterMap, expectedMap, "Unexpected map filtering.");

        assertEquals(new HashMap<>(), CommonUtils.filterMapValues(new HashMap<>(), value -> value.equals("good")),
                "Unexpected empty map filtering.");

        // Function should filter null elements
        try {
            Map<String, Object> expectedMap2 = new HashMap<>();
            expectedMap2.put("first", "string 1");
            expectedMap2.put("second", "s");
            Map<String, Object> withNull = new HashMap<>(expectedMap2);
            withNull.put("third", null);
            withNull.put("forth", 2);
            withNull.put("fifth", "none of the letter");

            withNull = CommonUtils.filterMapValues(withNull, value -> value.toString().contains("s"));
            assertEquals(withNull, expectedMap2, "Unexpected map filtering.");

        } catch (NullPointerException e) {
            throw new TestException("Unexpected NullPointerException. Function should filter null elements.");
        }
    }

    public void testFilterMapKeys() {
        Map<String, Integer> expectedMap = new HashMap<>();
        expectedMap.put("goodOne", 1);
        expectedMap.put("goodTwo", 2);
        Map<String, Integer> filterMap = new HashMap<>(expectedMap);
        filterMap.put("badOne", 1);
        filterMap.put("badTwo", 2);

        assertEquals(new HashMap<>(), CommonUtils.filterMapKeys(new HashMap<>(), key -> key.equals("good")),
                "Unexpected empty map filtering.");

        assertNotEquals(filterMap, expectedMap, "Unexpected maps initializing.");
        filterMap = CommonUtils.filterMapKeys(filterMap, key -> key.startsWith("good"));
        assertEquals(filterMap, expectedMap, "Unexpected map filtering.");

        // Function should filter null elements
        try {
            Map<Object, String> expectedMap2 = new HashMap<>();
            expectedMap2.put("1", "string 1");
            expectedMap2.put(1, "s");
            Map<Object, String> withNull = new HashMap<>(expectedMap2);
            withNull.put(null, "a");
            withNull.put(2, "2");

            withNull = CommonUtils.filterMapKeys(withNull, key -> key.toString().equals("1"));
            assertEquals(withNull, expectedMap2, "Unexpected map filtering.");

        } catch (NullPointerException e) {
            throw new TestException("Unexpected NullPointerException. Function should filter null elements.");
        }
    }

    public void testEntriesOnlyOnLeftMap() {
        Map<String, Integer> baseMap = new HashMap<>();
        baseMap.put("badOne", 1);
        baseMap.put("badTwo", 2);
        Map<String, Integer> expectedMap = new HashMap<>();
        expectedMap.put("left", 1);

        Map<String, Integer> leftMap = new HashMap<>(baseMap);
        leftMap.put("left", expectedMap.get("left"));
        Map<String, Integer> rightMap = new HashMap<>(baseMap);
        rightMap.put("right", expectedMap.get("left"));

        assertEquals(CommonUtils.entriesOnlyOnLeftMap(leftMap, rightMap), expectedMap, "Unexpected entries on left map.");
    }

    public void testConcatLists() {
        assertEquals(CommonUtils.concatLists(Arrays.asList(1, 2), Arrays.asList(3, 2)), Arrays.asList(1, 2, 3, 2),
                "Unexpected Integer lists concatenation result.");

        assertEquals(CommonUtils.concatLists(new ArrayList<>(), Arrays.asList(0, 1)), Arrays.asList(0, 1),
                "Unexpected empty list and integer list concatenation result.");

        assertEquals(CommonUtils.concatLists(Arrays.asList("strings", "seem"), Collections.singletonList("fine")),
                Arrays.asList("strings", "seem", "fine"), "Unexpected string lists concatenation result.");
    }

    public void testTransformList() {
        List<String> addedSuffix = CommonUtils.transformList(Arrays.asList("life", "is", "like"), e -> e + "-chocolates");
        assertEquals(addedSuffix, Arrays.asList("life-chocolates", "is-chocolates", "like-chocolates"),
                "Unexpected string suffix addition result.");

        List<String> conversion = CommonUtils.transformList(Arrays.asList(1, 2, 3), Object::toString);
        assertEquals(conversion, Arrays.asList("1", "2", "3"),
                "Unexpected int to string conversion result.");

        assertEquals(CommonUtils.transformList(new ArrayList<>(), e -> "empty"), new ArrayList<>(),
                "Unexpected empty list transform result.");
    }

    public void testGetFirstSatisfying() {
        List<String> list = Arrays.asList("life", "is", "like", "a box of", "box of chocolates");
        assertEquals(CommonUtils.getFirstSatisfying(list, e -> e.contains("box"), "def"), "a box of",
                "Unexpected first satisfying element.");

        List<Integer> integers = Arrays.asList(435, 345, 22, 4346, 333);
        Integer first = CommonUtils.getFirstSatisfying(integers, i -> i > 5000, 5555);
        Integer expected = 5555;
        assertEquals(first, expected, "Expected to get the default value when no element is satisfying.");

        List<Integer> empty = new ArrayList<>();
        first = CommonUtils.getFirstSatisfying(empty, i -> i > 1, 0);
        expected = 0;
        assertEquals(first, expected, "Expected to get the default value when list is empty.");

        // Function should filter null elements
        try {
            List<Integer> withNull = Arrays.asList(3, null, 4);
            first = CommonUtils.getFirstSatisfying(withNull, i -> i % 2 == 0, 0);
            expected = 4;
            assertEquals(first, expected, "Unexpected first satisfying element.");
        } catch (NullPointerException e) {
            throw new TestException("Unexpected NullPointerException. Function should filter null elements.");
        }
    }

    public void testIsAnySatisfying() {
        List<String> list = Arrays.asList("life", "is", "like", "a box of", "box of chocolates");
        assertTrue(CommonUtils.isAnySatisfying(list, e -> e.contains("box")),
                "Expected list to satisfy.");

        List<Integer> integers = Arrays.asList(435, 345, 22, 4346, 333);
        assertFalse(CommonUtils.isAnySatisfying(integers, i -> i > 5000),
                "Expected list to not satisfy.");

        assertFalse(CommonUtils.isAnySatisfying(new ArrayList<>(), i -> i.equals(111)),
                "Expected empty list to not satisfy.");

        // Function should filter null elements
        try {
            List<Integer> withNull = Arrays.asList(3, null, 4);
            assertTrue(CommonUtils.isAnySatisfying(withNull, i -> i % 2 == 0),
                    "Expected list to satisfy.");

        } catch (NullPointerException e) {
            throw new TestException("Unexpected NullPointerException. Function should filter null elements.");
        }
    }

    public void testFilterCollection() {
        List<String> list = Arrays.asList("life", "is", "like", "a box of", "box of chocolates");
        List<String> expected = Arrays.asList("a box of", "box of chocolates");
        assertEquals(CommonUtils.filterCollection(list, e -> e.contains("box")), expected,
                "Unexpected collection filter result.");

        Set<Integer> integers = CommonUtils.newHashSet(435, 345, 22, 4346, 333);
        Set<Integer> expected2 = CommonUtils.newHashSet(435, 345, 4346);
        assertEquals(CommonUtils.filterCollection(integers, i -> i > 340), expected2,
                "Unexpected collection filter result.");

        assertEquals(CommonUtils.filterCollection(integers, i -> i.equals(111)), new HashSet<>(),
                "Expected empty list");

        // Function should filter null elements
        try {
            List<Integer> withNull = Arrays.asList(3, null, 4);
            assertEquals(CommonUtils.filterCollection(withNull, i -> i % 2 == 0), Collections.singletonList(4),
                    "Unexpected collection filter result.");

        } catch (NullPointerException e) {
            throw new TestException("Unexpected NullPointerException. Function should filter null elements.");
        }
    }

    public void testGetLast() {
        List<String> list = Arrays.asList("returns", "the", "last");
        assertEquals(CommonUtils.getLast(list), "last",
                "Unexpected last item.");

        List<String> withNull = Arrays.asList("returns", null, "last");
        assertEquals(CommonUtils.getLast(withNull), "last",
                "Unexpected last item.");
    }

    public void testGetOnlyElement() {
        List<String> single = Collections.singletonList("only");
        assertEquals(CommonUtils.getOnlyElement(single), "only",
                "Expected the only item.");

        List<String> two = Arrays.asList("not", "alone");
        assertThrows(IllegalArgumentException.class, () -> CommonUtils.getOnlyElement(two));
    }

    public void testWriteReadByCharset() throws IOException {
        File file = File.createTempFile("temp", "write-read-test");
        String expected = "Please Check\rThis\rOut\n";
        CommonUtils.writeByCharset(expected, file, Charset.forName("utf-8"));
        String output = CommonUtils.readByCharset(file, Charset.forName("utf-8"));
        assertEquals(expected, output, "Unexpected string read from file");
    }

    public void testNewHashSet() {
        Set<String> set = CommonUtils.newHashSet("a", "new", "set");
        Set<String> expected = new HashSet<>();
        expected.add("new");
        expected.add("a");
        expected.add("set");
        assertEquals(set, expected, "Unexpected set construction");

        assertEquals(CommonUtils.newHashSet(), new HashSet<>(), "Unexpected set construction");
    }
}
