package org.jfrog.build.api.multiMap;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

/**
 * A multimap that uses a {@link LinkedList} to store the values.
 */
public class ListMultimap<Key, Value> extends Multimap<Key, Value> {

    /**
     * Default constructor.
     */
    public ListMultimap() {
        super();
    }

    /**
     * Constructor that accepts a map.
     *
     * @param map the map
     */
    public ListMultimap(Map<Key, Value> map) {
        super(map);
    }

    /**
     * Put a key-value pair into the multimap.
     *
     * @param key   the key
     * @param value the value
     */
    public void put(Key key, Value value) {
        Collection<Value> currentValue = multiMap.getOrDefault(key, new LinkedList<>());
        currentValue.add(value);
        multiMap.put(key, currentValue);
    }
}
