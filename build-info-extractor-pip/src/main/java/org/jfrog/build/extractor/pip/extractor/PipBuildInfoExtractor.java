package org.jfrog.build.extractor.pip.extractor;

import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.builder.ModuleType;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.builder.DependencyBuilder;
import org.jfrog.build.extractor.builder.ModuleBuilder;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.extractor.ci.Dependency;
import org.jfrog.build.extractor.ci.Module;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Bar Belity on 09/07/2020.
 */
public class PipBuildInfoExtractor {

    private static final String PIP_AQL_FORMAT =
            "items.find({" +
                    "\"repo\":\"%s\"," +
                    "\"$or\":[%s]" +
                    "}).include(\"name\",\"repo\",\"path\",\"actual_sha1\",\"actual_md5\")";
    private static final String PIP_AQL_FILE_PART = "{\"name\":\"%s\"},";
    private static final int PIP_AQL_BULK_SIZE = 3;

    BuildInfo extract(ArtifactoryManager artifactoryManager, String repository, String installationLog, Path executionPath, String module, Log logger) throws IOException {
        // Parse logs and create dependency list of <pkg-name, pkg-file>
        Map<String, String> downloadedDependencies = PipLogParser.parse(installationLog, logger);

        // Create package-name to dependency map.
        Map<String, Dependency> dependenciesMap = buildDependenciesMap(downloadedDependencies, artifactoryManager, repository, executionPath, logger);

        // Create BuildInfo.
        List<Dependency> dependenciesList = new ArrayList<>(dependenciesMap.values());
        return createBuild(dependenciesList, module);
    }

    /**
     * Create a mapping of this build's package-name and its Dependency object.
     * Creation is based on the dependencies downloaded in this pip-install execution, and the cache saved in previous builds.
     *
     * @param downloadedDependencies - The dependencies of this pip-execution, package-name to downloaded package-file map.
     * @param artifactoryManager     - Artifactory manager for fetching artifacts data.
     * @param repository             - Resolution repository.
     * @param executionPath          - Path of pip command's execution.
     * @param logger                 - The logger.
     * @return Mapping of a package-name and its Dependency object.
     */
    Map<String, Dependency> buildDependenciesMap(Map<String, String> downloadedDependencies, ArtifactoryManager artifactoryManager, String repository, Path executionPath, Log logger) throws IOException {
        Map<String, Dependency> dependenciesMap = new HashMap<>();
        DependenciesCache dependenciesCache = DependenciesCache.getProjectDependenciesCache(executionPath, logger);

        // Mapping an actual downloaded file -> package name, contains all files to get its checksums from Artifactory.
        Map<String, String> getFromArtifactoryMap = new HashMap<>();
        for (String pkgName : downloadedDependencies.keySet()) {
            String fileName = downloadedDependencies.get(pkgName);
            if (StringUtils.isNotBlank(fileName)) {
                // Dependency downloaded from Artifactory.
                getFromArtifactoryMap.put(fileName, pkgName);
                continue;
            }
            // Dependency wasn't downloaded in this execution, get dependency info from cache.
            if (dependenciesCache != null && dependenciesCache.getDependency(pkgName) != null) {
                dependenciesMap.put(pkgName, dependenciesCache.getDependency(pkgName));
            }
        }

        // Get dependencies from Artifactory.
        dependenciesMap.putAll(getDependenciesFromArtifactory(getFromArtifactoryMap, repository, artifactoryManager, logger));

        // Prompt missing dependencies.
        Set<String> missingDeps = downloadedDependencies.keySet().stream()
                .filter(x -> !dependenciesMap.containsKey(x))
                .collect(Collectors.toSet());
        promptMissingDeps(missingDeps, logger);

        DependenciesCache.updateDependenciesCache(dependenciesMap, executionPath);
        return dependenciesMap;
    }

    /**
     * Create Dependency objects for the files in 'fileToPackageMap'.
     * Get Dependencies information from Artifactory by running AQL to get the checksums.
     *
     * @param fileToPackageMap - Mapping between a downloaded file to its package name.
     * @param repository       - Resolution repository.
     * @param artifactoryManager           - Artifactory manager for fetching artifacts data.
     * @param logger           - The logger.
     * @return Mapping of a package-name and its Dependency object.
     */
    private Map<String, Dependency> getDependenciesFromArtifactory(Map<String, String> fileToPackageMap, String repository,
                                                                   ArtifactoryManager artifactoryManager, Log logger) throws IOException {
        if (fileToPackageMap.isEmpty()) {
            return Collections.emptyMap();
        }
        AqlSearchResult searchResult = runAqlQueries(createAqlQueries(fileToPackageMap, repository, PIP_AQL_BULK_SIZE), artifactoryManager);
        return createDependenciesFromAqlResult(searchResult, fileToPackageMap, logger);
    }

    static List<String> createAqlQueries(Map<String, String> fileToPackageMap, String repository, int bulkSize) {
        List<String> aqlQueries = new ArrayList<>();
        StringBuilder filesQueryPartBuilder = new StringBuilder();
        int filesCounter = 0;
        for (String file : fileToPackageMap.keySet()) {
            filesCounter++;
            filesQueryPartBuilder.append(String.format(PIP_AQL_FILE_PART, file));
            if (filesCounter == bulkSize) {
                aqlQueries.add(getPipDependenciesAql(filesQueryPartBuilder, repository));
                filesCounter = 0;
                filesQueryPartBuilder.setLength(0);
            }
        }
        if (filesQueryPartBuilder.length() > 0) {
            aqlQueries.add(getPipDependenciesAql(filesQueryPartBuilder, repository));
        }
        return aqlQueries;
    }

    static String getPipDependenciesAql(StringBuilder filesQueryPartBuilder, String repository) {
        filesQueryPartBuilder.setLength(filesQueryPartBuilder.length() - 1);
        return String.format(PIP_AQL_FORMAT, repository, filesQueryPartBuilder.toString());
    }

    private AqlSearchResult runAqlQueries(List<String> aqlQueries, ArtifactoryManager artifactoryManager) throws IOException {
        AqlSearchResult aggregatedResults = new AqlSearchResult();
        for (String aql : aqlQueries) {
            try {
                AqlSearchResult searchResult = artifactoryManager.searchArtifactsByAql(aql);
                if (!searchResult.getResults().isEmpty()) {
                    aggregatedResults.getResults().addAll(searchResult.getResults());
                }
            } catch (IOException e) {
                throw new IOException("Failed fetching dependencies checksums from Artifactory ", e);
            }
        }
        return aggregatedResults;
    }

    private Map<String, Dependency> createDependenciesFromAqlResult(AqlSearchResult searchResult, Map<String, String> fileToPackage, Log logger) {
        if (searchResult.getResults().isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Dependency> dependenciesMap = new HashMap<>();
        for (AqlSearchResult.SearchEntry searchEntry : searchResult.getResults()) {
            if (!isResultComplete(searchEntry)) {
                continue;
            }
            // Avoid adding duplicated dependencies.
            if (dependenciesMap.containsKey(fileToPackage.get(searchEntry.getName()))) {
                continue;
            }

            // Add a new dependency only if searchEntry represents a package downloaded in this build execution.
            if (fileToPackage.containsKey(searchEntry.getName())) {
                Dependency curDep = new DependencyBuilder()
                        .id(searchEntry.getName())
                        .md5(searchEntry.getActualMd5())
                        .sha1(searchEntry.getActualSha1())
                        .build();
                dependenciesMap.put(fileToPackage.get(searchEntry.getName()), curDep);
            }
        }

        Set<String> missingFiles = fileToPackage.keySet().stream()
                .filter(x -> !dependenciesMap.containsKey(fileToPackage.get(x)))
                .collect(Collectors.toSet());
        promptMissingChecksumFromArtifactory(missingFiles, logger);

        return dependenciesMap;
    }

    private boolean isResultComplete(AqlSearchResult.SearchEntry searchEntry) {
        return StringUtils.isNotBlank(searchEntry.getName()) && StringUtils.isNotBlank(searchEntry.getActualMd5())
                && StringUtils.isNotBlank(searchEntry.getActualSha1());
    }

    private void promptMissingDeps(Set<String> missingDeps, Log logger) {
        if (!missingDeps.isEmpty()) {
            logger.info(Arrays.toString(missingDeps.toArray()));
            logger.info("The pypi packages above could not be found in Artifactory or were not downloaded in this execution, therefore they are not included in the build-info.\n" +
                    "Reinstalling in clean environment or using '--no-cache-dir' and '--force-reinstall' flags (in one execution only), will force downloading and populating Artifactory with these packages, and therefore resolve the issue.");
        }
    }

    private void promptMissingChecksumFromArtifactory(Set<String> notFoundFiles, Log logger) {
        if (notFoundFiles.size() < 1) {
            return;
        }
        logger.debug(Arrays.toString(notFoundFiles.toArray()));
        logger.debug("Failed fetching checksums from Artifactory for the above files.");
    }

    private BuildInfo createBuild(List<Dependency> dependenciesList, String moduleName) {
        Module module = new ModuleBuilder().type(ModuleType.PYPI).id(moduleName).dependencies(dependenciesList).build();
        List<Module> modules = new ArrayList<>();
        modules.add(module);
        BuildInfo buildInfo = new BuildInfo();
        buildInfo.setModules(modules);
        return buildInfo;
    }
}
