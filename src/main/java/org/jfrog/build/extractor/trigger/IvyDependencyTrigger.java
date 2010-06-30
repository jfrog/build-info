package org.jfrog.build.extractor.trigger;

import com.google.common.collect.Lists;
import org.apache.ivy.core.event.IvyEvent;
import org.apache.ivy.core.event.resolve.EndResolveEvent;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ConfigurationResolveReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.plugins.trigger.AbstractTrigger;
import org.apache.ivy.plugins.trigger.Trigger;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.util.FileChecksumCalculator;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * This trigger is fired after a successful {@code post-resolve} event. After which the event gives a list of
 * dependencies via the {@link ResolveReport} with file locations and configurations.
 *
 * @author Tomer Cohen
 */
public class IvyDependencyTrigger extends AbstractTrigger implements Trigger {

    private static List<Module> modules;

    public IvyDependencyTrigger() {
        modules = Lists.newArrayList();
    }

    public void progress(IvyEvent event) {
        ResolveReport report = ((EndResolveEvent) event).getReport();
        Map<String, String> attributes = event.getAttributes();
        String moduleName = attributes.get("module");
        ModuleBuilder moduleBuilder = new ModuleBuilder().id(moduleName);
        String[] configurations = report.getConfigurations();
        List<Dependency> moduleDependencies = Lists.newArrayList();
        for (String configuration : configurations) {
            ConfigurationResolveReport configurationReport = report.getConfigurationReport(configuration);
            ArtifactDownloadReport[] allArtifactsReports = configurationReport.getAllArtifactsReports();
            for (ArtifactDownloadReport artifactsReport : allArtifactsReports) {
                DependencyBuilder dependencyBuilder = new DependencyBuilder();
                dependencyBuilder.type(artifactsReport.getType()).id(artifactsReport.getName())
                        .scopes(Arrays.asList(configurations));
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
            }

        }
        moduleBuilder.dependencies(moduleDependencies);
        modules.add(moduleBuilder.build());

    }

    public static List<Module> getModules() {
        return modules;
    }
}
