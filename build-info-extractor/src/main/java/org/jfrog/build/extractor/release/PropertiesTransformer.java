/*
 * Copyright (C) 2011 JFrog Ltd.
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

package org.jfrog.build.extractor.release;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

/**
 * Rewrites the given properties in the input properties file.
 *
 * @author Tomer Cohen
 */
public class PropertiesTransformer {

    private final File propertiesFile;
    private final Map<String, String> versionsByName;

    public PropertiesTransformer(File propertiesFile, Map<String, String> versionsByName) {
        this.propertiesFile = propertiesFile;
        this.versionsByName = versionsByName;
    }

    /**
     * {@inheritDoc}
     *
     * @return True in case the properties file was modified during the transformation. False otherwise.
     */
    public Boolean transform() throws IOException, InterruptedException {
        if (!propertiesFile.exists()) {
            throw new IllegalArgumentException("Couldn't find properties file: " + propertiesFile.getAbsolutePath());
        }
        String properties = Files.toString(propertiesFile, Charsets.UTF_8);
        StringBuilder result = new StringBuilder();
        BufferedReader reader = new BufferedReader(new StringReader(properties));
        String line;
        boolean modified = false;
        while ((line = reader.readLine()) != null) {
            if (line.contains("=")) {
                String[] keyValue = StringUtils.split(line, "=");
                String value = versionsByName.get(unescape(keyValue[0]));
                if (value != null) {
                    if (!value.equals(unescape(keyValue[1]))) {
                        modified = true;
                        keyValue[1] = escape(value, false);
                        line = keyValue[0] + "=" + keyValue[1];
                    }
                }
            }
            result.append(line).append("\n");
        }

        if (modified) {
            propertiesFile.delete();
            Files.write(result, propertiesFile, Charsets.UTF_8);
        }
        return modified;
    }

    // escape the value of the properties file so it will be in accordance with the properties spec.
    private static String escape(String str, boolean isKey) {
        int len = str.length();
        StringBuilder result = new StringBuilder(len * 2);
        for (int index = 0; index < len; index++) {
            char c = str.charAt(index);
            switch (c) {
                case ' ':
                    if (index == 0 || isKey) {
                        result.append('\\');
                    }
                    result.append(' ');
                    break;
                case '\\':
                    result.append("\\\\");
                    break;
                case '\t':
                    result.append("\\t");
                    break;
                case '\n':
                    result.append("\\n");
                    break;
                case '\r':
                    result.append("\\r");
                    break;
                case '\f':
                    result.append("\\f");
                    break;
                default:
                    if ("=: \t\r\n\f#!".indexOf(c) != -1) {
                        result.append('\\');
                    }
                    result.append(c);
            }
        }
        return result.toString();
    }

    private static String unescape(String str) {
        StringBuilder result = new StringBuilder(str.length());
        for (int index = 0; index < str.length(); ) {
            char c = str.charAt(index++);
            if (c == '\\') {
                c = str.charAt(index++);
                switch (c) {
                    case 't':
                        c = '\t';
                        break;
                    case 'r':
                        c = '\r';
                        break;
                    case 'n':
                        c = '\n';
                        break;
                    case 'f':
                        c = '\f';
                        break;
                }
            }
            result.append(c);
        }
        return result.toString();
    }
}
