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
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.ivy.core.IvyPatternHelper;
import org.gradle.StartParameter;
import org.gradle.api.Project;
import org.gradle.api.Task;
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
import org.jfrog.build.api.builder.PromotionStatusBuilder;
import org.jfrog.build.api.release.Promotion;
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
import static org.jfrog.build.extractor.BuildInfoExtractorUtils.BUILD_INFO_PROP_PREDICATE;

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
    private final Properties mergedProps;

    public Properties getGradleProps() {
        return gradleProps;
    }

    public Properties getStartParamProps() {
        return startParamProps;
    }

    GradleBuildInfoExtractor(Project rootProject, Properties mergedProps) {
        this.mergedProps = mergedProps;
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
        buildInfoProps = new Properties();
        Properties buildInfoProperties =
                BuildInfoExtractorUtils.filterDynamicProperties(mergedProps, BUILD_INFO_PROP_PREDICATE);
        buildInfoProperties =
                BuildInfoExtractorUtils.stripPrefixFromProperties(buildInfoProperties, BUILD_INFO_PROP_PREFIX);
        buildInfoProps.putAll(buildInfoProperties);
    }


    public Build extract(BuildInfoRecorderTask buildInfoTask, BuildInfoExtractorSpec spec) {
        Project rootProject = buildInfoTask.getRootProject();
        long startTime = Long.parseLong(mergedProps.getProperty("build.start"));
        String buildName = mergedProps.getProperty(PROP_BUILD_NAME);
        if (StringUtils.isBlank(buildName)) {
            buildName = rootProject.getName();
        }
        BuildInfoBuilder buildInfoBuilder = new BuildInfoBuilder(buildName);
        Date startedDate = new Date();
        startedDate.setTime(startTime);
        buildInfoBuilder.type(BuildType.GRADLE);
        String buildNumber = mergedProps.getProperty(PROP_BUILD_NUMBER);
        if (buildNumber == null) {
            buildNumber = System.getProperty("timestamp", Long.toString(System.currentTimeMillis()));
        }
        GradleInternal gradleInternals = (GradleInternal) rootProject.getGradle();
        BuildAgent buildAgent = new BuildAgent("Gradle", gradleInternals.getGradleVersion());
        String buildAgentNameProp = mergedProps.getProperty(BuildInfoProperties.PROP_AGENT_NAME);
        String buildAgentVersionProp = mergedProps.getProperty(BuildInfoProperties.PROP_AGENT_VERSION);
        if (StringUtils.isNotBlank(buildAgentNameProp) && StringUtils.isNotBlank(buildAgentVersionProp)) {
            buildInfoBuilder.agent(new Agent(buildAgentNameProp, buildAgentVersionProp));
        }
        buildInfoBuilder.durationMillis(System.currentTimeMillis() - startTime)
                .startedDate(startedDate).number(buildNumber)
                .buildAgent(buildAgent);
        Set<Project> allProjects = rootProject.getAllprojects();
        for (Project project : allProjects) {
            BuildInfoRecorderTask buildInfoRecorderTask = getBuildInfoRecorderTask(project);
            if (buildInfoRecorderTask != null) {
                Configuration configuration = buildInfoRecorderTask.getConfiguration();
                if (configuration != null) {
                    if ((!configuration.getArtifacts().isEmpty())) {
                        buildInfoBuilder.addModule(extractModule(configuration, project));
                    }
                }
            }
        }
        String parentName = mergedProps.getProperty(PROP_PARENT_BUILD_NAME);
        String parentNumber = mergedProps.getProperty(PROP_PARENT_BUILD_NUMBER);
        if (parentName != null && parentNumber != null) {
            buildInfoBuilder.parentName(parentName);
            buildInfoBuilder.parentNumber(parentNumber);
        }
        String principal = mergedProps.getProperty(PROP_PRINCIPAL);
        if (StringUtils.isBlank(principal)) {
            principal = System.getProperty("user.name");
        }
        buildInfoBuilder.principal(principal);
        String artifactoryPrincipal = mergedProps.getProperty(ClientProperties.PROP_PUBLISH_USERNAME);
        if (StringUtils.isBlank(artifactoryPrincipal)) {
            artifactoryPrincipal = System.getProperty("user.name");
        }
        buildInfoBuilder.artifactoryPrincipal(artifactoryPrincipal);
        String buildUrl = mergedProps.getProperty(BuildInfoProperties.PROP_BUILD_URL);
        if (StringUtils.isNotBlank(buildUrl)) {
            buildInfoBuilder.url(buildUrl);
        }
        String vcsRevision = mergedProps.getProperty(BuildInfoProperties.PROP_VCS_REVISION);
        if (StringUtils.isNotBlank(vcsRevision)) {
            buildInfoBuilder.vcsRevision(vcsRevision);
        }
        boolean runLicenseChecks = true;
        String runChecks = mergedProps.getProperty(BuildInfoProperties.PROP_LICENSE_CONTROL_RUN_CHECKS);
        if (StringUtils.isNotBlank(runChecks)) {
            runLicenseChecks = Boolean.parseBoolean(runChecks);
        }
        LicenseControl licenseControl = new LicenseControl(runLicenseChecks);
        String notificationRecipients = mergedProps
                .getProperty(BuildInfoProperties.PROP_LICENSE_CONTROL_VIOLATION_RECIPIENTS);
        if (StringUtils.isNotBlank(notificationRecipients)) {
            licenseControl.setLicenseViolationsRecipientsList(notificationRecipients);
        }
        String includePublishedArtifacts = mergedProps
                .getProperty(BuildInfoProperties.PROP_LICENSE_CONTROL_INCLUDE_PUBLISHED_ARTIFACTS);
        if (StringUtils.isNotBlank(includePublishedArtifacts)) {
            licenseControl.setIncludePublishedArtifacts(Boolean.parseBoolean(includePublishedArtifacts));
        }
        String scopes = mergedProps.getProperty(BuildInfoProperties.PROP_LICENSE_CONTROL_SCOPES);
        if (StringUtils.isNotBlank(scopes)) {
            licenseControl.setScopesList(scopes);
        }
        String autoDiscover = mergedProps.getProperty(BuildInfoProperties.PROP_LICENSE_CONTROL_AUTO_DISCOVER);
        if (StringUtils.isNotBlank(autoDiscover)) {
            licenseControl.setAutoDiscover(Boolean.parseBoolean(autoDiscover));
        }
        buildInfoBuilder.licenseControl(licenseControl);
        BuildRetention buildRetention = new BuildRetention();
        String buildRetentionDays = mergedProps.getProperty(BuildInfoProperties.PROP_BUILD_RETENTION_DAYS);
        if (StringUtils.isNotBlank(buildRetentionDays)) {
            buildRetention.setCount(Integer.parseInt(buildRetentionDays));
        }
        String buildRetentionMinimumDays =
                mergedProps.getProperty(BuildInfoProperties.PROP_BUILD_RETENTION_MINIMUM_DATE);
        if (StringUtils.isNotBlank(buildRetentionMinimumDays)) {
            int minimumDays = Integer.parseInt(buildRetentionMinimumDays);
            if (minimumDays > -1) {
                Calendar calendar = Calendar.getInstance();
                calendar.roll(Calendar.DAY_OF_YEAR, -minimumDays);
                buildRetention.setMinimumBuildDate(calendar.getTime());
            }
        }
        buildInfoBuilder.buildRetention(buildRetention);
        String buildInfoEnabled = mergedProps.getProperty(BuildInfoProperties.PROP_RELEASE_ENABLED);
        if (StringUtils.isNotBlank(buildInfoEnabled) && Boolean.parseBoolean(buildInfoEnabled)) {
            String stagingRepository = mergedProps.getProperty(ClientProperties.PROP_PUBLISH_REPOKEY);
            String comment = mergedProps.getProperty(BuildInfoProperties.PROP_RELEASE_COMMENT, "");
            buildInfoBuilder.addStatus(new PromotionStatusBuilder(Promotion.STAGED).timestampDate(startedDate)
                    .comment(comment).repository(stagingRepository)
                    .ciUser(principal).user(artifactoryPrincipal).build());
        }
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

    private BuildInfoRecorderTask getBuildInfoRecorderTask(Project project) {
        Set<Task> tasks = project.getTasksByName("buildInfo", false);
        if (tasks.isEmpty()) {
            return null;
        }
        return (BuildInfoRecorderTask) tasks.iterator().next();
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
        String projectName = project.getName();
        BuildInfoRecorderTask task = getBuildInfoRecorderTask(project);
        if (task != null) {
            String artifactName = task.getArtifactName();
            if (StringUtils.isNotBlank(artifactName)) {
                projectName = ArtifactoryPluginUtils.getProjectName(project, artifactName);
            }
        }
        ModuleBuilder builder = new ModuleBuilder()
                .id(project.getGroup() + ":" + projectName + ":" + project.getVersion().toString());
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

    private List<Artifact> calculateArtifacts(final Configuration configuration, final Project project)
            throws Exception {
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
                                String pattern = ArtifactoryPluginUtils.getArtifactPattern(mergedProps);
                                Map<String, String> extraTokens = Maps.newHashMap();
                                if (StringUtils.isNotBlank(from.getClassifier())) {
                                    extraTokens.put("classifier", from.getClassifier());
                                }
                                String projectName = project.getName();
                                BuildInfoRecorderTask task = getBuildInfoRecorderTask(project);
                                if (task != null) {
                                    if (StringUtils.isNotBlank(task.getArtifactName())) {
                                        projectName = task.getArtifactName();
                                    }
                                }
                                String finalPattern = IvyPatternHelper.substitute(pattern,
                                        ArtifactoryPluginUtils.getGroupIdPatternByM2Compatible(project, mergedProps),
                                        projectName,
                                        project.getVersion().toString(), null, from.getType(),
                                        from.getExtension(), configuration.getName(),
                                        extraTokens, null);
                                int index = finalPattern.lastIndexOf('/');
                                return new ArtifactBuilder(finalPattern.substring(index + 1)).type(type)
                                        .md5(checkSums.get(MD5)).sha1(checkSums.get(SHA1)).build();
                            }
                        } catch (Exception e) {
                            log.error("Error during artifact calculation: ", e);
                        }
                        return new Artifact();
                    }
                }));

        File mavenPom = new File(project.getRepositories().getMavenPomDir(), "pom-default.xml");
        String publishPom = mergedProps.getProperty(ClientGradleProperties.PROP_PUBLISH_MAVEN);
        boolean isPublishPom = StringUtils.isNotBlank(publishPom) && Boolean.parseBoolean(publishPom);
        if (mavenPom.exists() && isPublishPom) {
            Map<String, String> checksums = calculateChecksumsForFile(mavenPom);
            String projectName = project.getName();
            BuildInfoRecorderTask task = getBuildInfoRecorderTask(project);
            if (task != null) {
                String artifactName = task.getArtifactName();
                if (StringUtils.isNotBlank(artifactName)) {
                    projectName = ArtifactoryPluginUtils.getProjectName(project, artifactName);
                }
            }
            Artifact pom =
                    new ArtifactBuilder(projectName + "-" + project.getVersion() + ".pom").md5(checksums.get(MD5))
                            .sha1(checksums.get(SHA1)).type("pom")
                            .build();
            artifacts.add(pom);
        }
        String publishIvy = mergedProps.getProperty(ClientIvyProperties.PROP_PUBLISH_IVY);
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
