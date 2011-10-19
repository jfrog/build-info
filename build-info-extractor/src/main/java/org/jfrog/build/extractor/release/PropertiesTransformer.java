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
import org.apache.commons.io.IOUtils;
import org.jfrog.build.extractor.EolDetectingInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

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
        Properties properties = new Properties();
        EolDetectingInputStream eolDetectingInputStream = null;
        try {
            eolDetectingInputStream = new EolDetectingInputStream(new FileInputStream(propertiesFile));
            properties.load(eolDetectingInputStream);
        } finally {
            IOUtils.closeQuietly(eolDetectingInputStream);
        }
        String eol = eolDetectingInputStream.getEol();
        boolean hasEol = !"".equals(eol);

        StringBuilder resultBuilder = new StringBuilder();
        boolean modified = false;
        Enumeration<?> propertyNames = properties.propertyNames();
        while (propertyNames.hasMoreElements()) {
            String propertyName = (String) propertyNames.nextElement();
            String propertyValue = properties.getProperty(propertyName);

            StringBuilder lineBuilder = new StringBuilder(propertyName).append("=");

            String newPropertyValue = versionsByName.get(propertyName);
            if ((newPropertyValue != null) && !newPropertyValue.equals(propertyValue)) {
                if (!modified) {
                    modified = true;
                }
                lineBuilder.append(newPropertyValue);
            } else {
                lineBuilder.append(propertyValue);
            }
            resultBuilder.append(lineBuilder.toString());
            if (hasEol) {
                resultBuilder.append(eol);
            }
        }

        if (modified) {
            propertiesFile.delete();
            String toWrite = resultBuilder.toString();
            Files.write(toWrite, propertiesFile, Charsets.UTF_8);
        }

        return modified;
    }
}
