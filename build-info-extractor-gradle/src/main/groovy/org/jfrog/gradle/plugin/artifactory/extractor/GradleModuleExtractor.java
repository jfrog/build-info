package org.jfrog.gradle.plugin.artifactory.extractor;

import org.apache.commons.compress.utils.Sets;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.jfrog.build.api.builder.ModuleType;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.extractor.ModuleExtractor;
import org.jfrog.build.extractor.builder.ArtifactBuilder;
import org.jfrog.build.extractor.builder.DependencyBuilder;
import org.jfrog.build.extractor.builder.ModuleBuilder;
import org.jfrog.build.extractor.ci.Artifact;
import org.jfrog.build.extractor.ci.Dependency;
import org.jfrog.build.extractor.ci.Module;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.IncludeExcludePatterns;
import org.jfrog.build.extractor.clientConfiguration.PatternMatcher;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPlugin;
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPluginUtil;
import org.jfrog.gradle.plugin.artifactory.extractor.listener.ArtifactoryDependencyResolutionListener;
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.jfrog.build.api.util.FileChecksumCalculator.*;
import static org.jfrog.build.extractor.BuildInfoExtractorUtils.getModuleIdString;
import static org.jfrog.build.extractor.BuildInfoExtractorUtils.getTypeString;

public class GradleModuleExtractor implements ModuleExtractor<Project> {
    private static final Logger log = Logging.getLogger(GradleModuleExtractor.class);

    @Override
    public Module extractModule(Project project) {
        Set<GradleDeployDetails> gradleDeployDetails = new HashSet<>();
        String artifactName = project.getName();
        ArtifactoryTask artifactoryTask = ProjectUtils.getBuildInfoTask(project);
        if (artifactoryTask != null) {
            artifactName = project.getName();
            try {
                artifactoryTask.collectDescriptorsAndArtifactsForUpload();
            } catch (IOException e) {
                throw new RuntimeException("Cannot collect deploy details for " + artifactoryTask.getPath(), e);
            }
            gradleDeployDetails = artifactoryTask.deployDetails;
        }
        String repo = gradleDeployDetails.stream()
                .map(GradleDeployDetails::getDeployDetails)
                .map(DeployDetails::getTargetRepository)
                .findAny()
                .orElse("");
        String moduleId = getModuleIdString(project.getGroup().toString(), artifactName, project.getVersion().toString());
        ModuleBuilder builder = new ModuleBuilder()
                .type(ModuleType.GRADLE)
                .id(moduleId)
                .repository(repo);
        try {
            // Extract the module's artifacts information if a publisher exists.
            ArtifactoryClientConfiguration.PublisherHandler publisher = ArtifactoryPluginUtil.getPublisherHandler(project);
            if (publisher != null) {
                boolean excludeArtifactsFromBuild = publisher.isFilterExcludedArtifactsFromBuild();
                IncludeExcludePatterns patterns = new IncludeExcludePatterns(
                        publisher.getIncludePatterns(),
                        publisher.getExcludePatterns());
                Iterable<GradleDeployDetails> deployExcludeDetails;
                Iterable<GradleDeployDetails> deployIncludeDetails;
                if (excludeArtifactsFromBuild) {
                    deployIncludeDetails = gradleDeployDetails.stream().filter(new IncludeExcludePredicate(project, patterns, true)).collect(Collectors.toSet());
                    deployExcludeDetails = gradleDeployDetails.stream().filter(new IncludeExcludePredicate(project, patterns, false)).collect(Collectors.toSet());
                } else {
                    deployIncludeDetails = gradleDeployDetails.stream().filter(new ProjectPredicate(project)).collect(Collectors.toSet());
                    deployExcludeDetails = new ArrayList<>();
                }
                builder.artifacts(calculateArtifacts(deployIncludeDetails))
                        .excludedArtifacts(calculateArtifacts(deployExcludeDetails));
            } else {
                log.warn("No publisher config found for project: " + project.getName());
            }
            builder.dependencies(calculateDependencies(project, moduleId));
        } catch (Exception e) {
            log.error("Error during extraction: ", e);
        }
        return builder.build();
    }

    private List<Artifact> calculateArtifacts(Iterable<GradleDeployDetails> deployDetails) {
        return StreamSupport.stream(deployDetails.spliterator(), false).map(from -> {
            PublishArtifactInfo publishArtifact = from.getPublishArtifact();
            DeployDetails deployDetails1 = from.getDeployDetails();
            String artifactPath = deployDetails1.getArtifactPath();
            int index = artifactPath.lastIndexOf('/');
            return new ArtifactBuilder(artifactPath.substring(index + 1))
                    .type(getTypeString(publishArtifact.getType(),
                            publishArtifact.getClassifier(), publishArtifact.getExtension()))
                    .md5(deployDetails1.getMd5())
                    .sha1(deployDetails1.getSha1())
                    .sha256(deployDetails1.getSha256())
                    .remotePath(artifactPath).build();
        }).collect(Collectors.toList());
    }

    private List<Dependency> calculateDependencies(Project project, String moduleId) throws Exception {
        ArtifactoryDependencyResolutionListener artifactoryDependencyResolutionListener =
                project.getRootProject().getPlugins().getPlugin(ArtifactoryPlugin.class).getArtifactoryDependencyResolutionListener();
        Map<String, String[][]> requestedByMap = artifactoryDependencyResolutionListener.getModulesHierarchyMap().get(moduleId);

        Set<Configuration> configurationSet = project.getConfigurations();
        List<Dependency> dependencies = new ArrayList<>();
        for (Configuration configuration : configurationSet) {
            if (configuration.getState() != Configuration.State.RESOLVED) {
                log.info("Artifacts for configuration '{}' were not all resolved, skipping", configuration.getName());
                continue;
            }
            ResolvedConfiguration resolvedConfiguration = configuration.getResolvedConfiguration();
            Set<ResolvedArtifact> resolvedArtifactSet = resolvedConfiguration.getResolvedArtifacts();
            for (final ResolvedArtifact artifact : resolvedArtifactSet) {
                File file = artifact.getFile();
                if (file.exists()) {
                    ModuleVersionIdentifier id = artifact.getModuleVersion().getId();
                    final String depId = getModuleIdString(id.getGroup(),
                            id.getName(), id.getVersion());
                    // if it's already in the dependencies list just add the current scope
                    Dependency existingDependency = dependencies.stream()
                            .filter(input -> input.getId().equals(depId)).findAny().orElse(null);
                    if (existingDependency != null) {
                        Set<String> existingScopes = existingDependency.getScopes();
                        existingScopes.add(configuration.getName());
                        existingDependency.setScopes(existingScopes);
                    } else {
                        DependencyBuilder dependencyBuilder = new DependencyBuilder()
                                .type(getTypeString(artifact.getType(),
                                        artifact.getClassifier(), artifact.getExtension()))
                                .id(depId)
                                .scopes(Sets.newHashSet(configuration.getName()));
                        if (requestedByMap != null) {
                            dependencyBuilder.requestedBy(requestedByMap.get(depId));
                        }
                        if (file.isFile()) {
                            // In recent gradle builds (3.4+) subproject dependencies are represented by a dir not jar.
                            Map<String, String> checksums = FileChecksumCalculator.calculateChecksums(file, MD5_ALGORITHM, SHA1_ALGORITHM, SHA256_ALGORITHM);
                            dependencyBuilder.md5(checksums.get(MD5_ALGORITHM)).sha1(checksums.get(SHA1_ALGORITHM)).sha256(checksums.get(SHA256_ALGORITHM));
                        }
                        dependencies.add(dependencyBuilder.build());
                    }
                }
            }
        }
        return dependencies;
    }

    private static class ProjectPredicate implements Predicate<GradleDeployDetails> {
        private final Project project;

        private ProjectPredicate(Project project) {
            this.project = project;
        }

        @Override
        public boolean test(@Nullable GradleDeployDetails input) {
            if (input == null) {
                return false;
            }
            return input.getProject().equals(project);
        }
    }

    private static class IncludeExcludePredicate implements Predicate<GradleDeployDetails> {
        private final Project project;
        private final IncludeExcludePatterns patterns;
        private final boolean include;

        public IncludeExcludePredicate(Project project, IncludeExcludePatterns patterns, boolean include) {
            this.project = project;
            this.patterns = patterns;
            this.include = include;
        }

        @Override
        public boolean test(@Nullable GradleDeployDetails input) {
            if (input == null) {
                return false;
            }
            if (include) {
                return input.getProject().equals(project) && !PatternMatcher.pathConflicts(input.getDeployDetails().getArtifactPath(), patterns);
            }
            return input.getProject().equals(project) && PatternMatcher.pathConflicts(input.getDeployDetails().getArtifactPath(), patterns);
        }
    }
}
