package org.jfrog.build.extractor.trigger;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.ivy.ant.IvyTask;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.event.IvyEvent;
import org.apache.ivy.core.event.publish.StartArtifactPublishEvent;
import org.apache.ivy.core.event.resolve.EndResolveEvent;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ConfigurationResolveReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.plugins.trigger.AbstractTrigger;
import org.apache.tools.ant.Project;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.BuildInfoProperties;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.client.ClientProperties;
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.context.BuildContext;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.util.IvyResolverHelper;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;

/**
 * This trigger is fired after a successful {@code post-resolve} event. After which the event gives a list of
 * dependencies via the {@link ResolveReport} with file locations and configurations.
 *
 * @author Tomer Cohen
 */
public class ArtifactoryBuildInfoTrigger extends AbstractTrigger {

    public void progress(IvyEvent event) {
        if (EndResolveEvent.NAME.equals(event.getName())) {
            collectDependencyInformation(event);
        } else if (StartArtifactPublishEvent.NAME.equals(event.getName())) {
            collectModuleInformation(event);
        }
    }

    /**
     * Collect dependency information during the build.
     *
     * @param event
     */
    private void collectDependencyInformation(IvyEvent event) {
        Project project = (Project) IvyContext.peekInContextStack(IvyTask.ANT_PROJECT_CONTEXT_KEY);
        project.log("Collecting dependencies.", Project.MSG_INFO);
        ResolveReport report = ((EndResolveEvent) event).getReport();
        Map<String, String> attributes = event.getAttributes();
        BuildContext ctx = (BuildContext) IvyContext.getContext().get(BuildContext.CONTEXT_NAME);
        List<Module> modules = ctx.getModules();
        String moduleName = attributes.get("module");
        if (!isModuleExists(modules, moduleName)) {
            ModuleBuilder moduleBuilder = new ModuleBuilder().id(moduleName);
            String[] configurations = report.getConfigurations();
            List<Dependency> moduleDependencies = Lists.newArrayList();
            for (String configuration : configurations) {
                project.log("Configuration: " + configuration + " Dependencies", Project.MSG_INFO);
                ConfigurationResolveReport configurationReport = report.getConfigurationReport(configuration);
                ArtifactDownloadReport[] allArtifactsReports = configurationReport.getAllArtifactsReports();
                for (final ArtifactDownloadReport artifactsReport : allArtifactsReports) {
                    project.log(
                            "Artifact Download Report for configuration: " + configuration + " : " + artifactsReport,
                            Project.MSG_INFO);
                    ModuleRevisionId id = artifactsReport.getArtifact().getModuleRevisionId();
                    Dependency dependency = findDependencyInList(id, moduleDependencies);
                    if (dependency == null) {
                        DependencyBuilder dependencyBuilder = new DependencyBuilder();
                        dependencyBuilder.type(artifactsReport.getType()).scopes(Lists.newArrayList(configuration));

                        dependencyBuilder.id(id.getOrganisation() + ":" + id.getName() + ":" + id.getRevision());
                        File file = artifactsReport.getLocalFile();
                        Map<String, String> checksums;
                        try {
                            checksums = FileChecksumCalculator.calculateChecksums(file, "MD5", "SHA1");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        String md5 = checksums.get("MD5");
                        String sha1 = checksums.get("SHA1");
                        dependencyBuilder.md5(md5).sha1(sha1);
                        moduleDependencies.add(dependencyBuilder.build());
                    } else {
                        dependency.getScopes().add(configuration);
                    }
                }
            }
            moduleBuilder.dependencies(moduleDependencies);
            modules.add(moduleBuilder.build());
        }
    }

    /**
     * Collect module information for each module.
     *
     * @param event
     */
    private void collectModuleInformation(IvyEvent event) {
        Project project = (Project) IvyContext.peekInContextStack(IvyTask.ANT_PROJECT_CONTEXT_KEY);
        final Map<String, String> map = event.getAttributes();
        BuildContext ctx = (BuildContext) IvyContext.getContext().get(BuildContext.CONTEXT_NAME);
        List<Module> modules = ctx.getModules();
        String file = map.get("file");
        final String moduleName = map.get("module");
        project.log("Collecting Module information for module: " + moduleName, Project.MSG_INFO);
        Module module = Iterables.find(modules, new Predicate<Module>() {
            public boolean apply(Module input) {
                return input.getId().equals(moduleName) || input.getId().equals(generateModuleIdFromAttributes(map));
            }
        });
        module.setId(generateModuleIdFromAttributes(map));
        File artifactFile = new File(file);
        List<Artifact> artifacts = module.getArtifacts();
        if (artifacts == null) {
            module.setArtifacts(Lists.<Artifact>newArrayList());
        }
        if (isArtifactExist(module.getArtifacts(), artifactFile.getName())) {
            return;
        }
        String path = artifactFile.getAbsolutePath();
        project.log("Module location: " + path, Project.MSG_INFO);
        ArtifactBuilder artifactBuilder = new ArtifactBuilder(artifactFile.getName());
        String type = map.get("type");
        String classifier = IvyResolverHelper.getClassifier(artifactFile.getName());
        Map<String, String> extraAttributes = new HashMap<String, String>();
        if (StringUtils.isNotBlank(classifier)) {
            type = type + "-" + classifier;
            extraAttributes.put("classifier", classifier);
        }
        artifactBuilder.type(type);
        Map<String, String> checksums;
        try {
            checksums = FileChecksumCalculator.calculateChecksums(artifactFile, "MD5", "SHA1");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String md5 = checksums.get("MD5");
        String sha1 = checksums.get("SHA1");
        artifactBuilder.md5(md5).sha1(sha1);

        module.getArtifacts().add(artifactBuilder.build());
        DeployDetails.Builder builder = new DeployDetails.Builder().file(artifactFile).sha1(sha1).md5(md5);
        Properties props = getMergedEnvAndSystemProps();
        String artifactPath = IvyResolverHelper.calculateArtifactPath(props, map, extraAttributes);
        builder.artifactPath(artifactPath);
        String targetRepository = IvyResolverHelper.getTargetRepository(props);
        builder.targetRepository(targetRepository);
        String svnRevision = props.getProperty("SVN_REVISION");
        if (StringUtils.isNotBlank(svnRevision)) {
            builder.addProperty(
                    StringUtils.removeStart(BuildInfoProperties.PROP_VCS_REVISION,
                            BuildInfoProperties.BUILD_INFO_PREFIX), svnRevision);
        }
        String buildName = props.getProperty(BuildInfoProperties.PROP_BUILD_NAME);
        if (StringUtils.isNotBlank(buildName)) {
            builder.addProperty(
                    StringUtils.removeStart(BuildInfoProperties.PROP_BUILD_NAME, BuildInfoProperties.BUILD_INFO_PREFIX),
                    buildName);
        }
        String buildNumber = props.getProperty(BuildInfoProperties.PROP_BUILD_NUMBER);
        if (StringUtils.isNotBlank(buildNumber)) {
            builder.addProperty(
                    StringUtils.removeStart(BuildInfoProperties.PROP_BUILD_NUMBER,
                            BuildInfoProperties.BUILD_INFO_PREFIX), buildNumber);
        }
        String buildTimestamp = props.getProperty(BuildInfoProperties.PROP_BUILD_TIMESTAMP);
        if (StringUtils.isBlank(buildTimestamp)) {
            buildTimestamp = ctx.getBuildStartTime() + "";
        }
        builder.addProperty(StringUtils.removeStart(BuildInfoProperties.PROP_BUILD_TIMESTAMP,
                BuildInfoProperties.BUILD_INFO_PREFIX), buildTimestamp);

        if (StringUtils.isNotBlank(buildNumber)) {
            builder.addProperty(
                    StringUtils.removeStart(BuildInfoProperties.PROP_BUILD_NUMBER,
                            BuildInfoProperties.BUILD_INFO_PREFIX), buildNumber);
        }
        String parentBuildName = props.getProperty(BuildInfoProperties.PROP_PARENT_BUILD_NAME);
        if (StringUtils.isNotBlank(parentBuildName)) {
            builder.addProperty(StringUtils.removeStart(BuildInfoProperties.PROP_PARENT_BUILD_NAME,
                    BuildInfoProperties.BUILD_INFO_PREFIX), parentBuildName);
        }
        String parentBuildNumber = props.getProperty(BuildInfoProperties.PROP_PARENT_BUILD_NUMBER);
        if (StringUtils.isNotBlank(parentBuildNumber)) {
            builder.addProperty(StringUtils.removeStart(BuildInfoProperties.PROP_PARENT_BUILD_NUMBER,
                    BuildInfoProperties.BUILD_INFO_PREFIX), parentBuildNumber);
        }
        Properties matrixParams =
                BuildInfoExtractorUtils.filterDynamicProperties(props, BuildInfoExtractorUtils.MATRIX_PARAM_PREDICATE);
        matrixParams = BuildInfoExtractorUtils
                .stripPrefixFromProperties(matrixParams, ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX);
        ImmutableMap<String, String> propertiesToAdd = Maps.fromProperties(matrixParams);
        builder.addProperties(propertiesToAdd);
        DeployDetails deployDetails = builder.build();
        ctx.addDeployDetailsForModule(deployDetails);
        List<Module> contextModules = ctx.getModules();
        if (contextModules.indexOf(module) == -1) {
            ctx.addModule(module);
        }
    }

    private String generateModuleIdFromAttributes(Map<String, String> attributes) {
        StringBuilder builder = new StringBuilder();
        builder.append(attributes.get("organisation")).append(":").append(attributes.get("module"))
                .append(":").append(attributes.get("revision"));
        return builder.toString();
    }

    private Dependency findDependencyInList(final ModuleRevisionId id, List<Dependency> moduleDependencies) {
        try {
            Dependency dependency = Iterables.find(moduleDependencies, new Predicate<Dependency>() {
                public boolean apply(Dependency input) {
                    return input.getId().equals(id.getOrganisation() + ":" + id.getName() + ":" + id.getRevision());
                }
            });
            return dependency;
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    private Properties getMergedEnvAndSystemProps() {
        Properties props = new Properties();
        props.putAll(System.getenv());
        return BuildInfoExtractorUtils.mergePropertiesWithSystemAndPropertyFile(props);
    }

    private boolean isModuleExists(List<Module> modules, final String moduleName) {
        try {
            Iterables.find(modules, new Predicate<Module>() {
                public boolean apply(Module input) {
                    return input.getId().equals(moduleName);
                }
            });
        } catch (NoSuchElementException e) {
            return false;
        }
        return true;
    }

    private boolean isArtifactExist(List<Artifact> artifacts, final String artifactName) {
        try {
            Iterables.find(artifacts, new Predicate<Artifact>() {
                public boolean apply(Artifact input) {
                    return input.getName().equals(artifactName);
                }
            });
        } catch (NoSuchElementException e) {
            return false;
        }
        return true;
    }
}

