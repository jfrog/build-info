package org.jfrog.build.extractor.clientConfiguration.util.spec;

import java.io.IOException;

/**
 * Created by romang on 4/20/16.
 */
public class FileSpec {
    private Aql aql;
    private String pattern;
    private String target;
    private String props;
    private String recursive;
    private String flat;
    private String regexp;

    public String getAql() throws IOException {
        if (aql != null) {
            return aql.getFind();
        }
        return null;
    }

    public String getPattern() {
        return pattern;
    }

    public String getTarget() {
        return target;
    }

    public void setAql(Aql aql) {
        this.aql = aql;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getProps() {
        return props;
    }

    public void setProps(String props) {
        this.props = props;
    }

    public String getRecursive() {
        return recursive;
    }

    public void setRecursive(String recursive) {
        this.recursive = recursive;
    }

    public String getRegexp() {
        return regexp;
    }

    public void setRegexp(String regexp) {
        this.regexp = regexp;
    }

    public String getFlat() {
        return flat;
    }

    public void setFlat(String flat) {
        this.flat = flat;
    }

    @Override
    public String toString() {
        return "FileSpec{" +
                "aql=" + aql +
                ", pattern='" + pattern + '\'' +
                ", target='" + target + '\'' +
                ", props='" + props + '\'' +
                ", recursive='" + recursive + '\'' +
                ", flat='" + flat + '\'' +
                ", regexp='" + regexp + '\'' +
                '}';
    }
}
