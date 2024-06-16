package org.jfrog.build.api.multiMap;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.util.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ListMultimap.class, name = "list"),
        @JsonSubTypes.Type(value = SetMultimap.class, name = "set"),
})
public abstract class Multimap<Key, Value> implements Serializable {
    private static final long serialVersionUID = 1L;
    Map<Key, Collection<Value>> multiMap = new HashMap<>();

    /**
     * Default constructor.
     */
    Multimap() {
    }

    /**
     * Constructor that accepts a map.
     *
     * @param map the map
     */
    Multimap(Map<Key, Value> map) {
        map.forEach(this::put);
    }

    /**
     * Put a key-value pair into the multimap.
     *
     * @param key   the key
     * @param value the value
     */
    public abstract void put(Key key, Value value);

    /**
     * Get all values for a key.
     *
     * @param key the key
     * @return a collection of values for the key
     */
    public Collection<Value> get(Key key) {
        return multiMap.get(key);
    }

    /**
     * Put all key-value pairs from a map into the multimap.
     *
     * @param map the map
     */
    public void putAll(Map<Key, Value> map) {
        for (Map.Entry<Key, Value> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Put all key-value pairs from a multimap into the multimap.
     *
     * @param multimap the multimap
     */
    public void putAll(Multimap<Key, Value> multimap) {
        for (Map.Entry<Key, Collection<Value>> entry : multimap.multiMap.entrySet()) {
            for (Value value : entry.getValue()) {
                put(entry.getKey(), value);
            }
        }
    }

    /**
     * Put all values for a key into the multimap.
     *
     * @param key    the key
     * @param values the values
     */
    public void putAll(Key key, Collection<Value> values) {
        for (Value value : values) {
            put(key, value);
        }
    }

    /**
     * Get all key-value pairs in the multimap.
     *
     * @return a set of key-value pairs
     */
    public Set<Map.Entry<Key, Value>> entries() {
        Set<Map.Entry<Key, Value>> entries = new HashSet<>();
        for (Map.Entry<Key, Collection<Value>> entry : multiMap.entrySet()) {
            for (Value value : entry.getValue()) {
                entries.add(new AbstractMap.SimpleEntry<>(entry.getKey(), value));
            }
        }
        return entries;
    }

    /**
     * Get the underlying map.
     *
     * @return the map
     */
    public Map<Key, Collection<Value>> asMap() {
        return multiMap;
    }

    /**
     * Get all keys in the multimap.
     *
     * @return a set of keys
     */
    public Set<Key> keySet() {
        return multiMap.keySet();
    }

    /**
     * Check if the multimap contains a value.
     *
     * @param value the value
     * @return true if the multimap contains the value
     */
    public boolean containsValue(Value value) {
        for (Collection<Value> values : multiMap.values()) {
            if (values.contains(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the number of key-value pairs in the multimap.
     *
     * @return the number of key-value pairs
     */
    public int size() {
        int size = 0;
        for (Collection<Value> values : multiMap.values()) {
            size += values.size();
        }
        return size;
    }

    /**
     * Check if the multimap is empty.
     *
     * @return true if the multimap is empty
     */
    public boolean isEmpty() {
        return multiMap.isEmpty();
    }

    /**
     * Clear the multimap.
     */
    public void clear() {
        multiMap.clear();
    }
}
