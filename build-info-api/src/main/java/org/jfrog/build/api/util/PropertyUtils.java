/*
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.build.api.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Utilities for filtering properties
 *
 * @author Yoav L.
 */
public abstract class PropertyUtils {

    /**
     * Filter the provided properties according to the set of include/exclude keys.
     *
     * @param original The original properties
     * @param includes A set of keys to keep
     * @param excludes A set of keys to filter out (always overrule includes)
     * @return
     */
    @SuppressWarnings({"unchecked"})
    public static Properties filterProperties(Properties original, Collection<String> includes,
            Collection<String> excludes) {
        HashMap map = new HashMap();
        map.putAll(original);
        return filterProperties(map, includes, excludes);
    }

    /**
     * Filter the provided properties according to the set of include/exclude keys.
     *
     * @param original The original properties
     * @param includes A set of keys to keep
     * @param excludes A set of keys to filter out (always overrule includes)
     * @return
     */
    public static Properties filterProperties(Map<String, String> original, Collection<String> includes,
            Collection<String> excludes) {
        //TODO: [by ] impl`
        return null;
    }
}