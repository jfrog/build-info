package org.jfrog.build.extractor.ci;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Contains the build module information
 *
 * @author Noam Y. Tenne
 */
public class Module extends BaseBuildBean {

    private String type;

    private String id;

    private String repository;

    private String md5;

    private String sha1;

    private List<Artifact> artifacts;

    private List<Artifact> excludedArtifacts;

    private List<Dependency> dependencies;

    /**
     * Returns the type of the module
     *
     * @return Module type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of the module
     *
     * @param type Module type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the ID of the module
     *
     * @return Module ID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the ID of the module
     *
     * @param id Module ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets the repository of the module
     *
     * @param repository Module repository
     */
    public void setRepository(String repository) {
        this.repository = repository;
    }

    /**
     * Returns the repository of the module
     *
     * @return Module repository
     */
    public String getRepository() {
        return repository;
    }

    /**
     * Sets the sha1 of the module
     *
     * @param sha1 Module sha1
     */
    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    /**
     * Returns the sha1 of the module
     *
     * @return Module sha1
     */
    public String getSha1() {
        return sha1;
    }

    /**
     * Sets the md5 of the module
     *
     * @param md5 Module md5
     */
    public void setMd5(String md5) {
        this.md5 = md5;
    }

    /**
     * Returns the md5 of the module
     *
     * @return Module md5
     */
    public String getMd5() {
        return md5;
    }

    /**
     * Returns the list of artifacts that have been deployed by the module
     *
     * @return Module deployed artifacts
     */
    public List<Artifact> getArtifacts() {
        return artifacts;
    }

    /**
     * Sets the list of artifacts that have been deployed by the module
     *
     * @param artifacts Module deployed artifacts
     */
    public void setArtifacts(List<Artifact> artifacts) {
        this.artifacts = artifacts;
    }

    /**
     * Returns the list of excluded_artifacts that haven't been deployed by the module
     *
     * @return Module deployed artifacts
     */
    public List<Artifact> getExcludedArtifacts() {
        return excludedArtifacts;
    }

    /**
     * Returns the dependencies of the module
     *
     * @return Module dependencies
     */
    public List<Dependency> getDependencies() {
        return dependencies;
    }

    /**
     * Sets the dependencies of the module
     *
     * @param dependencies Module dependencies
     */
    public void setDependencies(List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * Sets the list of artifacts that haven't been deployed by the module
     *
     * @param excludedArtifacts Module excluded artifacts
     */
    public void setExcludedArtifacts(List<Artifact> excludedArtifacts) {
        this.excludedArtifacts = excludedArtifacts;
    }

    /**
     * Append other module to this module
     *
     * @param other Module to append
     */
    public void append(Module other) {
        artifacts = appendBuildFileLists(artifacts, other.getArtifacts());
        excludedArtifacts = appendBuildFileLists(excludedArtifacts, other.getExcludedArtifacts());
        dependencies = appendBuildFileLists(dependencies, other.getDependencies());
        type = StringUtils.defaultIfEmpty(type, other.type);
        repository = StringUtils.defaultIfEmpty(repository, other.repository);
        md5 = StringUtils.defaultIfEmpty(md5, other.md5);
        sha1 = StringUtils.defaultIfEmpty(sha1, other.sha1);
    }

    private <T extends BaseBuildBean> List<T> appendBuildFileLists(List<T> a, List<T> b) {
        if (a == null && b == null) {
            return null;
        }
        return Stream.of(Optional.ofNullable(a).orElseGet(Collections::emptyList), Optional.ofNullable(b).orElseGet(Collections::emptyList))
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Module module = (Module) o;

        return StringUtils.equals(getType(), module.getType()) &&
                StringUtils.equals(getId(), module.getId()) &&
                StringUtils.equals(getRepository(), module.getRepository()) &&
                StringUtils.equals(getSha1(), module.getSha1()) &&
                StringUtils.equals(getMd5(), module.getMd5()) &&
                ArrayUtils.isEquals(getArtifacts(), module.getArtifacts()) &&
                ArrayUtils.isEquals(getExcludedArtifacts(), module.getExcludedArtifacts()) &&
                ArrayUtils.isEquals(getDependencies(), module.getDependencies());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), getId(), getRepository(), getSha1(), getMd5(), getArtifacts(), getExcludedArtifacts(), getDependencies());
    }
}