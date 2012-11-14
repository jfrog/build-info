package org.jfrog.build.util;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.DownloadableArtifact;
import org.jfrog.build.api.dependency.PatternResultFileSet;
import org.jfrog.build.api.dependency.PropertySearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.PatternMatcher;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper class for parsing custom resolved dependencies
 *
 * @author Shay Yaakov
 */
public class DependenciesHelper {

    private DependenciesDownloader downloader;
    private Log log;

    public DependenciesHelper(DependenciesDownloader downloader, Log log) {
        this.downloader = downloader;
        this.log = log;
    }

    public List<Dependency> retrievePublishedDependencies(String resolvePattern)
            throws IOException, InterruptedException {
        Multimap<String, String> patternPairs = PublishedItemsHelper.getPublishedItemsPatternPairs(resolvePattern);
        List<Dependency> dependencies = Collections.emptyList();

        // Don't run if dependencies mapping came out to be empty.
        if (patternPairs.isEmpty()) {
            return dependencies;
        }

        log.info("Beginning to resolve Build Info published dependencies.");
        dependencies = downloader.download(collectArtifactsToDownload(patternPairs));
        log.info("Finished resolving Build Info published dependencies.");

        return dependencies;
    }

    private Set<DownloadableArtifact> collectArtifactsToDownload(Multimap<String, String> patternPairs)
            throws IOException, InterruptedException {
        Set<DownloadableArtifact> downloadableArtifacts = Sets.newHashSet();
        for (Map.Entry<String, String> patternPair : patternPairs.entries()) {
            if (!patternPair.getKey().contains("@")) {
                downloadableArtifacts.addAll(handleDependencyPatternPair(patternPair));
            }
        }

        return downloadableArtifacts;
    }

    private Set<DownloadableArtifact> handleDependencyPatternPair(Map.Entry<String, String> patternPair)
            throws IOException {
        String sourcePattern = patternPair.getKey();
        String pattern = extractPatternFromSource(sourcePattern);
        String matrixParams = extractMatrixParamsFromSource(sourcePattern);

        log.info("Resolving published dependencies with pattern " + sourcePattern);
        String relativeDirPath = patternPair.getValue();
        if (StringUtils.contains(pattern, "**")) {
            if (StringUtils.isNotBlank(matrixParams)) {
                return performPropertySearch(pattern, relativeDirPath, matrixParams);
            } else {
                throw new IllegalArgumentException(
                        "Wildcard '**' is not allowed without matrix params for pattern '" + pattern + "'");
            }
        } else {
            return performPatternSearch(pattern, relativeDirPath, matrixParams);
        }
    }

    private Set<DownloadableArtifact> performPropertySearch(String pattern, String relativeDirPath, String matrixParams)
            throws IOException {
        Set<DownloadableArtifact> downloadableArtifacts = Sets.newHashSet();
        PropertySearchResult propertySearchResult = downloader.getClient().searchArtifactsByProperties(matrixParams);
        List<PropertySearchResult.SearchEntry> filteredEntries = filterResultEntries(
                propertySearchResult.getResults(), pattern);
        log.info("Found " + filteredEntries.size() + " dependencies.");
        for (PropertySearchResult.SearchEntry searchEntry : filteredEntries) {
            downloadableArtifacts.add(
                    new DownloadableArtifact(searchEntry.getRepoUri(), relativeDirPath,
                            searchEntry.getFilePath(), matrixParams));
        }

        return downloadableArtifacts;
    }

    private List<PropertySearchResult.SearchEntry> filterResultEntries(List<PropertySearchResult.SearchEntry> results,
            String pattern) {
        final String patternStr = pattern.replaceFirst(":", "/");
        return Lists.newArrayList(Iterables.filter(results, new Predicate<PropertySearchResult.SearchEntry>() {
            @Override
            public boolean apply(PropertySearchResult.SearchEntry input) {
                return PatternMatcher.match(patternStr, input.getRepoPath(), false);
            }
        }));
    }

    private Set<DownloadableArtifact> performPatternSearch(String pattern, String relativeDirPath, String matrixParams)
            throws IOException {
        Set<DownloadableArtifact> downloadableArtifacts = Sets.newHashSet();
        PatternResultFileSet fileSet = downloader.getClient().searchArtifactsByPattern(pattern);
        Set<String> filesToDownload = fileSet.getFiles();
        log.info("Found " + filesToDownload.size() + " dependencies.");
        for (String fileToDownload : filesToDownload) {
            downloadableArtifacts.add(
                    new DownloadableArtifact(fileSet.getRepoUri(), relativeDirPath, fileToDownload, matrixParams));
        }

        return downloadableArtifacts;
    }

    private String extractPatternFromSource(String sourcePattern) {
        int indexOfSemiColon = sourcePattern.indexOf(';');
        if (indexOfSemiColon == -1) {
            return sourcePattern;
        }

        return sourcePattern.substring(0, indexOfSemiColon);
    }

    private String extractMatrixParamsFromSource(String sourcePattern) throws UnsupportedEncodingException {
        StringBuilder matrixParamBuilder = new StringBuilder();

        //Split pattern to fragments in case there are any matrix params
        String[] patternFragments = StringUtils.split(sourcePattern, ';');

        //Iterate and add matrix params if there are any
        if (patternFragments.length > 1) {
            for (int i = 1; i < patternFragments.length; i++) {
                String matrixParam = patternFragments[i];
                String[] matrixParamFragments = StringUtils.split(matrixParam, '=');

                if (matrixParamFragments.length == 0) {
                    continue;
                }
                //If the key is mandatory, separate the + before encoding
                String key = matrixParamFragments[0];
                boolean mandatory = false;
                if (key.endsWith("+")) {
                    mandatory = true;
                    key = key.substring(0, key.length() - 1);
                }
                if (i > 1) {
                    matrixParamBuilder.append(";");
                }
                matrixParamBuilder.append(URLEncoder.encode(key, "utf-8"));
                if (mandatory) {
                    matrixParamBuilder.append("+");
                }
                if (matrixParamFragments.length > 1) {
                    matrixParamBuilder.append("=").append(URLEncoder.encode(matrixParamFragments[1], "utf-8"));
                }
            }
        }

        return matrixParamBuilder.toString();
    }
}
