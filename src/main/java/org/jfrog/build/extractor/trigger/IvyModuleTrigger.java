package org.jfrog.build.extractor.trigger;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.ivy.ant.IvyTask;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.event.IvyEvent;
import org.apache.ivy.plugins.trigger.AbstractTrigger;
import org.apache.tools.ant.Project;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.context.BuildContext;
import org.jfrog.build.util.IvyResolverHelper;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * This trigger is fired when a {@code pre-publish-artifact} has occurred. Allowing to get module data as to the
 * artifacts that will be published.
 *
 * @author Tomer Cohen
 */
public class IvyModuleTrigger extends AbstractTrigger {

    private static List<Module> allModules = Lists.newArrayList();

    public void progress(IvyEvent event) {
        Project project = (Project) IvyContext.peekInContextStack(IvyTask.ANT_PROJECT_CONTEXT_KEY);
        project.log("Collecting Module information.", Project.MSG_INFO);
        Map<String, String> map = event.getAttributes();
        final String moduleName = map.get("module");
        List<Module> modules = IvyDependencyTrigger.getModules();
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
        BuildContext ctx = (BuildContext) IvyContext.getContext().get(BuildContext.CONTEXT_NAME);
        DeployDetails.Builder builder = new DeployDetails.Builder().file(artifactFile).sha1(sha1).md5(md5);
        String revision = map.get("revision");
        String artifactPath =
                IvyResolverHelper.calculateArtifactPath(artifactFile, organization, moduleName, revision);
        builder.artifactPath(artifactPath);
        String targetRepository = IvyResolverHelper.getTargetRepository();
        builder.targetRepository(targetRepository);
        DeployDetails deployDetails = builder.build();
        ctx.addDeployDetailsForModule(deployDetails);
        if (allModules.indexOf(module) == -1) {
            allModules.add(module);
        }
    }

    public static List<Module> getAllModules() {
        return allModules;
    }
}
