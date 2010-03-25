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
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.util.GUtil;
import org.jfrog.build.api.Agent;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.extractor.BuildInfoExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static com.google.common.collect.Iterables.*;
import static com.google.common.collect.Lists.newArrayList;

/**
 * An upload task uploads files to the repositories assigned to it.  The files that get uploaded are the artifacts of
 * your project, if they belong to the configuration associated with the upload task.
 *
 * @author Tomer Cohen
 */
public class BuildInfoRecorder extends ConventionTask implements BuildInfoExtractor<Project, Build> {
    private static final Logger log = LoggerFactory.getLogger(BuildInfoRecorder.class);

    public static final String BUILD_INFO_KEY = "buildInfo";
    public static final String BUILD_START_PROPERTY_NAME = "build.start";
    public static final String BUILD_NUMBER_PROPERTY_NAME = "build.number";
    public static final String BUILD_NAME_PROPERTY_NAME = "build.name";
    public static final String BUILD_PARENT_NAME_PROPERTY_NAME = "build.parentName";
    public static final String BUILD_PARENT_NUMBER_PROPERTY_NAME = "build.parentNumber";


    private Configuration configuration;
    private org.jfrog.build.api.Module module;

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    @TaskAction
    public Build extract(Project project) {
        ModuleBuilder builder = new ModuleBuilder()
                .id(project.getGroup() + ":" + project.getName() + ":" + project.getVersion().toString());
        if (getConfiguration() != null) {
            try {
                builder.artifacts(calculateArtifacts(project)).dependencies(calculateDependencies());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        module = builder.build();
        if (project.equals(project.getRootProject())) {
            //Deploy time!
            return closeAndDeploy(project);
        }
        return null;
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
                            return new ArtifactBuilder(from.getName()).type(type)
                                    .md5(calculateMd5ForArtifact(from.getFile())).build();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return new Artifact();
                    }
                }));

        File mavenPom = new File(getProject().getRepositories().getMavenPomDir(), "pom-default.xml");
        if (mavenPom.exists()) {
            Artifact pom =
                    new ArtifactBuilder(getProject().getName()).md5(calculateMd5ForArtifact(mavenPom)).type("pom")
                            .build();
            artifacts.add(pom);
        }
        File ivy = new File(getProject().getBuildDir(), "ivy.xml");
        if (ivy.exists()) {
            Artifact ivyArtifact =
                    new ArtifactBuilder(getProject().getName()).md5(calculateMd5ForArtifact(ivy)).type("ivy").build();
            artifacts.add(ivyArtifact);
        }
        return artifacts;
    }

    private List<Dependency> calculateDependencies() throws Exception {
        Set<Configuration> configurationSet = getProject().getConfigurations().getAll();
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
                    dependencyBuilder.type(artifact.getType()).id(depId)
                            .scopes(newArrayList(configuration.getName())).
                            md5(calculateMd5ForArtifact(artifact.getFile()));
                    dependencies.add(dependencyBuilder.build());
                }
            }
        }
        return dependencies;
    }

    private String calculateMd5ForArtifact(File file) throws Exception {
        if (file != null && file.exists()) {
            log.debug("Calculating MD5 for file: " + file.getAbsolutePath());
            MessageDigest digest = MessageDigest.getInstance("MD5");
            InputStream is = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String output = bigInt.toString(16);
            log.debug("MD5: " + output);
            return output;
        }
        return "";
    }

    /**
     * Returns the artifacts which will be uploaded.
     *
     * @param project Tomer will document later.
     * @throws java.io.IOException Tomer will document later.
     *//*
    @InputFiles
    public FileCollection getArtifacts() {
        Configuration configuration = getConfiguration();
        return configuration == null ? null : configuration.getAllArtifactFiles();
    }*/
    private Build closeAndDeploy(Project project) {
        GradleInternal gradleInternals = (GradleInternal) project.getGradle();
        Properties gradleProps = new Properties();
        File projectPropsFile = new File(project.getProjectDir(), Project.GRADLE_PROPERTIES);
        if (projectPropsFile.exists()) {
            Properties projectProps = GUtil.loadProperties(projectPropsFile);
            gradleProps.putAll(projectProps);
        }
        gradleProps.putAll(System.getProperties());
        File gradleUserProps =
                new File(gradleInternals.getStartParameter().getGradleUserHomeDir(), Project.GRADLE_PROPERTIES);
        if (gradleUserProps.exists()) {
            Properties gradleUserPropsFile = GUtil.loadProperties(projectPropsFile);
            gradleProps.putAll(gradleUserPropsFile);
        }
        long startTime = Long.parseLong(System.getProperty(BUILD_START_PROPERTY_NAME));
        String buildName = (String) gradleProps.get(BUILD_NAME_PROPERTY_NAME);
        if (buildName == null) {
            buildName = project.getName();
        }
        BuildInfoBuilder buildInfoBuilder = new BuildInfoBuilder(buildName);
        Date startedDate = new Date();
        startedDate.setTime(startTime);
        Object buildNumber = gradleProps.get(BUILD_NUMBER_PROPERTY_NAME);
        if (buildNumber == null) {
            String message =
                    "Build number not set, please provide system variable \'" + BUILD_NUMBER_PROPERTY_NAME + "\'";
            log.error(message);
            throw new GradleException(message);
        }
        buildInfoBuilder.agent(new Agent("Gradle", gradleInternals.getGradleVersion()))
                .durationMillis(System.currentTimeMillis() - startTime)
                .startedDate(startedDate)
                .number(Long.parseLong((String) buildNumber));
        for (Project subProject : project.getSubprojects()) {
            addModule(buildInfoBuilder, subProject);
        }
        addModule(buildInfoBuilder, project);
        String parentName = (String) gradleProps.get(BUILD_PARENT_NAME_PROPERTY_NAME);
        String parentNumber = (String) gradleProps.get(BUILD_PARENT_NUMBER_PROPERTY_NAME);
        if (parentName != null && parentNumber != null) {
            String parent = parentName + ":" + parentNumber;
            buildInfoBuilder.parentBuildId(parent);
        }
        String propertyPrefixes = gradleProps.getProperty("artifactory.propertyPrefixes");
        if (propertyPrefixes != null && !propertyPrefixes.isEmpty()) {
            if (!propertyPrefixes.equals("*")) {
                final String[] groups = propertyPrefixes.split(",");
                Properties fileteredProps = new Properties();
                fileteredProps.putAll(Maps.filterKeys(gradleProps, new Predicate<Object>() {
                    public boolean apply(@Nullable Object input) {
                        String key = (String) input;
                        for (String group : groups) {
                            if (key.startsWith(group)) {
                                return true;
                            }
                        }
                        return false;
                    }
                }));
                gradleProps = fileteredProps;
            }
            buildInfoBuilder.properties(gradleProps);
        }
        Build build = buildInfoBuilder.build();
        log.debug("buildInfoBuilder = " + buildInfoBuilder);
        return build;
    }

    private void addModule(BuildInfoBuilder buildInfoBuilder, Project project) {
        Set<Task> buildInfoTasks = project.getTasksByName(BUILD_INFO_KEY, false);
        for (Task task : buildInfoTasks) {
            BuildInfoRecorder buildInfoTask = (BuildInfoRecorder) task;
            Module module = buildInfoTask.module;
            if (module != null) {
                buildInfoBuilder.addModule(module);
            }
        }
    }

}
