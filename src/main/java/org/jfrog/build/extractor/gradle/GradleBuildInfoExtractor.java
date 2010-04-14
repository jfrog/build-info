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
import org.gradle.StartParameter;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.util.GUtil;
import org.jfrog.build.api.Agent;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildAgent;
import org.jfrog.build.api.BuildInfoProperties;
import org.jfrog.build.api.BuildType;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.extractor.BuildInfoExtractor;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static com.google.common.collect.Iterables.*;
import static com.google.common.collect.Lists.newArrayList;
import static org.jfrog.build.api.BuildInfoProperties.*;

/**
 * An upload task uploads files to the repositories assigned to it.  The files that get uploaded are the artifacts of
 * your project, if they belong to the configuration associated with the upload task.
 *
 * @author Tomer Cohen
 */
public class GradleBuildInfoExtractor implements BuildInfoExtractor<BuildInfoRecorderTask, Build> {
    private static final Logger log = Logging.getLogger(GradleBuildInfoExtractor.class);

    private static final String SHA1 = "sha1";
    private static final String MD5 = "md5";
    private Properties gradleProps;
    private Properties startParamProps;

    public Properties getGradleProps() {
        return gradleProps;
    }

    public Properties getStartParamProps() {
        return startParamProps;
    }

    GradleBuildInfoExtractor(Project rootProject) {
        StartParameter startParameter = rootProject.getGradle().getStartParameter();
        startParamProps = new Properties();
        startParamProps.putAll(startParameter.getProjectProperties());
        gradleProps = BuildInfoExtractorUtils.getBuildInfoProperties();
        File projectPropsFile = new File(rootProject.getProjectDir(), Project.GRADLE_PROPERTIES);
        if (projectPropsFile.exists()) {
            Properties properties = GUtil.loadProperties(projectPropsFile);
            gradleProps.putAll(BuildInfoExtractorUtils.filterProperties(properties));
        }
        gradleProps.putAll(BuildInfoExtractorUtils.filterProperties(startParamProps));
    }

    public Build extract(BuildInfoRecorderTask buildInfoTask) {
        Project rootProject = buildInfoTask.getRootProject();
        long startTime = Long.parseLong(System.getProperty("build.start"));
        String buildName = gradleProps.getProperty(PROP_BUILD_NAME);
        if (buildName == null) {
            buildName = rootProject.getName();
        }
        BuildInfoBuilder buildInfoBuilder = new BuildInfoBuilder(buildName);
        Date startedDate = new Date();
        startedDate.setTime(startTime);
        buildInfoBuilder.type(BuildType.GRADLE);
        String buildNumber = gradleProps.getProperty(PROP_BUILD_NUMBER);
        if (buildNumber == null) {
            String message = "Build number not set, please provide system variable \'" + PROP_BUILD_NUMBER + "\'";
            log.error(message);
            throw new GradleException(message);
        }
        GradleInternal gradleInternals = (GradleInternal) rootProject.getGradle();
        BuildAgent buildAgent = new BuildAgent("Gradle", gradleInternals.getGradleVersion());
        // If
        String agentString = startParamProps.getProperty(BuildInfoProperties.PROP_BUILD_AGENT, buildAgent.toString());
        Agent agent = new Agent(agentString);
        buildInfoBuilder.agent(agent)
                .durationMillis(System.currentTimeMillis() - startTime)
                .startedDate(startedDate).number(Long.parseLong(buildNumber))
                .buildAgent(buildAgent);
        for (Project subProject : rootProject.getSubprojects()) {
            BuildInfoRecorderTask birTask = (BuildInfoRecorderTask) subProject.getTasks().getByName("buildInfo");
            buildInfoBuilder.addModule(extractModule(birTask.getConfiguration(), subProject));
        }
        buildInfoBuilder.addModule(extractModule(buildInfoTask.getConfiguration(), rootProject));
        String parentName = gradleProps.getProperty(PROP_PARENT_BUILD_NAME);
        String parentNumber = gradleProps.getProperty(PROP_PARENT_BUILD_NUMBER);
        if (parentName != null && parentNumber != null) {
            String parent = parentName + ":" + parentNumber;
            buildInfoBuilder.parentBuildId(parent);
        }
        String buildUrl = gradleProps.getProperty(BuildInfoProperties.PROP_BUILD_URL);
        if (StringUtils.isNotBlank(buildUrl)) {
            buildInfoBuilder.url(buildUrl);
        }
        buildInfoBuilder.properties(gatherSysPropInfo());
        log.debug("buildInfoBuilder = " + buildInfoBuilder);
        return buildInfoBuilder.build();
    }

    private Properties gatherSysPropInfo() {
        Properties props = new Properties();
        props.setProperty("os.arch", System.getProperty("os.arch"));
        props.setProperty("os.name", System.getProperty("os.name"));
        props.setProperty("os.version", System.getProperty("os.version"));
        props.setProperty("java.version", System.getProperty("java.version"));
        props.setProperty("java.vm.info", System.getProperty("java.vm.info"));
        props.setProperty("java.vm.name", System.getProperty("java.vm.name"));
        props.setProperty("java.vm.specification.name", System.getProperty("java.vm.specification.name"));
        props.setProperty("java.vm.vendor", System.getProperty("java.vm.vendor"));
        return props;
    }

    public Module extractModule(Configuration configuration, Project project) {
        ModuleBuilder builder = new ModuleBuilder()
                .id(project.getGroup() + ":" + project.getName() + ":" + project.getVersion().toString());
        if (configuration != null) {
            try {
                builder.artifacts(calculateArtifacts(configuration, project))
                        .dependencies(calculateDependencies(project));
            } catch (Exception e) {
                log.error("Error during extraction: ", e);
            }
        }
        return builder.build();
    }

    private List<Artifact> calculateArtifacts(Configuration configuration, Project project) throws Exception {
        List<Artifact> artifacts = newArrayList(
                transform(configuration.getAllArtifacts(), new Function<PublishArtifact, Artifact>() {
                    public Artifact apply(PublishArtifact from) {
                        try {
                            String type = from.getType();
                            if (StringUtils.isNotBlank(from.getClassifier())) {
                                type = type + "-" + from.getClassifier();
                            }
                            Map<String, String> checkSums = calculateChecksumsForFile(from.getFile());
                            return new ArtifactBuilder(from.getFile().getName()).type(type)
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
                    new ArtifactBuilder(project.getName() + "-" + project.getVersion() + ".pom").md5(checksums.get(MD5))
                            .sha1(checksums.get(SHA1)).type("pom")
                            .build();
            artifacts.add(pom);
        }
        File ivy = new File(project.getBuildDir(), "ivy.xml");
        if (ivy.exists()) {
            Map<String, String> checksums = calculateChecksumsForFile(ivy);
            Artifact ivyArtifact =
                    new ArtifactBuilder("ivy-" + project.getVersion() + ".xml").md5(checksums.get(MD5))
                            .sha1(checksums.get(SHA1)).type("ivy")
                            .build();
            artifacts.add(ivyArtifact);
        }
        return artifacts;
    }

    private List<Dependency> calculateDependencies(Project project) throws Exception {
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
