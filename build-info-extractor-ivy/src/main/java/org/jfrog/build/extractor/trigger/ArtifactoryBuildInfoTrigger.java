package org.jfrog.build.extractor.trigger;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.ivy.ant.IvyTask;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.event.IvyEvent;
import org.apache.ivy.core.event.publish.StartArtifactPublishEvent;
import org.apache.ivy.core.event.resolve.EndResolveEvent;
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
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.context.BuildContext;
import org.jfrog.build.util.IvyResolverHelper;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

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
        String moduleName = attributes.get("module");
        ModuleBuilder moduleBuilder = new ModuleBuilder().id(moduleName);
        String[] configurations = report.getConfigurations();
        List<Dependency> moduleDependencies = Lists.newArrayList();
        for (String configuration : configurations) {
            project.log("Configuration: " + configuration + " Dependencies", Project.MSG_INFO);
            ConfigurationResolveReport configurationReport = report.getConfigurationReport(configuration);
            ArtifactDownloadReport[] allArtifactsReports = configurationReport.getAllArtifactsReports();
            for (final ArtifactDownloadReport artifactsReport : allArtifactsReports) {
                project.log("Artifact Download Report for configuration: " + configuration + " : " + artifactsReport,
                        Project.MSG_INFO);
                Dependency dependency = findDependencyInList(artifactsReport, moduleDependencies);
                if (dependency == null) {
                    DependencyBuilder dependencyBuilder = new DependencyBuilder();
                    dependencyBuilder.type(artifactsReport.getType()).id(artifactsReport.getName())
                            .scopes(Lists.newArrayList(configuration));
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
        BuildContext ctx = (BuildContext) IvyContext.getContext().get(BuildContext.CONTEXT_NAME);
        List<Module> modules = ctx.getModules();
        modules.add(moduleBuilder.build());
    }

    /**
     * Collect module information for each module.
     *
     * @param event
     */
    private void collectModuleInformation(IvyEvent event) {
        Project project = (Project) IvyContext.peekInContextStack(IvyTask.ANT_PROJECT_CONTEXT_KEY);
        project.log("Collecting Module information.", Project.MSG_INFO);
        Map<String, String> map = event.getAttributes();
        final String moduleName = map.get("module");
        BuildContext ctx = (BuildContext) IvyContext.getContext().get(BuildContext.CONTEXT_NAME);
        List<Module> modules = ctx.getModules();
        Module module = Iterables.find(modules, new Predicate<Module>() {
            public boolean apply(Module input) {
                return input.getId().equals(moduleName);
            }
        });
        String file = map.get("file");
        File artifactFile = new File(file);
        String organization = map.get("organisation");
        String path = artifactFile.getAbsolutePath();
        project.log("Module location: " + path, Project.MSG_INFO);
        ArtifactBuilder artifactBuilder = new ArtifactBuilder(artifactFile.getName());
        artifactBuilder.type(map.get("type"));
        Map<String, String> checksums;
        try {
            checksums = FileChecksumCalculator.calculateChecksums(artifactFile, "MD5", "SHA1");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String md5 = checksums.get("MD5");
        String sha1 = checksums.get("SHA1");
        artifactBuilder.md5(md5).sha1(sha1);
        List<Artifact> artifacts = module.getArtifacts();
        if (artifacts == null) {
            module.setArtifacts(Lists.<Artifact>newArrayList());
        }
        module.getArtifacts().add(artifactBuilder.build());
        DeployDetails.Builder builder = new DeployDetails.Builder().file(artifactFile).sha1(sha1).md5(md5);
        String revision = map.get("revision");
        String artifactPath =
                IvyResolverHelper.calculateArtifactPath(artifactFile, organization, moduleName, revision);
        builder.artifactPath(artifactPath);
        String targetRepository = IvyResolverHelper.getTargetRepository();
        builder.targetRepository(targetRepository);
        String svnRevision = System.getenv("SVN_REVISION");
        if (StringUtils.isNotBlank(svnRevision)) {
            builder.addProperty(
                    StringUtils.removeStart(BuildInfoProperties.PROP_VCS_REVISION,
                            BuildInfoProperties.BUILD_INFO_PREFIX),
                    svnRevision);
        }
        String buildName = System.getenv(BuildInfoProperties.PROP_BUILD_NAME);
        if (StringUtils.isNotBlank(buildName)) {
            builder.addProperty(
                    StringUtils.removeStart(BuildInfoProperties.PROP_BUILD_NAME, BuildInfoProperties.BUILD_INFO_PREFIX),
                    buildName);
        }
        String buildNumber = System.getenv(BuildInfoProperties.PROP_BUILD_NUMBER);
        if (StringUtils.isNotBlank(buildNumber)) {
            builder.addProperty(
                    StringUtils.removeStart(BuildInfoProperties.PROP_BUILD_NUMBER,
                            BuildInfoProperties.BUILD_INFO_PREFIX),
                    buildNumber);
        }
        DeployDetails deployDetails = builder.build();
        ctx.addDeployDetailsForModule(deployDetails);
        List<Module> contextModules = ctx.getModules();
        if (contextModules.indexOf(module) == -1) {
            ctx.addModule(module);
        }
    }

    private Dependency findDependencyInList(final ArtifactDownloadReport artifactsReport,
            List<Dependency> moduleDependencies) {
        try {
            Dependency dependency = Iterables.find(moduleDependencies, new Predicate<Dependency>() {
                public boolean apply(Dependency input) {
                    return input.getId().equals(artifactsReport.getName());
                }
            });
            return dependency;
        } catch (NoSuchElementException e) {
            return null;
        }
    }
}
