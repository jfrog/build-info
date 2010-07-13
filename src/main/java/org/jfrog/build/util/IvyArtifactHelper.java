package org.jfrog.build.util;

import org.apache.ivy.ant.IvyPublish;
import org.apache.ivy.ant.IvyTask;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParser;
import org.apache.tools.ant.Project;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.util.FileChecksumCalculator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Helper class to deal with Ivy artifacts, such as calculating ivy artifacts from Ivy descriptor, as well as from the
 * project's context.
 *
 * @author Tomer Cohen
 */
public class IvyArtifactHelper {

    /**
     * Calculate the Ivy artifact {@code ivy.xml} from the project's context, this method resolves the file by the
     * module's name, organization, and pubRevision, and then uses the srcIvyPattern the find the file itself, allowing
     * for checksum calculation.
     *
     * @param moduleName    The Ivy module's name
     * @param organization  The Ivy module's organization.
     * @param pubRevision   The Ivy module's publication revision.
     * @param srcIvyPattern the srcIvyPattern as is given by the resolver as is shown in the {@link IvyPublish} task.
     * @return An artifact object representing the ivy.xml descriptor.
     */
    public static Artifact calculateIvyArtifact(String moduleName, String organization, String pubRevision,
            String srcIvyPattern) {
        Project project = (Project) IvyContext.peekInContextStack(IvyTask.ANT_PROJECT_CONTEXT_KEY);
        project.log("Collecting Module information.", Project.MSG_INFO);
        File ivyFile = project.resolveFile(IvyPatternHelper.substitute(
                srcIvyPattern, organization, moduleName, pubRevision, "ivy", "ivy", "xml"));
        ArtifactBuilder artifactBuilder = new ArtifactBuilder(ivyFile.getName());
        artifactBuilder.type("ivy");
        Map<String, String> checksums;
        try {
            checksums = FileChecksumCalculator.calculateChecksums(ivyFile, "MD5", "SHA1");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String md5 = checksums.get("MD5");
        String sha1 = checksums.get("SHA1");
        artifactBuilder.md5(md5).sha1(sha1);
        return artifactBuilder.build();
    }

    /**
     * Get all the to-be-published artifacts that go along with the {@code ivy.xml}
     *
     * @param settings   The ivy settings.
     * @param ivyFile    The ivy.xml file
     * @param srcPattern The src pattern of the file.
     * @return A list of files to be deployed
     */
    public static List<File> calculateArtifactFilesFromIvy(IvySettings settings, File ivyFile, String srcPattern)
            throws Exception {
        List<File> files = new ArrayList<File>();
        Set<org.apache.ivy.core.module.descriptor.Artifact> ivyArtifacts =
                new HashSet<org.apache.ivy.core.module.descriptor.Artifact>();
        ModuleDescriptor md =
                XmlModuleDescriptorParser.getInstance().parseDescriptor(settings, ivyFile.toURI().toURL(), false);
        String[] confs = md.getConfigurationsNames();
        for (String conf : confs) {
            org.apache.ivy.core.module.descriptor.Artifact[] artifacts = md.getArtifacts(conf);
            ivyArtifacts.addAll(Arrays.asList(artifacts));
        }
        for (org.apache.ivy.core.module.descriptor.Artifact ivyArtifact : ivyArtifacts) {
            File artifactFile = settings.resolveFile(
                    IvyPatternHelper.substitute(settings.substitute(srcPattern), ivyArtifact));
            files.add(artifactFile);
        }
        return files;
    }
}
