/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jfrog.build.extractor.gradle;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import org.apache.commons.lang.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.extractor.BuildInfoExtractor;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Iterables.*;
import static com.google.common.collect.Lists.newArrayList;

/**
 * An upload task uploads files to the repositories assigned to it.  The files that get uploaded are the artifacts of
 * your project, if they belong to the configuration associated with the upload task.
 *
 * @author Tomer Cohen
 */
public class BuildInfoRecorder implements BuildInfoExtractor<Project, Module> {
    private static final Logger log = Logging.getLogger(BuildInfoRecorder.class);
    private Project project;

    private Configuration configuration;
    private static final String SHA1 = "sha1";
    private static final String MD5 = "md5";

    public BuildInfoRecorder(Configuration configuration) {
        this.configuration = configuration;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public Module extract(Project project) {
        this.project = project;
        ModuleBuilder builder = new ModuleBuilder()
                .id(project.getGroup() + ":" + project.getName() + ":" + project.getVersion().toString());
        if (getConfiguration() != null) {
            try {
                builder.artifacts(calculateArtifacts(project)).dependencies(calculateDependencies());
            } catch (Exception e) {
                log.error("Error during extraction: ", e);
            }
        }
        return builder.build();
    }

    private List<Artifact> calculateArtifacts(Project project) throws Exception {
        List<Artifact> artifacts = newArrayList(
                transform(getConfiguration().getAllArtifacts(), new Function<PublishArtifact, Artifact>() {
                    public Artifact apply(PublishArtifact from) {
                        try {
                            String type = from.getType();
                            if (StringUtils.isNotBlank(from.getClassifier())) {
                                type = type + "-" + from.getClassifier();
                            }
                            Map<String, String> checkSums = calculateChecksumsForFile(from.getFile());
                            return new ArtifactBuilder(from.getName()).type(type)
                                    .md5(checkSums.get(MD5)).sha1(checkSums.get(SHA1)).build();
                        } catch (Exception e) {
                            log.error("Error during artifact calculation: ", e);
                        }
                        return new Artifact();
                    }
                }));

        File mavenPom = new File(project.getRepositories().getMavenPomDir(), "pom-default.xml");
        if (mavenPom.exists()) {
            Map<String, String> checksums = calculateChecksumsForFile(mavenPom);
            Artifact pom =
                    new ArtifactBuilder(project.getName()).md5(checksums.get(MD5)).sha1(checksums.get(SHA1)).type("pom")
                            .build();
            artifacts.add(pom);
        }
        File ivy = new File(project.getBuildDir(), "ivy.xml");
        if (ivy.exists()) {
            Map<String, String> checksums = calculateChecksumsForFile(ivy);
            Artifact ivyArtifact =
                    new ArtifactBuilder(project.getName()).md5(checksums.get(MD5)).sha1(checksums.get(SHA1)).type("ivy")
                            .build();
            artifacts.add(ivyArtifact);
        }
        return artifacts;
    }

    private List<Dependency> calculateDependencies() throws Exception {
        Set<Configuration> configurationSet = project.getConfigurations().getAll();
        List<Dependency> dependencies = newArrayList();
        for (Configuration configuration : configurationSet) {
            ResolvedConfiguration resolvedConfiguration = configuration.getResolvedConfiguration();
            Set<ResolvedArtifact> resolvedArtifactSet = resolvedConfiguration.getResolvedArtifacts();
            for (final ResolvedArtifact artifact : resolvedArtifactSet) {
                ResolvedDependency resolvedDependency = artifact.getResolvedDependency();
                String depId = resolvedDependency.getName();
                if (depId.startsWith(":")) {
                    depId = resolvedDependency.getModuleGroup() + depId + ":" + resolvedDependency.getModuleVersion();
                }
                final String finalDepId = depId;
                Predicate<Dependency> idEqualsPredicate = new Predicate<Dependency>() {
                    public boolean apply(@Nullable Dependency input) {
                        return input.getId().equals(finalDepId);
                    }
                };
                //maybe we have it already?
                if (any(dependencies, idEqualsPredicate)) {
                    Dependency existingDependency = find(dependencies, idEqualsPredicate);
                    List<String> existingScopes = existingDependency.getScopes();
                    String configScope = configuration.getName();
                    if (!existingScopes.contains(configScope)) {
                        existingScopes.add(configScope);
                    }
                } else {
                    DependencyBuilder dependencyBuilder = new DependencyBuilder();
                    Map<String, String> checksums = calculateChecksumsForFile(artifact.getFile());
                    dependencyBuilder.type(artifact.getType()).id(depId)
                            .scopes(newArrayList(configuration.getName())).
                            md5(checksums.get(MD5)).sha1(checksums.get(SHA1));
                    dependencies.add(dependencyBuilder.build());
                }
            }
        }
        return dependencies;
    }

    private Map<String, String> calculateChecksumsForFile(File file)
            throws NoSuchAlgorithmException, IOException {
        Map<String, String> checkSums =
                FileChecksumCalculator.calculateChecksums(file, MD5, SHA1);
        return checkSums;
    }
}
