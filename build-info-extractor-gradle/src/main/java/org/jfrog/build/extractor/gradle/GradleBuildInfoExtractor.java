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

package org.jfrog.build.extractor.gradle;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import org.apache.commons.lang.StringUtils;
import org.gradle.StartParameter;
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
import org.jfrog.build.ArtifactoryPluginUtils;
import org.jfrog.build.api.*;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.client.ClientGradleProperties;
import org.jfrog.build.client.ClientIvyProperties;
import org.jfrog.build.client.ClientProperties;
import org.jfrog.build.extractor.BuildInfoExtractor;
import org.jfrog.build.extractor.BuildInfoExtractorSpec;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
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
    private Properties buildInfoProps;

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
        gradleProps = new Properties();
        gradleProps.putAll(startParamProps);
        File projectPropsFile = new File(rootProject.getProjectDir(), Project.GRADLE_PROPERTIES);
        if (projectPropsFile.exists()) {
            Properties properties = GUtil.loadProperties(projectPropsFile);
            gradleProps.putAll(properties);
        }
        Properties mergedProps = BuildInfoExtractorUtils.mergePropertiesWithSystemAndPropertyFile(startParamProps);
        buildInfoProps = new Properties();
        buildInfoProps.putAll(BuildInfoExtractorUtils.filterDynamicProperties(mergedProps,
                BuildInfoExtractorUtils.BUILD_INFO_PROP_PREDICATE));
    }

    public Build extract(BuildInfoRecorderTask buildInfoTask, BuildInfoExtractorSpec spec) {
        Project rootProject = buildInfoTask.getRootProject();
        long startTime = Long.parseLong(System.getProperty("build.start"));
        String buildName = ArtifactoryPluginUtils.getProperty(PROP_BUILD_NAME, rootProject);
        if (StringUtils.isBlank(buildName)) {
            buildName = rootProject.getName();
        }
        BuildInfoBuilder buildInfoBuilder = new BuildInfoBuilder(buildName);
        Date startedDate = new Date();
        startedDate.setTime(startTime);
        buildInfoBuilder.type(BuildType.GRADLE);
        String buildNumber = ArtifactoryPluginUtils.getProperty(PROP_BUILD_NUMBER, rootProject);
        if (buildNumber == null) {
            buildNumber = System.getProperty("timestamp", Long.toString(System.currentTimeMillis()));
        }
        GradleInternal gradleInternals = (GradleInternal) rootProject.getGradle();
        BuildAgent buildAgent = new BuildAgent("Gradle", gradleInternals.getGradleVersion());
        // If
        String agentString = buildAgent.toString();
        Agent agent = new Agent(agentString);
        String buildAgentNameProp = ArtifactoryPluginUtils.getProperty(BuildInfoProperties.PROP_BUILD_AGENT_NAME,
                rootProject);
        String buildAgentVersionProp = ArtifactoryPluginUtils.getProperty(BuildInfoProperties.PROP_BUILD_AGENT_VERSION,
                rootProject);
        if (StringUtils.isNotBlank(buildAgentNameProp) && StringUtils.isNotBlank(buildAgentVersionProp)) {
            agent = new Agent(buildAgentNameProp, buildAgentVersionProp);
        }
        buildInfoBuilder.agent(agent)
                .durationMillis(System.currentTimeMillis() - startTime)
                .startedDate(startedDate).number(buildNumber)
                .buildAgent(buildAgent);
        Set<Project> subProjects = rootProject.getSubprojects();
        if (subProjects.isEmpty()) {
            Set<Configuration> configurations = rootProject.getConfigurations().getAll();
            for (Configuration configuration : configurations) {
                if ((!configuration.getArtifacts().isEmpty())) {
                    buildInfoBuilder.addModule(extractModule(configuration, rootProject));
                }
            }
        } else {
            for (Project subProject : subProjects) {
                Set<Configuration> configurations = subProject.getConfigurations().getAll();
                for (Configuration configuration : configurations) {
                    if ((!configuration.getArtifacts().isEmpty())) {
                        buildInfoBuilder.addModule(extractModule(configuration, subProject));
                    }
                }
            }
        }
        String parentName = ArtifactoryPluginUtils.getProperty(PROP_PARENT_BUILD_NAME, rootProject);
        String parentNumber = ArtifactoryPluginUtils.getProperty(PROP_PARENT_BUILD_NUMBER, rootProject);
        if (parentName != null && parentNumber != null) {
            buildInfoBuilder.parentName(parentName);
            buildInfoBuilder.parentNumber(parentNumber);
        }
        String principal = ArtifactoryPluginUtils.getProperty(PROP_PRINCIPAL, rootProject);
        if (StringUtils.isBlank(principal)) {
            principal = System.getProperty("user.name");
        }
        buildInfoBuilder.principal(principal);
        String artifactoryPrincipal =
                ArtifactoryPluginUtils.getProperty(ClientProperties.PROP_PUBLISH_USERNAME, rootProject);
        if (StringUtils.isBlank(artifactoryPrincipal)) {
            artifactoryPrincipal = System.getProperty("user.name");
        }
        buildInfoBuilder.artifactoryPrincipal(artifactoryPrincipal);
        String buildUrl = ArtifactoryPluginUtils.getProperty(BuildInfoProperties.PROP_BUILD_URL, rootProject);
        if (StringUtils.isNotBlank(buildUrl)) {
            buildInfoBuilder.url(buildUrl);
        }
        String vcsRevision = ArtifactoryPluginUtils.getProperty(BuildInfoProperties.PROP_VCS_REVISION, rootProject);
        if (StringUtils.isNotBlank(vcsRevision)) {
            buildInfoBuilder.vcsRevision(vcsRevision);
        }
        boolean runLicenseChecks = true;
        String runChecks =
                ArtifactoryPluginUtils.getProperty(BuildInfoProperties.PROP_LICENSE_CONTROL_RUN_CHECKS, rootProject);
        if (StringUtils.isNotBlank(runChecks)) {
            runLicenseChecks = Boolean.parseBoolean(runChecks);
        }
        LicenseControl licenseControl = new LicenseControl(runLicenseChecks);
        String notificationRecipients = ArtifactoryPluginUtils
                .getProperty(BuildInfoProperties.PROP_LICENSE_CONTROL_VIOLATION_RECIPIENTS, rootProject);
        if (StringUtils.isNotBlank(notificationRecipients)) {
            licenseControl.setLicenseViolationsRecipientsList(notificationRecipients);
        }
        String includePublishedArtifacts = ArtifactoryPluginUtils
                .getProperty(BuildInfoProperties.PROP_LICENSE_CONTROL_INCLUDE_PUBLISHED_ARTIFACTS, rootProject);
        if (StringUtils.isNotBlank(includePublishedArtifacts)) {
            licenseControl.setIncludePublishedArtifacts(Boolean.parseBoolean(includePublishedArtifacts));
        }
        String scopes =
                ArtifactoryPluginUtils.getProperty(BuildInfoProperties.PROP_LICENSE_CONTROL_SCOPES, rootProject);
        if (StringUtils.isNotBlank(scopes)) {
            licenseControl.setScopesList(scopes);
        }
        String autoDiscover =
                ArtifactoryPluginUtils.getProperty(BuildInfoProperties.PROP_LICENSE_CONTROL_AUTO_DISCOVER, rootProject);
        if (StringUtils.isNotBlank(autoDiscover)) {
            licenseControl.setAutoDiscover(Boolean.parseBoolean(autoDiscover));
        }
        buildInfoBuilder.licenseControl(licenseControl);
        BuildRetention buildRetention = new BuildRetention();
        String buildRetentionDays =
                ArtifactoryPluginUtils.getProperty(BuildInfoProperties.PROP_BUILD_RETENTION_DAYS, rootProject);
        if (StringUtils.isNotBlank(buildRetentionDays)) {
            buildRetention.setCount(Integer.parseInt(buildRetentionDays));
        }
        String buildRetentionMinimumDays =
                ArtifactoryPluginUtils.getProperty(BuildInfoProperties.PROP_BUILD_RETENTION_MINIMUM_DATE, rootProject);
        if (StringUtils.isNotBlank(buildRetentionMinimumDays)) {
            int minimumDays = Integer.parseInt(buildRetentionMinimumDays);
            if (minimumDays > -1) {
                Calendar calendar = Calendar.getInstance();
                calendar.roll(Calendar.DAY_OF_YEAR, -minimumDays);
                buildRetention.setMinimumBuildDate(calendar.getTime());
            }
        }
        buildInfoBuilder.buildRetention(buildRetention);
        Properties properties = gatherSysPropInfo();
        properties.putAll(buildInfoProps);
        properties.putAll(BuildInfoExtractorUtils.getEnvProperties(startParamProps));
        buildInfoBuilder.properties(properties);
        log.debug("buildInfoBuilder = " + buildInfoBuilder);
        // for backward compatibility for Artifactory 2.2.3
        Build build = buildInfoBuilder.build();
        if (parentName != null && parentNumber != null) {
            build.setParentBuildId(parentName);
        }
        return build;
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
                            File artifactFile = from.getFile();
                            if (artifactFile != null && artifactFile.exists()) {
                                Map<String, String> checkSums = calculateChecksumsForFile(artifactFile);
                                return new ArtifactBuilder(from.getFile().getName()).type(type)
                                        .md5(checkSums.get(MD5)).sha1(checkSums.get(SHA1)).build();
                            }
                        } catch (Exception e) {
                            log.error("Error during artifact calculation: ", e);
                        }
                        return new Artifact();
                    }
                }));

        File mavenPom = new File(project.getRepositories().getMavenPomDir(), "pom-default.xml");
        String publishPom = ArtifactoryPluginUtils.getProperty(ClientGradleProperties.PROP_PUBLISH_MAVEN, project);
        boolean isPublishPom = StringUtils.isNotBlank(publishPom) && Boolean.parseBoolean(publishPom);
        if (mavenPom.exists() && isPublishPom) {
            Map<String, String> checksums = calculateChecksumsForFile(mavenPom);
            Artifact pom =
                    new ArtifactBuilder(project.getName() + "-" + project.getVersion() + ".pom").md5(checksums.get(MD5))
                            .sha1(checksums.get(SHA1)).type("pom")
                            .build();
            artifacts.add(pom);
        }
        String publishIvy = ArtifactoryPluginUtils.getProperty(ClientIvyProperties.PROP_PUBLISH_IVY, project);
        boolean isPublishIvy = StringUtils.isNotBlank(publishIvy) && Boolean.parseBoolean(publishIvy);
        File ivy = new File(project.getBuildDir(), "ivy.xml");
        if (ivy.exists() && isPublishIvy) {
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
                /**
                 * If including sources jar the jars will have the same ID despite one of them having a sources
                 * classifier, this fix will remain until GAP-13 is fixed, on both our side and the Gradle side.
                 */
                File file = artifact.getFile();
                if (file != null && file.exists() && !file.getName().endsWith("-sources.jar")) {
                    String depId = resolvedDependency.getName();
                    final String finalDepId = depId;
                    Predicate<Dependency> idEqualsPredicate = new Predicate<Dependency>() {
                        public boolean apply(@Nullable Dependency input) {
                            return input.getId().equals(finalDepId);
                        }
                    };
                    // if it's already in the dependencies list just add the current scope
                    if (any(dependencies, idEqualsPredicate)) {
                        Dependency existingDependency = find(dependencies, idEqualsPredicate);
                        List<String> existingScopes = existingDependency.getScopes();
                        String configScope = configuration.getName();
                        if (!existingScopes.contains(configScope)) {
                            existingScopes.add(configScope);
                        }
                    } else {
                        Map<String, String> checksums = calculateChecksumsForFile(file);
                        DependencyBuilder dependencyBuilder = new DependencyBuilder()
                                .type(artifact.getType()).id(depId)
                                .scopes(newArrayList(configuration.getName())).
                                        md5(checksums.get(MD5)).sha1(checksums.get(SHA1));
                        dependencies.add(dependencyBuilder.build());
                    }
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
