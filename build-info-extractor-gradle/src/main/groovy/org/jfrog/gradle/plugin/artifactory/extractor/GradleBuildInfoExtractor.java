/*
 * Copyright (C) 2011 JFrog Ltd.
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

package org.jfrog.gradle.plugin.artifactory.extractor;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.commons.lang.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.jfrog.build.api.*;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.builder.PromotionStatusBuilder;
import org.jfrog.build.api.release.Promotion;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.client.ArtifactoryClientConfiguration;
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.extractor.BuildInfoExtractor;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static com.google.common.collect.Iterables.*;
import static com.google.common.collect.Lists.newArrayList;
import static org.jfrog.build.extractor.BuildInfoExtractorUtils.getModuleIdString;
import static org.jfrog.build.extractor.BuildInfoExtractorUtils.getTypeString;

/**
 * An upload task uploads files to the repositories assigned to it.  The files that get uploaded are the artifacts of
 * your project, if they belong to the configuration associated with the upload task.
 *
 * @author Tomer Cohen
 */
public class GradleBuildInfoExtractor implements BuildInfoExtractor<Project, Build> {
    private static final Logger log = Logging.getLogger(GradleBuildInfoExtractor.class);

    private static final String SHA1 = "sha1";
    private static final String MD5 = "md5";
    private final ArtifactoryClientConfiguration clientConf;
    private final Set<GradleDeployDetails> gradleDeployDetails;


    public GradleBuildInfoExtractor(ArtifactoryClientConfiguration clientConf,
            Set<GradleDeployDetails> gradleDeployDetails) {
        this.clientConf = clientConf;
        this.gradleDeployDetails = gradleDeployDetails;
    }

    @Override
    public Build extract(Project rootProject) {
        String buildName = clientConf.info.getBuildName();
        BuildInfoBuilder bib = new BuildInfoBuilder(buildName);

        bib.type(BuildType.GRADLE); //backward compat

        String buildNumber = clientConf.info.getBuildNumber();
        bib.number(buildNumber);

        String buildStartedIso = clientConf.info.getBuildStarted();
        Date buildStartDate = null;
        try {
            buildStartDate = new SimpleDateFormat(Build.STARTED_FORMAT).parse(buildStartedIso);
        } catch (ParseException e) {
            log.error("Build start date format error: " + buildStartedIso, e);
        }
        bib.started(buildStartedIso);

        BuildAgent buildAgent =
                new BuildAgent(clientConf.info.getBuildAgentName(), clientConf.info.getBuildAgentVersion());
        bib.buildAgent(buildAgent);

        //CI agent
        String agentName = clientConf.info.getAgentName();
        String agentVersion = clientConf.info.getAgentVersion();
        if (StringUtils.isNotBlank(agentName) && StringUtils.isNotBlank(agentVersion)) {
            bib.agent(new Agent(agentName, agentVersion));
        } else {
            //Fallback for standalone builds
            bib.agent(new Agent(buildAgent.getName(), buildAgent.getVersion()));
        }

        long durationMillis = buildStartDate != null ? System.currentTimeMillis() - buildStartDate.getTime() : 0;
        bib.durationMillis(durationMillis);

        Set<Project> allProjects = rootProject.getAllprojects();
        for (Project project : allProjects) {
            BuildInfoTask buildInfoTask = getBuildInfoTask(project);
            if (buildInfoTask != null && buildInfoTask.hasConfigurations()) {
                bib.addModule(extractModule(project));
            }
        }
        String parentName = clientConf.info.getParentBuildName();
        String parentNumber = clientConf.info.getParentBuildNumber();
        if (parentName != null && parentNumber != null) {
            bib.parentName(parentName);
            bib.parentNumber(parentNumber);
        }
        String principal = clientConf.info.getPrincipal();
        if (StringUtils.isBlank(principal)) {
            principal = System.getProperty("user.name");
        }
        bib.principal(principal);
        String artifactoryPrincipal = clientConf.publisher.getUsername();
        if (StringUtils.isBlank(artifactoryPrincipal)) {
            artifactoryPrincipal = System.getProperty("user.name");
        }
        bib.artifactoryPrincipal(artifactoryPrincipal);
        String buildUrl = clientConf.info.getBuildUrl();
        if (StringUtils.isNotBlank(buildUrl)) {
            bib.url(buildUrl);
        }
        String vcsRevision = clientConf.info.getVcsRevision();
        if (StringUtils.isNotBlank(vcsRevision)) {
            bib.vcsRevision(vcsRevision);
        }

        LicenseControl licenseControl = new LicenseControl(clientConf.info.licenseControl.isRunChecks());
        String notificationRecipients = clientConf.info.licenseControl.getViolationRecipients();
        if (StringUtils.isNotBlank(notificationRecipients)) {
            licenseControl.setLicenseViolationsRecipientsList(notificationRecipients);
        }
        licenseControl.setIncludePublishedArtifacts(clientConf.info.licenseControl.isIncludePublishedArtifacts());
        String scopes = clientConf.info.licenseControl.getScopes();
        if (StringUtils.isNotBlank(scopes)) {
            licenseControl.setScopesList(scopes);
        }
        licenseControl.setAutoDiscover(clientConf.info.licenseControl.isAutoDiscover());
        bib.licenseControl(licenseControl);

        BlackDuckProperties blackDuckProperties = new BlackDuckProperties();
        blackDuckProperties.setRunChecks(clientConf.info.blackDuckProperties.isRunChecks());
        blackDuckProperties.setAppName(clientConf.info.blackDuckProperties.getAppName());
        blackDuckProperties.setAppVersion(clientConf.info.blackDuckProperties.getAppVersion());
        blackDuckProperties.setReportRecipients(clientConf.info.blackDuckProperties.getReportRecipients());
        blackDuckProperties.setScopes(clientConf.info.blackDuckProperties.getScopes());
        blackDuckProperties.setIncludePublishedArtifacts(clientConf.info.blackDuckProperties.isIncludePublishedArtifacts());
        bib.blackDuckProperties(blackDuckProperties);

        BuildRetention buildRetention = new BuildRetention(clientConf.info.isDeleteBuildArtifacts());
        Integer count = clientConf.info.getBuildRetentionDays();
        if (count != null) {
            buildRetention.setCount(count);
        }
        String buildRetentionMinimumDays = clientConf.info.getBuildRetentionMinimumDate();
        if (StringUtils.isNotBlank(buildRetentionMinimumDays)) {
            int minimumDays = Integer.parseInt(buildRetentionMinimumDays);
            if (minimumDays > -1) {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, -minimumDays);
                buildRetention.setMinimumBuildDate(calendar.getTime());
            }
        }
        String[] notToDelete = clientConf.info.getBuildNumbersNotToDelete();
        for (String notToDel : notToDelete) {
            buildRetention.addBuildNotToBeDiscarded(notToDel);
        }
        bib.buildRetention(buildRetention);
        if (clientConf.info.isReleaseEnabled()) {
            String stagingRepository = clientConf.publisher.getRepoKey();
            String comment = clientConf.info.getReleaseComment();
            if (comment == null) {
                comment = "";
            }
            bib.addStatus(new PromotionStatusBuilder(Promotion.STAGED).timestampDate(buildStartDate)
                    .comment(comment).repository(stagingRepository)
                    .ciUser(principal).user(artifactoryPrincipal).build());
        }

        String issueTrackerName = clientConf.info.issues.getIssueTrackerName();
        if (StringUtils.isNotBlank(issueTrackerName)) {
            Issues issues = new Issues();
            issues.setAggregateBuildIssues(clientConf.info.issues.getAggregateBuildIssues());
            issues.setAggregationBuildStatus(clientConf.info.issues.getAggregationBuildStatus());
            issues.setTracker(new IssueTracker(issueTrackerName, clientConf.info.issues.getIssueTrackerVersion()));
            Set<Issue> affectedIssuesSet = clientConf.info.issues.getAffectedIssuesSet();
            if (!affectedIssuesSet.isEmpty()) {
                issues.setAffectedIssues(affectedIssuesSet);
            }
            bib.issues(issues);
        }

        if (clientConf.isIncludeEnvVars()) {
            Properties envProperties = new Properties();
            envProperties.putAll(clientConf.getAllProperties());
            envProperties = BuildInfoExtractorUtils.getEnvProperties(envProperties);
            for (Map.Entry<Object, Object> envProp : envProperties.entrySet()) {
                bib.addProperty(envProp.getKey(), envProp.getValue());
            }
        }
        log.debug("buildInfoBuilder = " + bib);
        // for backward compatibility for Artifactory 2.2.3
        Build build = bib.build();
        if (parentName != null && parentNumber != null) {
            build.setParentBuildId(parentName);
        }
        return build;
    }

    private BuildInfoTask getBuildInfoTask(Project project) {
        Set<Task> tasks = project.getTasksByName(BuildInfoTask.BUILD_INFO_TASK_NAME, false);
        if (tasks.isEmpty()) {
            return null;
        }
        BuildInfoTask buildInfoTask = (BuildInfoTask) tasks.iterator().next();
        if (buildInfoTask.getState().getDidWork()) {
            return buildInfoTask;
        }
        return null;
    }


    public Module extractModule(Project project) {
        String artifactName = project.getName();
        BuildInfoTask task = getBuildInfoTask(project);
        if (task != null) {
            artifactName = project.getName();
        }
        ModuleBuilder builder = new ModuleBuilder()
                .id(getModuleIdString(project.getGroup().toString(),
                        artifactName, project.getVersion().toString()));
        try {
            builder.artifacts(calculateArtifacts(project))
                    .dependencies(calculateDependencies(project));
        } catch (Exception e) {
            log.error("Error during extraction: ", e);
        }
        return builder.build();
    }

    private List<Artifact> calculateArtifacts(final Project project) throws Exception {
        Iterable<GradleDeployDetails> deployDetails = getProjectDeployDetails(project);
        List<Artifact> artifacts = newArrayList(transform(deployDetails, new Function<GradleDeployDetails, Artifact>() {
            public Artifact apply(GradleDeployDetails from) {
                PublishArtifactInfo publishArtifact = from.getPublishArtifact();
                DeployDetails deployDetails = from.getDeployDetails();
                String artifactPath = deployDetails.getArtifactPath();
                int index = artifactPath.lastIndexOf('/');
                return new ArtifactBuilder(artifactPath.substring(index + 1))
                        .type(getTypeString(publishArtifact.getType(),
                                publishArtifact.getClassifier(), publishArtifact.getExtension()))
                        .md5(deployDetails.getMd5()).sha1(deployDetails.getSha1()).build();
            }
        }));
        return artifacts;
    }

    private Iterable<GradleDeployDetails> getProjectDeployDetails(final Project project) {
        return Iterables.filter(gradleDeployDetails, new Predicate<GradleDeployDetails>() {
            public boolean apply(@Nullable GradleDeployDetails input) {
                return input.getProject().equals(project);
            }
        });
    }

    private List<Dependency> calculateDependencies(Project project) throws Exception {
        Set<Configuration> configurationSet = project.getConfigurations();
        List<Dependency> dependencies = newArrayList();
        for (Configuration configuration : configurationSet) {
            if (configuration.getState() != Configuration.State.RESOLVED) {
                log.info("Artifacts for configuration '{}' were not all resolved, skipping", configuration.getName());
                continue;
            }
            ResolvedConfiguration resolvedConfiguration = configuration.getResolvedConfiguration();
            Set<ResolvedArtifact> resolvedArtifactSet = resolvedConfiguration.getResolvedArtifacts();
            for (final ResolvedArtifact artifact : resolvedArtifactSet) {
                File file = artifact.getFile();
                if (file != null && file.exists()) {
                    ModuleVersionIdentifier id = artifact.getModuleVersion().getId();
                    final String depId = getModuleIdString(id.getGroup(),
                            id.getName(), id.getVersion());
                    Predicate<Dependency> idEqualsPredicate = new Predicate<Dependency>() {
                        public boolean apply(@Nullable Dependency input) {
                            return input.getId().equals(depId);
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
                        Map<String, String> checksums = FileChecksumCalculator.calculateChecksums(file, MD5, SHA1);
                        DependencyBuilder dependencyBuilder = new DependencyBuilder()
                                .type(getTypeString(artifact.getType(),
                                        artifact.getClassifier(), artifact.getExtension()))
                                .id(depId)
                                .scopes(newArrayList(configuration.getName())).
                                        md5(checksums.get(MD5)).sha1(checksums.get(SHA1));
                        dependencies.add(dependencyBuilder.build());
                    }
                }
            }
        }
        return dependencies;
    }
}