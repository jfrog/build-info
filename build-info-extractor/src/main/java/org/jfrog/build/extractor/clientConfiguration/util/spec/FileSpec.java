package org.jfrog.build.extractor.clientConfiguration.util.spec;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.ArrayUtils;

import java.io.IOException;
import java.util.Arrays;

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
    private String build;
    private String explode;
    private String[] excludePatterns;
    private String[] sortBy;
    private String sortOrder;
    private String limit;
    private String offset;


    public enum SpecType {
        BUILD,
        PATTERN,
        AQL
    }

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

    public String getBuild() {
        return build;
    }

    public void setBuild(String build) {
        this.build = build;
    }

    public String getExplode() {
        return explode;
    }

    public void setExplode(String explode) {
        this.explode = explode;
    }

    public String[] getExcludePatterns() {
        return excludePatterns;
    }

    public void setExcludePatterns(String[] excludePatterns) {
        this.excludePatterns = excludePatterns;
    }

    public String getExcludePattern(int index) {
        return excludePatterns[index];
    }

    public void setExcludePattern(String excludePattern, int index) {
        this.excludePatterns[index] = excludePattern;
    }

    public String[] getSortBy() {
        if (sortBy != null) {
            return sortBy;
        }
        return ArrayUtils.EMPTY_STRING_ARRAY;
    }

    public void setSortBy(String[] sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getOffset() {
        return offset;
    }

    public void setOffset(String offset) {
        this.offset = offset;
    }

    public String getLimit() {
        return limit;
    }

    public void setLimit(String resultLimit) {
        this.limit = resultLimit;
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
                ", build='" + build + '\'' +
                ", explode='" + explode + '\'' +
                ", excludePatterns='" + Arrays.toString(excludePatterns) + '\'' +
                ", sortBy='" + Arrays.toString(sortBy) + '\'' +
                ", sortOrder='" + sortOrder + '\'' +
                ", offset='" + offset + '\'' +
                ", limit='" + limit + '\'' +
                '}';
    }

    public SpecType getSpecType() throws IOException {
        if (StringUtils.isNotEmpty(this.build) && StringUtils.isEmpty(getAql()) && (StringUtils.isEmpty(this.pattern) || this.pattern.equals("*"))) {
            return SpecType.BUILD;
        } else if (StringUtils.isNotEmpty(this.pattern)) {
            return SpecType.PATTERN;
        } else if (StringUtils.isNotEmpty(getAql())) {
            return SpecType.AQL;
        }
        return null;
    }
}
