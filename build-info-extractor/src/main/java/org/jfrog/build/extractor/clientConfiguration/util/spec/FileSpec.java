package org.jfrog.build.extractor.clientConfiguration.util.spec;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

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
    private String project;
    private String explode;
    // Deprecated, Use Exclusions instead.
    private String[] excludePatterns;
    private String[] exclusions;
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

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getExplode() {
        return explode;
    }

    public void setExplode(String explode) {
        this.explode = explode;
    }

    /**
     * @deprecated Use {@link org.jfrog.build.extractor.clientConfiguration.util.spec.FileSpec#getExclusions()} instead.
     */
    @Deprecated
    public String[] getExcludePatterns() {
        return excludePatterns;
    }

    /**
     * @deprecated Use {@link org.jfrog.build.extractor.clientConfiguration.util.spec.FileSpec#setExclusions(String[] exclusions)} instead.
     */
    @Deprecated
    public void setExcludePatterns(String[] excludePatterns) {
        this.excludePatterns = excludePatterns;
    }

    @Deprecated
    public String getExcludePattern(int index) {
        return excludePatterns[index];
    }

    @Deprecated
    public void setExcludePattern(String excludePattern, int index) {
        this.excludePatterns[index] = excludePattern;
    }

    public String[] getSortBy() {
        if (sortBy != null) {
            return sortBy;
        }
        return ArrayUtils.EMPTY_STRING_ARRAY;
    }

    public String[] getExclusions() {
        return exclusions;
    }

    public void setExclusions(String[] exclusions) {
        this.exclusions = exclusions;
    }

    public String getExclusion(int index) {
        return exclusions[index];
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
                ", project='" + project + '\'' +
                ", explode='" + explode + '\'' +
                ", excludePatterns='" + Arrays.toString(excludePatterns) + '\'' +
                ", exclusions='" + Arrays.toString(exclusions) + '\'' +
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
