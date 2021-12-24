package org.jfrog.build.extractor.clientConfiguration;

import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.Log;

import java.util.Map;

import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.ARTIFACTORY_PREFIX;

/**
 * @author freds
 * Date: 1/6/11
 */
public class PrefixPropertyHandler {
    protected final Map<String, String> props;
    protected final Log log;
    private final String prefix;

    public PrefixPropertyHandler(Log log, Map<String, String> props) {
        this(log, props, "");
    }

    public PrefixPropertyHandler(PrefixPropertyHandler root, String prefix) {
        this(root.log, root.props, prefix);
    }

    private PrefixPropertyHandler(Log log, Map<String, String> props, String prefix) {
        this.log = log;
        this.props = props;
        this.prefix = prefix;
    }

    public Log getLog() {
        return log;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getDeprecatedPrefix() {
        return ARTIFACTORY_PREFIX + prefix;
    }

    public String getStringValue(String key) {
        return getStringValue(key, null);
    }

    public String getStringValue(String key, String def) {
        // First, get value using deprecated key.
        // This check must be first, otherwise, build.gradle properties will override the CI (e.g Jenkins / teamcity) properties.
        String value = props.get(ARTIFACTORY_PREFIX + prefix + key);
        if (StringUtils.isNotBlank(value)) {
            return value;
        }
        // Fallback to none deprecated key.
        value = props.get(prefix + key);
        if (StringUtils.isNotBlank(value)) {
            return value;
        }

        return def;
    }

    public void setStringValue(String key, String value) {
        if (value == null) {
            props.remove(prefix + key);
        } else {
            props.put(prefix + key, value);
        }
    }

    public Boolean getBooleanValue(String key, Boolean def) {
        // First, get value using deprecated key.
        // This check must be first, otherwise, build.gradle properties will override the CI (e.g Jenkins / teamcity) properties.
        String value = props.get(ARTIFACTORY_PREFIX + prefix + key);
        // TODO: throw exception if not true or false. If prop set to something else
        Boolean result = (value == null) ? null : Boolean.parseBoolean(value);
        if (result != null) {
            return result;
        }
        // Fallback to none deprecated key.
        value = props.get(prefix + key);
        result = (value == null) ? null : Boolean.parseBoolean(value);
        if (result != null) {
            return result;
        }
        return def;
    }

    public void setBooleanValue(String key, Boolean value) {
        if (value == null) {
            props.remove(prefix + key);
        } else {
            props.put(prefix + key, value.toString());
        }
    }

    public Integer getIntegerValue(String key) {
        return getIntegerValue(key, null);
    }

    public Integer getIntegerValue(String key, Integer def) {
        // First, get value using deprecated key.
        // This check must be first, otherwise, build.gradle properties will override the CI (e.g Jenkins / teamcity) properties.
        Integer result = getInteger(key, ARTIFACTORY_PREFIX + prefix);
        if (result != null) {
            return result;
        }
        // Fallback to none deprecated key.
         result = getInteger(key, prefix);
        if (result != null) {
            return result;
        }
        return def;
    }

    private Integer getInteger(String key, String targetPrefix) {
        Integer result;
        String s = props.get(targetPrefix + key);
        if (s != null && !StringUtils.isNumeric(s)) {
            log.debug("Property '" + targetPrefix + key + "' is not of numeric value '" + s + "'");
            result = null;
        } else {
            result = (s == null) ? null : Integer.parseInt(s);
        }
        return result;
    }

    public void setIntegerValue(String key, Integer value) {
        if (value == null) {
            props.remove(prefix + key);
        } else {
            props.put(prefix + key, value.toString());
        }
    }

    public Map<String, String> getProps() {
        return props;
    }
}
