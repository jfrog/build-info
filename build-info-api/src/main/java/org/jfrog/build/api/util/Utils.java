package org.jfrog.build.api.util;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSetMultimap;

import java.util.ArrayList;
import java.util.List;

public class Utils {
    /**
     * Build string of properties in the following form:
     * key=value;key=value...
     *
     * @param properties - keys and values to concat
     * @return
     */
    public static String buildPropertiesString(ArrayListMultimap<String, String> properties) {
        StringBuilder props = new StringBuilder();
        List<String> keys = new ArrayList<String>(properties.keySet());
        for (int i = 0; i < keys.size(); i++) {
            props.append(keys.get(i)).append("=");
            List<String> values = properties.get(keys.get(i));
            for (int j = 0; j < values.size(); j++) {
                props.append(values.get(j));
                if (j != values.size() - 1) {
                    props.append(",");
                }
            }
            if (i != keys.size() - 1) {
                props.append(";");
            }
        }
        return props.toString();
    }
}
