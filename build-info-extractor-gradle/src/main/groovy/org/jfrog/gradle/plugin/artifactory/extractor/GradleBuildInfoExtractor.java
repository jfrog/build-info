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

import org.apache.commons.lang.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.jfrog.build.api.*;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.api.builder.PromotionStatusBuilder;
import org.jfrog.build.api.release.Promotion;
import org.jfrog.build.extractor.BuildInfoExtractor;
import org.jfrog.build.extractor.ModuleExtractorUtils;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.packageManager.PackageManagerUtils;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * An upload task uploads files to the repositories assigned to it.  The files that get uploaded are the artifacts of
 * your project, if they belong to the configuration associated with the upload task.
 *
 * @author Tomer Cohen
 */
public class GradleBuildInfoExtractor implements BuildInfoExtractor<Project> {
    private static final Logger log = Logging.getLogger(GradleBuildInfoExtractor.class);

    private final ArtifactoryClientConfiguration clientConf;
    private final List<ModuleInfoFileProducer> moduleInfoFileProducers;

    public GradleBuildInfoExtractor(ArtifactoryClientConfiguration clientConf, List<ModuleInfoFileProducer> moduleInfoFileProducers) {
        this.clientConf = clientConf;
        this.moduleInfoFileProducers = moduleInfoFileProducers;
    }

    @Override
    public Build extract(Project rootProject) {
        String buildName = clientConf.info.getBuildName();
        BuildInfoBuilder bib = new BuildInfoBuilder(buildName);

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

        Set<File> moduleFilesWithModules = moduleInfoFileProducers.stream()
                .filter(ModuleInfoFileProducer::hasModules)
                .flatMap(moduleInfoFileProducer -> moduleInfoFileProducer.getModuleInfoFiles().getFiles().stream())
                .collect(Collectors.toSet());

        moduleFilesWithModules.forEach(moduleFile -> {
            try {
                Module module = ModuleExtractorUtils.readModuleFromFile(moduleFile);
                List<Artifact> artifacts = module.getArtifacts();
                List<Dependency> dependencies = module.getDependencies();
                if ((artifacts != null && !artifacts.isEmpty()) || (dependencies != null && !dependencies.isEmpty())) {
                    bib.addModule(module);
                }
            } catch (IOException e) {
                throw new RuntimeException("Cannot load module info from file: " + moduleFile.getAbsolutePath(), e);
            }
        });

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
        if (StringUtils.isBlank(artifactoryPluginVersion)) {
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

        log.debug("buildInfoBuilder = " + bib);
        // for backward compatibility for Artifactory 2.2.3
        Build build = bib.build();
        if (parentName != null && parentNumber != null) {
            build.setParentBuildId(parentName);
        }
        PackageManagerUtils.collectEnvIfNeeded(clientConf, build);
        return build;
    }
}
