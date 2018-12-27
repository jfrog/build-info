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
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.TaskState;
import org.jfrog.build.api.*;
import org.jfrog.build.api.builder.*;
import org.jfrog.build.api.release.Promotion;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.extractor.BuildInfoExtractor;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.IncludeExcludePatterns;
import org.jfrog.build.extractor.clientConfiguration.PatternMatcher;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPluginUtil;
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask;

import javax.annotation.Nullable;
import java.io.File;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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
public class GradleBuildInfoExtractor implements BuildInfoExtractor<Project> {
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
            if(project.getState().getExecuted()) {
                ArtifactoryTask buildInfoTask = getBuildInfoTask(project);
                if (buildInfoTask != null && buildInfoTask.hasModules()) {
                    bib.addModule(extractModule(project));
                }
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

        String artifactoryPluginVersion = clientConf.info.getArtifactoryPluginVersion();
        if (StringUtils.isBlank(artifactoryPluginVersion)){
            artifactoryPluginVersion = "Unknown";
        }
        bib.artifactoryPluginVersion(artifactoryPluginVersion);

        String buildUrl = clientConf.info.getBuildUrl();
        if (StringUtils.isNotBlank(buildUrl)) {
            bib.url(buildUrl);
        }
        String vcsRevision = clientConf.info.getVcsRevision();
        Vcs vcs = new Vcs();
        if (StringUtils.isNotBlank(vcsRevision)) {
            vcs.setRevision(vcsRevision);
            bib.vcsRevision(vcsRevision);
        }

        String vcsUrl = clientConf.info.getVcsUrl();
        if (StringUtils.isNotBlank(vcsUrl)) {
            vcs.setUrl(vcsUrl);
            bib.vcsUrl(vcsUrl);
        }
        if (!vcs.isEmpty()) {
            bib.vcs(Arrays.asList(vcs));
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

        final BlackDuckProperties blackDuckProperties;
        if (clientConf.info.blackDuckProperties.isRunChecks()) {
            blackDuckProperties = clientConf.info.blackDuckProperties.copyBlackDuckProperties();
        } else {
            blackDuckProperties = new BlackDuckProperties();
        }

        Governance governance = new Governance();
        governance.setBlackDuckProperties(blackDuckProperties);
        bib.governance(governance);

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

        for (Map.Entry<String, String> runParam : clientConf.info.getRunParameters().entrySet()) {
            MatrixParameter matrixParameter = new MatrixParameter(runParam.getKey(), runParam.getValue());
            bib.addRunParameters(matrixParameter);
        }

        if (clientConf.isIncludeEnvVars()) {
            Properties envProperties = new Properties();
            envProperties.putAll(clientConf.getAllProperties());
            envProperties = BuildInfoExtractorUtils.getEnvProperties(envProperties, clientConf.getLog());
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

    private ArtifactoryTask getBuildInfoTask(Project project) {
        Set<Task> tasks = project.getTasksByName(ArtifactoryTask.ARTIFACTORY_PUBLISH_TASK_NAME, false);
        if (tasks.isEmpty()) {
            return null;
        }
        ArtifactoryTask artifactoryTask = (ArtifactoryTask)tasks.iterator().next();
        if (taskDidWork(artifactoryTask)) {
            return artifactoryTask;
        }
        return null;
    }

    /**
     * Determines if the task actually did any work.
     * This methods wraps Gradle's task.getState().getDidWork().
     * @param task  The ArtifactoryTask
     * @return      true if the task actually did any work.
     */
    private boolean taskDidWork(ArtifactoryTask task) {
        try {
            return task.getState().getDidWork();
        } catch (NoSuchMethodError error) {
            // Compatibility with older versions of Gradle:
            try {
                Method m = task.getClass().getMethod("getState");
                TaskState state = (TaskState)m.invoke(task);
                return state.getDidWork();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Module extractModule(Project project) {
        String artifactName = project.getName();
        ArtifactoryTask task = getBuildInfoTask(project);
        if (task != null) {
            artifactName = project.getName();
        }
        ModuleBuilder builder = new ModuleBuilder()
                .id(getModuleIdString(project.getGroup().toString(),
                        artifactName, project.getVersion().toString()));
        try {
            ArtifactoryClientConfiguration.PublisherHandler publisher = ArtifactoryPluginUtil.getPublisherHandler(project);
            if (publisher != null) {
                boolean excludeArtifactsFromBuild = publisher.isFilterExcludedArtifactsFromBuild();
                IncludeExcludePatterns patterns = new IncludeExcludePatterns(
                        publisher.getIncludePatterns(),
                        publisher.getExcludePatterns());
                Iterable<GradleDeployDetails> deployExcludeDetails = null;
                Iterable<GradleDeployDetails> deployIncludeDetails = null;
                if (excludeArtifactsFromBuild) {
                    deployIncludeDetails = Iterables.filter(gradleDeployDetails, new IncludeExcludePredicate(project, patterns, true));
                    deployExcludeDetails = Iterables.filter(gradleDeployDetails, new IncludeExcludePredicate(project, patterns, false));
                } else {
                    deployIncludeDetails = Iterables.filter(gradleDeployDetails, new ProjectPredicate(project));
                    deployExcludeDetails = new ArrayList<GradleDeployDetails>();
                }
                builder.artifacts(calculateArtifacts(deployIncludeDetails))
                        .excludedArtifacts(calculateArtifacts(deployExcludeDetails))
                        .dependencies(calculateDependencies(project));
            } else {
                log.warn("No publisher config found for project: " + project.getName());
            }
        } catch (Exception e) {
            log.error("Error during extraction: ", e);
        }
        return builder.build();
    }

    private List<Artifact> calculateArtifacts(Iterable<GradleDeployDetails> deployDetails) throws Exception {
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
                        Set<String> existingScopes = existingDependency.getScopes();
                        existingScopes.add(configuration.getName());
                        existingDependency.setScopes(existingScopes);
                    } else {
                        DependencyBuilder dependencyBuilder = new DependencyBuilder()
                                .type(getTypeString(artifact.getType(),
                                        artifact.getClassifier(), artifact.getExtension()))
                                .id(depId)
                                .scopes(Sets.newHashSet(configuration.getName()));
                        if (file.isFile()) {
                            // In recent gradle builds (3.4+) subproject dependencies are represented by a dir not jar.
                            Map<String, String> checksums = FileChecksumCalculator.calculateChecksums(file, MD5, SHA1);
                            dependencyBuilder.md5(checksums.get(MD5)).sha1(checksums.get(SHA1));
                        }
                        dependencies.add(dependencyBuilder.build());
                    }
                }
            }
        }
        return dependencies;
    }

    private class ProjectPredicate implements Predicate<GradleDeployDetails> {
        private final Project project;

        private ProjectPredicate(Project project) {
            this.project = project;
        }

        public boolean apply(@Nullable GradleDeployDetails input) {
            return input.getProject().equals(project);
        }
    }

    private class IncludeExcludePredicate implements Predicate<GradleDeployDetails> {
        private Project project;
        private IncludeExcludePatterns patterns;
        private boolean include;

        public IncludeExcludePredicate(Project project, IncludeExcludePatterns patterns, boolean isInclude) {
            this.project = project;
            this.patterns = patterns;
            include = isInclude;
        }

        public boolean apply(@Nullable GradleDeployDetails input) {
            if (include) {
                return input.getProject().equals(project) && !PatternMatcher.pathConflicts(input.getDeployDetails().getArtifactPath(), patterns);
            } else {
                return input.getProject().equals(project) && PatternMatcher.pathConflicts(input.getDeployDetails().getArtifactPath(), patterns);
            }
        }
    }
}