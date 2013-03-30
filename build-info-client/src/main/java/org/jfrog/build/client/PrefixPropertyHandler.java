package org.jfrog.build.client;

import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.util.Log;

import java.util.Map;

/**
 * @author freds
 *         Date: 1/6/11
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

    public String getPrefix() {
        return prefix;
    }

    public String getStringValue(String key) {
        return getStringValue(key, null);
    }

    public String getStringValue(String key, String def) {
        String s = props.get(prefix + key);
        if (s == null) {
            s = def;
        }
        return s;
    }

    public void setStringValue(String key, String value) {
        if (value == null) {
            props.remove(prefix + key);
        } else {
            props.put(prefix + key, value);
        }
    }

    public Boolean getBooleanValue(String key, Boolean def) {
        String s = props.get(prefix + key);
        // TODO: throw exception if not true or false. If prop set to something else
        Boolean result = (s == null) ? null : Boolean.parseBoolean(s);
        if (result == null) {
            result = def;
        }
        return result;
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
        Integer result;
        String s = props.get(prefix + key);
        if (s != null && !StringUtils.isNumeric(s)) {
            log.debug("Property '" + prefix + key + "' is not of numeric value '" + s + "'");
            result = null;
        } else {
            result = (s == null) ? null : Integer.parseInt(s);
        }
        if (result == null) {
            result = def;
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

}
