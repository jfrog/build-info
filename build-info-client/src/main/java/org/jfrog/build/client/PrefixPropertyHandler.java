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
    private final PrefixPropertyHandler parent;
    private final String prefix;

    public PrefixPropertyHandler(Log log, Map<String, String> props) {
        this(log, props, "", null);
    }

    public PrefixPropertyHandler(PrefixPropertyHandler root, String prefix) {
        this(root.log, root.props, prefix, null);
    }

    private PrefixPropertyHandler(Log log, Map<String, String> props, String prefix, PrefixPropertyHandler parent) {
        this.log = log;
        this.props = props;
        this.parent = parent;
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    protected String getStringValue(String key) {
        String s = props.get(prefix + key);
        if (s == null && parent != null) {
            return parent.getStringValue(prefix + key);
        }
        return s;
    }

    protected void setStringValue(String key, String value) {
        if (value == null) {
            props.remove(prefix + key);
        } else {
            props.put(prefix + key, value);
        }
    }

    protected Boolean getBooleanValue(String key) {
        String s = props.get(prefix + key);
        if (s == null && parent != null) {
            return parent.getBooleanValue(prefix + key);
        }
        // TODO: throw exception if not true or false. If prop set to something else
        return (s == null) ? null : Boolean.parseBoolean(s);
    }

    protected void setBooleanValue(String key, Boolean value) {
        if (value == null) {
            props.remove(prefix + key);
        } else {
            props.put(prefix + key, value.toString());
        }
    }

    protected Integer getIntegerValue(String key) {
        String s = props.get(prefix + key);
        if (s == null) {
            if (parent != null) {
                return parent.getIntegerValue(prefix + key);
            }
            return null;
        }
        if (!StringUtils.isNumeric(s)) {
            log.debug("Property '" + prefix + key + "' is not of numeric value '" + s + "'");
            return null;
        }

        return (s == null) ? null : Integer.parseInt(s);
    }

    protected void setIntegerValue(String key, Integer value) {
        if (value == null) {
            props.remove(prefix + key);
        } else {
            props.put(prefix + key, value.toString());
        }
    }

}
