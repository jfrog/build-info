package org.jfrog.build.extractor.pip.extractor;

import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by Bar Belity on 09/07/2020.
 */
public class PipBuildInfoExtractor {

    private static final String PIP_AQL_FORMAT =
            "items.find({" +
                    "\"repo\": \"%s\"," +
                    "\"$or\": [{" +
                    "\"$and\": [{" +
                    "\"path\": {\"$match\": \"*\"}," +
                    "\"name\": {\"$match\": \"%s\"}" +
                    "}]" +
                    "}]" +
                    "}).include(\"name\", \"repo\", \"path\", \"actual_sha1\", \"actual_md5\")";

    Build extract(ArtifactoryDependenciesClient client, String repository, String installationLog, Path executionPath, String module, Log logger) throws IOException {
        // Parse logs and create dependency list of <pkg-name, pkg-file>
        Map<String, String> downloadedDependencies = PipLogParser.parse(installationLog, logger);

        // Create package-name to dependency map.
        Map<String, Dependency> dependenciesMap = buildDependenciesMap(downloadedDependencies, client, repository, executionPath, logger);

        // Create Build.
        List<Dependency> dependenciesList = new ArrayList<>(dependenciesMap.values());
        return createBuild(dependenciesList, module);
    }

    Map<String, Dependency> buildDependenciesMap(Map<String, String> downloadedDependencies, ArtifactoryDependenciesClient client, String repository, Path executionPath, Log logger) throws IOException {
        Map<String, Dependency> dependenciesMap = new HashMap<>();
        Set<String> missingDeps = new HashSet<>();
        DependenciesCache dependenciesCache = DependenciesCache.getProjectDependenciesCache(executionPath, logger);

        for (String pkgName : downloadedDependencies.keySet()) {
            String fileName =  downloadedDependencies.get(pkgName);
            Dependency dependency = null;
            if (StringUtils.isNotBlank(fileName)) {
                // Get dependency info from Artifactory - may throw IOException.
                dependency = getPackageInfoFromArtifactory(fileName, repository, client, logger);
            } else {
                // Get dependency info from cache.
                if (dependenciesCache != null) {
                    dependency = dependenciesCache.getDependency(pkgName);
                }
            }

            if (dependency == null) {
                // Add missing dependency.
                missingDeps.add(pkgName);
                continue;
            }

            dependenciesMap.put(pkgName, dependency);
        }

        promptMissingDeps(missingDeps, logger);
        DependenciesCache.updateDependenciesCache(dependenciesMap, executionPath);
        return dependenciesMap;
    }

    Dependency getPackageInfoFromArtifactory(String fileName, String repository, ArtifactoryDependenciesClient client, Log logger) throws IOException {
        String aql = String.format(PIP_AQL_FORMAT, repository, fileName);
        AqlSearchResult searchResult;

        try {
            searchResult = client.searchArtifactsByAql(aql);
        } catch (IOException e) {
            throw new IOException("Failed fetching checksums of file: '" + fileName + "' from Artifactory ", e);
        }

        if (searchResult.getResults().isEmpty()) {
            logger.debug("File: " + fileName + " could not be found in repository: " + repository);
            return null;
        }

        AqlSearchResult.SearchEntry searchEntry = searchResult.getResults().get(0);
        if (StringUtils.isBlank(searchEntry.getActualMd5()) || StringUtils.isBlank(searchEntry.getActualSha1())) {
            logger.debug("Missing checksums for file: " + fileName + ", sha1: '" + searchEntry.getActualSha1() + "' md5: '" + searchEntry.getActualMd5() + "'");
            return null;
        }

        return new DependencyBuilder().id(fileName).md5(searchEntry.getActualMd5()).sha1(searchEntry.getActualSha1()).build();
    }

    private void promptMissingDeps(Set<String> missingDeps, Log logger) {
        if (!missingDeps.isEmpty()) {
            logger.info(Arrays.toString(missingDeps.toArray()));
            logger.info("The pypi packages above could not be found in Artifactory or were not downloaded in this execution, therefore they are not included in the build-info.\n" +
                    "Reinstalling in clean environment or using '--no-cache-dir' and '--force-reinstall' flags (in one execution only), will force downloading and populating Artifactory with these packages, and therefore resolve the issue.");
        }
    }

    private Build createBuild(List<Dependency> dependenciesList, String moduleName) {
        Module module = new ModuleBuilder().id(moduleName).dependencies(dependenciesList).build();
        List<Module> modules = new ArrayList<>();
        modules.add(module);
        Build build= new Build();
        build.setModules(modules);
        return build;
    }
}
