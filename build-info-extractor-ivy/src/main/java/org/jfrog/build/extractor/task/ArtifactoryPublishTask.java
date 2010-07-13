package org.jfrog.build.extractor.task;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.ivy.Ivy;
import org.apache.ivy.ant.IvyPublish;
import org.apache.ivy.ant.IvyTask;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.context.BuildContext;
import org.jfrog.build.extractor.trigger.IvyDependencyTrigger;
import org.jfrog.build.util.IvyArtifactHelper;
import org.jfrog.build.util.IvyResolverHelper;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * A customized Ivy task that is used to configure a custom resolver which wraps around the existing resolver. It
 * extends the {@link IvyPublish} so that the artifacts will not be automatically published, but rather we have more
 * control on artifact deployment.
 *
 * @author Tomer Cohen
 */
public class ArtifactoryPublishTask extends IvyPublish {

    @Override
    public void doExecute() throws BuildException {
        IvySettings ivySettings = getSettings();
        Project project = (Project) IvyContext.peekInContextStack(IvyTask.ANT_PROJECT_CONTEXT_KEY);
        project.log("Collecting Module information.", Project.MSG_INFO);
        final String moduleName = getProperty(ivySettings, "ivy.module");
        List<Module> modules = IvyDependencyTrigger.getModules();
        Module module = Iterables.find(modules, new Predicate<Module>() {
            public boolean apply(Module input) {
                return input.getId().equals(moduleName);
            }
        });
        String organization = getProperty(ivySettings, "ivy.organisation");
        String revision = getProperty(ivySettings, "ivy.revision");


        if ("working".equals(revision)) {
            revision = Ivy.getWorkingRevision();
        }
        String pubRevision = revision;
        Date pubdate = getPubDate("now", new Date());
        if (pubRevision == null) {
            if (revision.startsWith("working@")) {
                pubRevision = Ivy.DATE_FORMAT.format(pubdate);
            }
        }
        Artifact artifact = IvyArtifactHelper
                .calculateIvyArtifact(moduleName, organization, pubRevision, getSrcivypattern());
        File ivyFile = getProject().resolveFile(IvyPatternHelper.substitute(
                getSrcivypattern(), organization, moduleName, pubRevision, "ivy", "ivy", "xml"));
        List<Artifact> artifacts = module.getArtifacts();
        if (artifacts == null) {
            module.setArtifacts(Lists.<Artifact>newArrayList());
        }
        module.getArtifacts().add(artifact);
        PropertyHelper propertyHelper = PropertyHelper.getPropertyHelper(project);
        BuildContext ctx = (BuildContext) propertyHelper.getUserProperty(BuildContext.CONTEXT_NAME);
        DeployDetails.Builder builder =
                new DeployDetails.Builder().file(ivyFile).sha1(artifact.getSha1()).md5(artifact.getMd5());
        String artifactPath =
                IvyResolverHelper.calculateArtifactPath(ivyFile, organization, moduleName, revision);
        builder.artifactPath(artifactPath);
        String targetRepository = IvyResolverHelper.getTargetRepository();
        builder.targetRepository(targetRepository);
        DeployDetails deployDetails = builder.build();
        ctx.addDeployDetailsForModule(deployDetails);

        List<File> artifactFiles;
        try {
            artifactFiles = IvyArtifactHelper.calculateArtifactFilesFromIvy(ivySettings, ivyFile, getSrcivypattern());
            for (File file : artifactFiles) {
                if (file.exists()) {
                    ArtifactBuilder artifactBuilder = new ArtifactBuilder(file.getName());
                    Map<String, String> checksums = FileChecksumCalculator.calculateChecksums(file, "MD5", "SHA1");
                    String sha1 = checksums.get("SHA1");
                    String md5 = checksums.get("MD5");
                    artifactBuilder.type("jar").sha1(sha1).md5(md5);
                    module.getArtifacts().add(artifactBuilder.build());
                    String path = IvyResolverHelper.calculateArtifactPath(file, organization, moduleName, revision);
                    builder = new DeployDetails.Builder().file(file).artifactPath(path).sha1(sha1).md5(md5);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
