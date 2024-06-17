package org.jfrog.build.api.multiMap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * A multimap that uses a {@link HashSet} to store the values.
 */
public class SetMultimap<Key, Value> extends Multimap<Key, Value> {
    public SetMultimap() {
        super();
    }

    /**
     * Constructor that accepts a map.
     *
     * @param map the map
     */
    public SetMultimap(Map<Key, Value> map) {
        super(map);
    }

    /**
     * Put a key-value pair into the multimap.
     *
     * @param key   the key
     * @param value the value
     */
    public void put(Key key, Value value) {
        Collection<Value> currentValue = multiMap.getOrDefault(key, new HashSet<>());
        currentValue.add(value);
        multiMap.put(key, currentValue);
    }
}
