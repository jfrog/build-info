package org.jfrog.build.extractor.pip.extractor;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Bar Belity on 19/07/2020.
 */
public class DependenciesCache {

    private static final int CACHE_VERSION = 1;
    private static ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    @JsonProperty("version")
    private int version = CACHE_VERSION;
    @JsonProperty("dependencies")
    private Map<String, Dependency> dependencies;

    DependenciesCache() {
        this.dependencies = new HashMap<>();
    }

    static DependenciesCache getProjectDependenciesCache(Path executionPath, Log logger) throws IOException {
        // Get cache file-path.
        Path cachePath = getCacheFilePath(executionPath);

        // If file not exists -> create and return null.
        File cacheFile = cachePath.toFile();
        if (!cacheFile.exists()) {
            Files.createDirectories(cacheFile.toPath().getParent());
            return null;
        }

        // Read file to object.
        DependenciesCache cache = new DependenciesCache();
        cache.read(cacheFile, logger);
        return cache;
    }

    static void updateDependenciesCache(Map<String, Dependency> updateMap, Path executionPath) throws IOException {
        DependenciesCache cache = new DependenciesCache();
        cache.dependencies = updateMap;
        // Get cache file-path.
        Path cachePath = getCacheFilePath(executionPath);
        // Replace file with json content.
        File cacheFile = cachePath.toFile();
        cache.write(cacheFile);
    }

    // Cache file will be located in the ./.jfrog/projects/deps.cache.json
    static Path getCacheFilePath(Path workDir) {
        // Get workdir.
        Path absoluteWorkDir = workDir.toAbsolutePath().normalize();
        // Return path of workdir/.jfrog/projects/deps.cache.json.
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
