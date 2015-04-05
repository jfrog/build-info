package org.jfrog.build.extractor.trigger;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.ivy.ant.IvyTask;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.event.IvyEvent;
import org.apache.ivy.core.event.IvyEventFilter;
import org.apache.ivy.core.event.publish.EndArtifactPublishEvent;
import org.apache.ivy.core.event.publish.PublishEvent;
import org.apache.ivy.core.event.resolve.EndResolveEvent;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ConfigurationResolveReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.plugins.matcher.ExactPatternMatcher;
import org.apache.ivy.plugins.trigger.Trigger;
import org.apache.ivy.util.filter.Filter;
import org.apache.tools.ant.Project;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.context.BuildContext;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.IncludeExcludePatterns;
import org.jfrog.build.extractor.clientConfiguration.PatternMatcher;
import org.jfrog.build.util.IvyResolverHelper;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.jfrog.build.extractor.BuildInfoExtractorUtils.getModuleIdString;
import static org.jfrog.build.extractor.BuildInfoExtractorUtils.getTypeString;

/**
 * This trigger is fired after a successful {@code post-resolve} event. After which the event gives a list of
 * dependencies via the {@link ResolveReport} with file locations and configurations.
 *
 * @author Tomer Cohen
 */
public class ArtifactoryBuildInfoTrigger implements Trigger {

    private static final String MD5 = "MD5";
    private static final String SHA1 = "SHA1";
    private final Filter filter;
    private BuildContext ctx;
    private String eventName;

    public ArtifactoryBuildInfoTrigger(String eventName) {
        this.eventName = eventName;
        this.filter = new IvyEventFilter(eventName, null, ExactPatternMatcher.INSTANCE);
    }

    public void setIvyBuildContext(BuildContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Filter getEventFilter() {
        return filter;
    }

    public void progress(IvyEvent event) {
        try {
            Project project = (Project) IvyContext.peekInContextStack(IvyTask.ANT_PROJECT_CONTEXT_KEY);
            if (project != null) {
                project.log("[buildinfo:collect] Received Event: " + event.getName(), Project.MSG_DEBUG);
            }
            if (EndResolveEvent.NAME.equals(event.getName())) {
                collectDependencyInformation(event);
            } else if (EndArtifactPublishEvent.NAME.equals(event.getName())) {
                collectModuleInformation(event);
            }
        } catch (Exception e) {
            RuntimeException re = new RuntimeException("Fail to collect dependencies and modules using the progress trigger in the Artifactory Ivy plugin, due to: " + e.getMessage(), e);
            re.printStackTrace();
            throw re;
        }
    }

    /**
     * Collect dependency information during the build.
     *
     * @param event The end of resolution Ivy event
     */
    private void collectDependencyInformation(IvyEvent event) {
        Project project = (Project) IvyContext.peekInContextStack(IvyTask.ANT_PROJECT_CONTEXT_KEY);
        ResolveReport report = ((EndResolveEvent) event).getReport();
        @SuppressWarnings("unchecked") Map<String, String> attributes = event.getAttributes();
        Module module = getOrCreateModule(attributes);
        project.log("[buildinfo:collect] Collecting dependencies for " + module.getId(), Project.MSG_INFO);
        if (module.getDependencies() == null || module.getDependencies().isEmpty()) {
            String[] configurations = report.getConfigurations();
            List<Dependency> moduleDependencies = Lists.newArrayList();
            for (String configuration : configurations) {
                project.log("[buildinfo:collect] Configuration: " + configuration + " Dependencies", Project.MSG_DEBUG);
                ConfigurationResolveReport configurationReport = report.getConfigurationReport(configuration);
                ArtifactDownloadReport[] allArtifactsReports = configurationReport.getAllArtifactsReports();
                for (final ArtifactDownloadReport artifactsReport : allArtifactsReports) {
                    project.log(
                            "[buildinfo:collect] Artifact Download Report for configuration: " + configuration + " : " + artifactsReport,
                            Project.MSG_DEBUG);
                    ModuleRevisionId id = artifactsReport.getArtifact().getModuleRevisionId();
                    String type = getType(artifactsReport.getArtifact());
                    Dependency dependency = findDependencyInList(id, type, moduleDependencies);
                    if (dependency == null) {
                        DependencyBuilder dependencyBuilder = new DependencyBuilder();
                        dependencyBuilder.type(type).scopes(Sets.newHashSet(configuration));
                        String idString = getModuleIdString(id.getOrganisation(),
                                id.getName(), id.getRevision());
                        dependencyBuilder.id(idString);
                        File file = artifactsReport.getLocalFile();
                        Map<String, String> checksums;
                        try {
                            checksums = FileChecksumCalculator.calculateChecksums(file, MD5, SHA1);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        String md5 = checksums.get(MD5);
                        String sha1 = checksums.get(SHA1);
                        dependencyBuilder.md5(md5).sha1(sha1);
                        dependency = dependencyBuilder.build();
                        moduleDependencies.add(dependency);
                        project.log(
                                "[buildinfo:collect] Added dependency '" + dependency.getId() + "'", Project.MSG_DEBUG);
                    } else {
                        if (!dependency.getScopes().contains(configuration)) {
                            dependency.getScopes().add(configuration);
                            project.log(
                                    "[buildinfo:collect] Added scope " + configuration +
                                            " to dependency '" + dependency.getId() + "'", Project.MSG_DEBUG);
                        } else {
                            project.log(
                                    "[buildinfo:collect] Find same dependency twice in configuration '" + configuration +
                                            "' for dependency '" + artifactsReport + "'", Project.MSG_WARN);
                        }
                    }
                }
            }
            module.setDependencies(moduleDependencies);
        }
    }

    /**
     * Collect module information for each module.
     *
     * @param event the Ivy publish event
     */
    private void collectModuleInformation(IvyEvent event) {
        ArtifactoryClientConfiguration.PublisherHandler publisher = ctx.getClientConf().publisher;
        IncludeExcludePatterns patterns = new IncludeExcludePatterns(
                publisher.getIncludePatterns(), publisher.getExcludePatterns());
        boolean excludeArtifactsFromBuild = publisher.isFilterExcludedArtifactsFromBuild();
        Project project = (Project) IvyContext.peekInContextStack(IvyTask.ANT_PROJECT_CONTEXT_KEY);

        // Finding module object from context
        @SuppressWarnings("unchecked") final Map<String, String> map = event.getAttributes();
        Module module = getOrCreateModule(map);
        List<Artifact> artifacts = module.getArtifacts();
        if (artifacts == null) {
            module.setArtifacts(Lists.<Artifact>newArrayList());
        }
        List<Artifact> excludedArtifacts = module.getExcludedArtifacts();
        if (excludedArtifacts == null) {
            module.setExcludedArtifacts(Lists.<Artifact>newArrayList());
        }

        final org.apache.ivy.core.module.descriptor.Artifact pubArtifact = ((PublishEvent) event).getArtifact();
        @SuppressWarnings("unchecked") Map<String, String> extraAttributes = pubArtifact.getExtraAttributes();
        // Using the original file, not the published one that can be far away (network wise)
        String file = map.get("file");
        // But all other attributes are taken from the actual published artifact
        final ModuleRevisionId mrid = pubArtifact.getModuleRevisionId();
        String moduleName = mrid.getName();
        String type = getType(pubArtifact);

        // By default simple name
        String name = pubArtifact.getName() + "-" + mrid.getRevision() + "." + pubArtifact.getExt();

        // Set name from name of published file
        String fullPath = IvyResolverHelper.calculateArtifactPath(publisher, map, extraAttributes);
        int lastSlash = fullPath.lastIndexOf('/');
        if (lastSlash > 0 && lastSlash + 1 < fullPath.length()) {
            name = fullPath.substring(lastSlash + 1);
        }
        project.log("[buildinfo:collect] Collecting artifact " + name + " for module " + moduleName +
                " using file " + file, Project.MSG_INFO);

        if (isArtifactExist(module.getArtifacts(), name) || isArtifactExist(module.getExcludedArtifacts(), name)) {
            return;
        }
        ArtifactBuilder artifactBuilder = new ArtifactBuilder(name);
        artifactBuilder.type(type);

        File artifactFile = new File(file);
        Map<String, String> checksums = calculateFileChecksum(artifactFile);
        String md5 = checksums.get(MD5);
        String sha1 = checksums.get(SHA1);
        artifactBuilder.md5(md5).sha1(sha1);
        Artifact artifact = artifactBuilder.build();
        if (excludeArtifactsFromBuild && PatternMatcher.pathConflicts(fullPath, patterns)) {
            module.getExcludedArtifacts().add(artifact);
        } else {
            module.getArtifacts().add(artifact);
        }
        @SuppressWarnings("unchecked") DeployDetails deployDetails =
                buildDeployDetails(artifactFile, artifact, ctx, map, extraAttributes);
        ctx.addDeployDetailsForModule(deployDetails);
        List<Module> contextModules = ctx.getModules();
        if (contextModules.indexOf(module) == -1) {
            ctx.addModule(module);
        }
    }

    private String getType(org.apache.ivy.core.module.descriptor.Artifact ivyArtifact) {
        return getTypeString(ivyArtifact.getType(),
                ivyArtifact.getExtraAttribute("classifier"),
                ivyArtifact.getExt());
    }

    private String getName(org.apache.ivy.core.module.descriptor.Artifact ivyArtifact) {
        return getTypeString(ivyArtifact.getType(),
                ivyArtifact.getExtraAttribute("classifier"),
                ivyArtifact.getExt());
    }

    private DeployDetails buildDeployDetails(File artifactFile, Artifact artifact,
                                             BuildContext ctx, Map<String, String> map, Map<String, String> extraAttributes) {
        ArtifactoryClientConfiguration clientConf = ctx.getClientConf();
        DeployDetails.Builder builder =
                new DeployDetails.Builder().file(artifactFile).sha1(artifact.getSha1()).md5(artifact.getMd5());
        builder.artifactPath(
                IvyResolverHelper.calculateArtifactPath(clientConf.publisher, map, extraAttributes));
        builder.targetRepository(clientConf.publisher.getRepoKey());
        if (StringUtils.isNotBlank(clientConf.info.getVcsRevision())) {
            builder.addProperty(BuildInfoFields.VCS_REVISION, clientConf.info.getVcsRevision());
        }
        if (StringUtils.isNotBlank(clientConf.info.getVcsUrl())) {
            builder.addProperty(BuildInfoFields.VCS_URL, clientConf.info.getVcsUrl());
        }
        if (StringUtils.isNotBlank(clientConf.info.getBuildName())) {
            builder.addProperty(BuildInfoFields.BUILD_NAME, clientConf.info.getBuildName());
        }
        if (StringUtils.isNotBlank(clientConf.info.getBuildNumber())) {
            builder.addProperty(BuildInfoFields.BUILD_NUMBER, clientConf.info.getBuildNumber());
        }
        String buildTimestamp = clientConf.info.getBuildTimestamp();
        if (StringUtils.isBlank(buildTimestamp)) {
            buildTimestamp = ctx.getBuildStartTime() + "";
        }
        builder.addProperty(BuildInfoFields.BUILD_TIMESTAMP, buildTimestamp);
        if (StringUtils.isNotBlank(clientConf.info.getParentBuildName())) {
            builder.addProperty(BuildInfoFields.BUILD_PARENT_NAME, clientConf.info.getParentBuildName());
        }
        if (StringUtils.isNotBlank(clientConf.info.getParentBuildNumber())) {
            builder.addProperty(BuildInfoFields.BUILD_PARENT_NUMBER, clientConf.info.getParentBuildNumber());
        }
        builder.addProperties(clientConf.publisher.getMatrixParams());
        return builder.build();
    }

    private Map<String, String> calculateFileChecksum(File file) {
        Map<String, String> checksums;
        try {
            checksums = FileChecksumCalculator.calculateChecksums(file, MD5, SHA1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return checksums;
    }

    private Dependency findDependencyInList(final ModuleRevisionId id, final String type, List<Dependency> moduleDependencies) {
        final String idToFind = getModuleIdString(id.getOrganisation(), id.getName(), "");
        return Iterables.find(moduleDependencies, new Predicate<Dependency>() {
            public boolean apply(Dependency input) {
                return input.getId().startsWith(idToFind) && input.getType().equals(type);
            }
        }, null);
    }

    private Module findModule(List<Module> modules, final String moduleKey) {
        return Iterables.find(modules, new Predicate<Module>() {
            public boolean apply(Module input) {
                return input.getId().startsWith(moduleKey);
            }
        }, null);
    }

    private Module getOrCreateModule(Map<String, String> attributes) {
        List<Module> modules = ctx.getModules();
        final String org = attributes.get("organisation");
        final String moduleName = attributes.get("module");
        String moduleKey = getModuleIdString(org, moduleName, "");
        String moduleId = getModuleIdString(org, moduleName, attributes.get("revision"));
        Module module = findModule(modules, moduleKey);
        if (module == null) {
            ModuleBuilder moduleBuilder = new ModuleBuilder().id(moduleId);
            module = moduleBuilder.build();
            modules.add(module);
        } else {
            module.setId(moduleId);
        }
        return module;
    }

    private boolean isArtifactExist(List<Artifact> artifacts, final String artifactName) {
        return Iterables.any(artifacts, new Predicate<Artifact>() {
            public boolean apply(Artifact input) {
                return input.getName().equals(artifactName);
            }
        });
    }
}

