package org.jfrog.gradle.plugin.artifactory.extractor;

import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.jfrog.build.api.builder.PromotionStatusBuilder;
import org.jfrog.build.extractor.builder.BuildInfoBuilder;
import org.jfrog.build.extractor.ci.Agent;
import org.jfrog.build.extractor.ci.Artifact;
import org.jfrog.build.extractor.ci.BuildAgent;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.extractor.ci.Dependency;
import org.jfrog.build.extractor.ci.Issue;
import org.jfrog.build.extractor.ci.IssueTracker;
import org.jfrog.build.extractor.ci.Issues;
import org.jfrog.build.extractor.ci.MatrixParameter;
import org.jfrog.build.extractor.ci.Module;
import org.jfrog.build.extractor.ci.Vcs;
import org.jfrog.build.api.release.Promotion;
import org.jfrog.build.extractor.BuildInfoExtractor;
import org.jfrog.build.extractor.ModuleExtractorUtils;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.packageManager.PackageManagerUtils;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    public BuildInfo extract(Project rootProject) {
        String buildName = clientConf.info.getBuildName();
        BuildInfoBuilder bib = new BuildInfoBuilder(buildName);

        String buildNumber = clientConf.info.getBuildNumber();
        bib.number(buildNumber);

        String buildProject = clientConf.info.getProject();
        bib.project(buildProject);

        String buildStartedIso = clientConf.info.getBuildStarted();
        Date buildStartDate = null;
        try {
            buildStartDate = new SimpleDateFormat(BuildInfo.STARTED_FORMAT).parse(buildStartedIso);
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
        if (StringUtils.isNotBlank(vcsRevision)) {
            bib.vcsRevision(vcsRevision);
        }

        String vcsUrl = clientConf.info.getVcsUrl();
        if (StringUtils.isNotBlank(vcsUrl)) {
            bib.vcsUrl(vcsUrl);
        }
        Vcs vcs = new Vcs(vcsUrl, vcsRevision, clientConf.info.getVcsBranch(), clientConf.info.getVcsMessage());
        if (!vcs.isEmpty()) {
            bib.vcs(Arrays.asList(vcs));
        }

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

        BuildInfo buildInfo = bib.build();
        PackageManagerUtils.collectAndFilterEnvIfNeeded(clientConf, buildInfo);
        log.debug("buildInfoBuilder = " + buildInfo);

        return buildInfo;
    }
}
