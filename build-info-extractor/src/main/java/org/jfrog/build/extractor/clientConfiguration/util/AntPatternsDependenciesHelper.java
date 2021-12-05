package org.jfrog.build.extractor.clientConfiguration.util;

import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.DownloadableArtifact;
import org.jfrog.build.api.dependency.PatternResultFileSet;
import org.jfrog.build.api.dependency.PropertySearchResult;
import org.jfrog.build.api.dependency.pattern.BuildDependencyPattern;
import org.jfrog.build.api.dependency.pattern.DependencyPattern;
import org.jfrog.build.api.util.CommonUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.PatternMatcher;

import java.io.IOException;
import java.util.*;

/**
 * Helper class for parsing custom resolved dependencies
 *
 * @author Shay Yaakov
 */
public class AntPatternsDependenciesHelper {

    private DependenciesDownloader downloader;
    private Log log;

    public AntPatternsDependenciesHelper(DependenciesDownloader downloader, Log log) {
        this.downloader = downloader;
        this.log = log;
    }

    public List<Dependency> retrievePublishedDependencies(String resolvePattern) throws IOException, InterruptedException {
        if (StringUtils.isBlank(resolvePattern)) {
            return Collections.emptyList();
        }

        List<String> patternLines = PublishedItemsHelper.parsePatternsFromProperty(resolvePattern);
        List<Dependency> dependencies = Collections.emptyList();

        // Don't run if dependencies mapping came out to be empty.
        if (patternLines.isEmpty()) {
            return dependencies;
        }

        log.info("Beginning to resolve Build Info dependencies.");
        dependencies = downloader.download(collectArtifactsToDownload(patternLines));
        log.info("Finished resolving Build Info dependencies.");
        return dependencies;
    }

    private Set<DownloadableArtifact> collectArtifactsToDownload(List<String> patternLines)
            throws IOException {
        Set<DownloadableArtifact> downloadableArtifacts = new HashSet<>();
        for (String patternLine : patternLines) {
            DependencyPattern dependencyPattern = PatternFactory.create(patternLine);
            if (!(dependencyPattern instanceof BuildDependencyPattern)) {
                downloadableArtifacts.addAll(handleDependencyPattern(dependencyPattern));
            }
        }

        return downloadableArtifacts;
    }

    private Set<DownloadableArtifact> handleDependencyPattern(DependencyPattern dependencyPattern) throws IOException {
        String pattern = dependencyPattern.getPattern();
        log.info("Resolving published dependencies with pattern " + pattern);
        if (StringUtils.contains(pattern, "**")) {
            if (StringUtils.isNotBlank(dependencyPattern.getMatrixParams())) {
                return performPropertySearch(dependencyPattern);
            } else {
                throw new IllegalArgumentException(
                        "Wildcard '**' is not allowed without matrix params for pattern '" + pattern + "'");
            }
        } else {
            return performPatternSearch(dependencyPattern);
        }
    }

    private Set<DownloadableArtifact> performPropertySearch(DependencyPattern dependencyPattern) throws IOException {
        Set<DownloadableArtifact> downloadableArtifacts = new HashSet<>();
        String pattern = dependencyPattern.getPattern();
        String matrixParams = dependencyPattern.getMatrixParams();
        PropertySearchResult propertySearchResult = downloader.getArtifactoryManager().searchArtifactsByProperties(matrixParams);
        List<PropertySearchResult.SearchEntry> filteredEntries = filterResultEntries(
                propertySearchResult.getResults(), pattern);
        log.info("Found " + filteredEntries.size() + " dependencies by doing a property search.");
        for (PropertySearchResult.SearchEntry searchEntry : filteredEntries) {
            downloadableArtifacts.add(
                    new DownloadableArtifact(searchEntry.getRepoUri(), dependencyPattern.getTargetDirectory(),
                            searchEntry.getFilePath(), matrixParams, pattern, dependencyPattern.getPatternType()));
        }

        return downloadableArtifacts;
    }

    private List<PropertySearchResult.SearchEntry> filterResultEntries(List<PropertySearchResult.SearchEntry> results,
                                                                       String pattern) {
        final String patternStr = pattern.replaceFirst(":", "/");
        return new ArrayList<>(CommonUtils.filterCollection(results, result -> PatternMatcher.match(patternStr, result.getRepoPath(), false)));
    }

    private Set<DownloadableArtifact> performPatternSearch(DependencyPattern dependencyPattern) throws IOException {
        Set<DownloadableArtifact> downloadableArtifacts = new HashSet<>();
        String pattern = dependencyPattern.getPattern();
        PatternResultFileSet fileSet = downloader.getArtifactoryManager().searchArtifactsByPattern(pattern);
        Set<String> filesToDownload = fileSet.getFiles();
        log.info("Found " + filesToDownload.size() + " dependencies by doing a pattern search.");
        for (String fileToDownload : filesToDownload) {
            downloadableArtifacts.add(
                    new DownloadableArtifact(fileSet.getRepoUri(), dependencyPattern.getTargetDirectory(),
                            fileToDownload, dependencyPattern.getMatrixParams(), pattern,
                            dependencyPattern.getPatternType()));
        }

        return downloadableArtifacts;
    }
}
