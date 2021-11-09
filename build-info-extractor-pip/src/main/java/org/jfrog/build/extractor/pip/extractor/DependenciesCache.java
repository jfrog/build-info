package org.jfrog.build.extractor.pip.extractor;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jfrog.build.extractor.ci.Dependency;
import org.jfrog.build.api.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.jfrog.build.extractor.BuildInfoExtractorUtils.createMapper;

/**
 * Created by Bar Belity on 19/07/2020.
 * <p>
 * Pip-install executions are responsible for downloading and installing the project's dependencies.
 * When pip identifies that a dependency is already installed, it doesn't reinstall it.
 * Since we are relying on pip's logs for identifying the project's dependencies, in such case we wouldn't be able to
 * identify the dependency correctly.
 * This cache is saved between pip-install executions, and used to get the required information of previously downloaded
 * pip dependencies.
 */
public class DependenciesCache {

    private static final int CACHE_VERSION = 1;
    private static final ObjectMapper objectMapper = createMapper();

    @JsonProperty("version")
    private int version = CACHE_VERSION;
    @JsonProperty("dependencies")
    private Map<String, Dependency> dependencies;

    DependenciesCache() {
        this.dependencies = new HashMap<>();
    }

    /**
     * Reads the json cache file of previously created project-dependencies cache.
     * The cache consists of a map where:
     * Key: dependency-name, Value: build-info's Dependency object.
     * If cache-file doesn't exist, create a new file.
     *
     * @param executionPath -  Path of pip command's execution.
     * @param logger        - The logger.
     * @return The dependencies-cache.
     * @throws IOException
     */
    static DependenciesCache getProjectDependenciesCache(Path executionPath, Log logger) throws IOException {
        Path cachePath = getCacheFilePath(executionPath);
        File cacheFile = cachePath.toFile();
        if (!cacheFile.exists()) {
            // Create an empty cache file to allow future writing to it.
            Files.createDirectories(cacheFile.toPath().getParent());
            return null;
        }

        // Create DependenciesCache from cache-file content.
        DependenciesCache cache = new DependenciesCache();
        cache.read(cacheFile, logger);
        return cache;
    }

    static void updateDependenciesCache(Map<String, Dependency> updateMap, Path executionPath) throws IOException {
        DependenciesCache cache = new DependenciesCache();
        cache.dependencies = updateMap;
        Path cachePath = getCacheFilePath(executionPath);
        File cacheFile = cachePath.toFile();
        cache.write(cacheFile);
    }

    static Path getCacheFilePath(Path workDir) {
        // Cache file will be located in ./.jfrog/projects/deps.cache.json
        Path absoluteWorkDir = workDir.toAbsolutePath().normalize();
        return Paths.get(absoluteWorkDir.toString(), ".jfrog", "projects", "deps.cache.json");
    }

    void read(File file, Log logger) throws IOException {
        try {
            DependenciesCache dependenciesCache = objectMapper.readValue(file, DependenciesCache.class);
            if (dependenciesCache.getVersion() != CACHE_VERSION) {
                logger.warn("Incorrect cache version " + dependenciesCache.getVersion() + ". Zapping the old cache.");
                return;
            }
            this.dependencies = dependenciesCache.dependencies;
        } catch (JsonParseException | JsonMappingException e) {
            logger.error("Failed reading cache file, zapping the old cache.");
        }
    }

    void write(File file) throws IOException {
        objectMapper.writeValue(file, this);
    }

    Dependency getDependency(String dependencyName) {
        return dependencies.get(dependencyName);
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Map<String, Dependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Map<String, Dependency> dependencies) {
        this.dependencies = dependencies;
    }
}
