/*
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.build.extractor.clientConfiguration.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.builder.dependency.BuildDependencyBuilder;
import org.jfrog.build.api.builder.dependency.BuildPatternArtifactsRequestBuilder;
import org.jfrog.build.api.dependency.*;
import org.jfrog.build.api.dependency.pattern.BuildDependencyPattern;
import org.jfrog.build.api.dependency.pattern.DependencyPattern;
import org.jfrog.build.api.util.Log;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Helper class for parsing build dependencies property.
 * Used only for legacy patterns in Jenkins, TeamCity and Bamboo.
 * @author Evgeny Goldin
 */
public class BuildDependenciesHelper {

    private DependenciesDownloader downloader;
    private Log log;

    public BuildDependenciesHelper(DependenciesDownloader downloader, Log log) {
        this.downloader = downloader;
        this.log = log;
    }

    public List<BuildDependency> retrieveBuildDependencies(String resolvePattern) throws IOException, InterruptedException {
        if (StringUtils.isBlank(resolvePattern)) {
            return Collections.emptyList();
        }

        List<String> patternLines = PublishedItemsHelper.parsePatternsFromProperty(resolvePattern);

        // Don't run if dependencies mapping came out to be empty.
        if (patternLines.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Beginning to resolve Build Info build dependencies.");
        Map<String, Map<String, List<BuildDependencyPattern>>> buildDependencies = getBuildDependencies(patternLines);
        List<BuildPatternArtifactsRequest> artifactsRequests = toArtifactsRequests(buildDependencies);
        List<BuildPatternArtifacts> artifactsResponses = downloader.getClient().retrievePatternArtifacts(
                artifactsRequests);
        Set<BuildDependency> result = Sets.newHashSet();
        downloader.download(collectArtifactsToDownload(buildDependencies, artifactsRequests, artifactsResponses, result));
        log.info("Finished resolving Build Info build dependencies.");

        return Lists.newArrayList(result);
    }

    private Map<String, Map<String, List<BuildDependencyPattern>>> getBuildDependencies(List<String> patternLines) {
        Map<String, Map<String, List<BuildDependencyPattern>>> buildsMap = Maps.newHashMap();
        for (String patternLine : patternLines) {
            DependencyPattern dependencyPattern = PatternFactory.create(patternLine);
            if (dependencyPattern instanceof BuildDependencyPattern) {
                BuildDependencyPattern buildDependencyPattern = (BuildDependencyPattern) dependencyPattern;
                String buildName = buildDependencyPattern.getBuildName();
                Map<String, List<BuildDependencyPattern>> numbersMap = buildsMap.get(buildName);
                if (numbersMap == null) {
                    buildsMap.put(buildName, Maps.<String, List<BuildDependencyPattern>>newHashMap());
                    numbersMap = buildsMap.get(buildName);
                }

                String buildNumber = buildDependencyPattern.getBuildNumber();
                List<BuildDependencyPattern> dependencyPatternList = numbersMap.get(buildNumber);
                if (dependencyPatternList == null) {
                    numbersMap.put(buildNumber, Lists.<BuildDependencyPattern>newLinkedList());
                    dependencyPatternList = numbersMap.get(buildNumber);
                }
                dependencyPatternList.add(buildDependencyPattern);
            }
        }


        return buildsMap;
    }

    private List<BuildPatternArtifactsRequest> toArtifactsRequests(
            Map<String, Map<String, List<BuildDependencyPattern>>> dependencyPatterns) {
        List<BuildPatternArtifactsRequest> artifactsRequests = Lists.newLinkedList();
        for (String buildName : dependencyPatterns.keySet()) {
            Map<String, List<BuildDependencyPattern>> buildNumbers = dependencyPatterns.get(buildName);
            for (String buildNumber : buildNumbers.keySet()) {
                List<BuildDependencyPattern> buildDependencyPatterns = buildNumbers.get(buildNumber);
                BuildPatternArtifactsRequestBuilder builder = new BuildPatternArtifactsRequestBuilder()
                        .buildName(buildName).buildNumber(buildNumber);
                for (BuildDependencyPattern buildDependencyPattern : buildDependencyPatterns) {
                    builder.pattern(buildDependencyPattern.getPattern());
                }
                artifactsRequests.add(builder.build());
            }
        }

        return artifactsRequests;
    }

    private Set<DownloadableArtifact> collectArtifactsToDownload(
            Map<String, Map<String, List<BuildDependencyPattern>>> dependencyPatterns,
            List<BuildPatternArtifactsRequest> artifactsRequests, List<BuildPatternArtifacts> artifactsResponses,
            Set<BuildDependency> buildDependencies) {
        Set<DownloadableArtifact> downloadableArtifacts = Sets.newHashSet();
        verifySameSize(artifactsRequests, artifactsResponses);

        for (int i = 0; i < artifactsRequests.size(); i++) {
            BuildPatternArtifacts artifacts = artifactsResponses.get(i);
            if (artifacts == null) {
                // Pattern didn't match any results: wrong build name or build number.
                continue;
            }


            List<BuildDependencyPattern> buildDependencyPatterns = dependencyPatterns.get(artifacts.getBuildName()).get(
                    artifactsRequests.get(i).getBuildNumber());
            for (int j = 0; j < buildDependencyPatterns.size(); j++) {
                BuildDependencyPattern buildDependencyPattern = buildDependencyPatterns.get(j);
                if (!buildDependencyPattern.getBuildName().equals(artifacts.getBuildName())) {
                    throw new IllegalArgumentException(String.format("Build names don't match: [%s] != [%s]",
                            buildDependencyPattern.getBuildName(), artifacts.getBuildName()));
                }

                final String message = String.format("Dependency on build [%s], number [%s]",
                        buildDependencyPattern.getBuildName(), buildDependencyPattern.getBuildNumber());

                /**
                 * Build number response is null for unresolved dependencies (wrong build name or build number).
                 */
                if (artifacts.getBuildNumber() == null) {
                    log.info(
                            message + " - no results found, check correctness of dependency build name and build number.");
                } else {
                    PatternResult patternResult = artifacts.getPatternResults().get(j);
                    List<PatternArtifact> patternArtifacts = patternResult.getPatternArtifacts();
                    log.info(message + String.format(", pattern [%s] - [%s] result%s found.",
                            buildDependencyPattern.getPattern(), patternArtifacts.size(),
                            (patternArtifacts.size() == 1 ? "" : "s")));

                    for (PatternArtifact patternArtifact : patternArtifacts) {
                        final String uri = patternArtifact.getUri(); // "libs-release-local/com/goldin/plugins/gradle/0.1.1/gradle-0.1.1.jar"
                        final int indexOfFirstSlash = uri.indexOf('/');

                        assert (indexOfFirstSlash > 0) : String.format("Failed to locate '/' in [%s]", uri);

                        final String repoUrl = patternArtifact.getArtifactoryUrl() + '/' + uri.substring(0,
                                indexOfFirstSlash);
                        final String filePath = uri.substring(indexOfFirstSlash + 1);
                        downloadableArtifacts.add(
                                new DownloadableArtifact(repoUrl, buildDependencyPattern.getTargetDirectory(), filePath,
                                        buildDependencyPattern.getMatrixParams(), buildDependencyPattern.getPattern(),
                                        buildDependencyPattern.getPatternType()));
                    }

                    if (!patternArtifacts.isEmpty()) {
                        BuildDependency buildDependency = new BuildDependencyBuilder()
                                .name(artifacts.getBuildName())
                                .number(artifacts.getBuildNumber())
                                .url(artifacts.getUrl())
                                .started(artifacts.getStarted())
                                .build();
                        buildDependencies.add(buildDependency);
                    }
                }
            }
        }

        return downloadableArtifacts;
    }

    /**
     * Verifies both lists are of the same size.
     *
     * @param l1 first list to check
     * @param l2 second list to check
     * @throws IllegalArgumentException if lists are of different sizes.
     */
    private void verifySameSize(List l1, List l2) {
        if (l1.size() != l2.size()) {
            throw new IllegalArgumentException(
                    String.format("List sizes don't match: [%s] != [%s]", l1.size(), l2.size()));
        }
    }
}