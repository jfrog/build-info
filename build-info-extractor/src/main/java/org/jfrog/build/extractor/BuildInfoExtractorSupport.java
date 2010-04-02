package org.jfrog.build.extractor;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import org.jfrog.build.api.constants.BuildInfoProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * @author Noam Y. Tenne
 */
public abstract class BuildInfoExtractorSupport<C, O> implements BuildInfoExtractor<C, O> {

    /**
     * Collect system properties and properties from the  {@link org.jfrog.build.api.constants.BuildInfoProperties#PROP_PROPS_FILE}
     * file.
     * <p/>
     * The caller is supposed to inject the build properties into the output (e.g.: adding them to the Build object if
     * the output of the extractor is a {@link org.jfrog.build.api.Build} instance, or saving them into a generated
     * buildInfo xml output file, if the output is a path to this file.
     *
     * @return
     */
    public Properties getBuildInfoProperties() throws IOException {
        //TODO: [by tc] extract thge props from org.jfrog.build.api.constants.BuildInfoProperties#PROP_PROPS_FILE (if
        // exists) and from any system props that begin with
        // org.jfrog.build.api.constants.BuildInfoProperties.BUILD_INFO_PROP_PREFIX
        Properties props = new Properties();
        File propertiesFile = new File(BuildInfoProperties.PROP_PROPS_FILE);
        if (propertiesFile.exists()) {
            InputStream inputStream = new FileInputStream(propertiesFile);
            props.load(inputStream);
            Map<Object, Object> filteredMap = Maps.filterKeys(props, new Predicate<Object>() {
                public boolean apply(Object input) {
                    return isPropertyValid(input);
                }
            });

            props = new Properties();
            props.putAll(filteredMap);
        }
        // now add all the relevant system props.
        Properties systemProperties = System.getProperties();
        Map<Object, Object> filteredSystemProps = Maps.filterKeys(systemProperties, new Predicate<Object>() {
            public boolean apply(Object input) {
                return isPropertyValid(input);
            }
        });
        props.putAll(filteredSystemProps);
        return props;
    }

    private boolean isPropertyValid(Object input) {
        String key = (String) input;
        return key.startsWith(BuildInfoProperties.BUILD_INFO_PROP_PREFIX);
    }
}