package org.jfrog.build.extractor.trigger;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.apache.ivy.ant.IvyTask;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.event.IvyEvent;
import org.apache.ivy.plugins.trigger.AbstractTrigger;
import org.apache.tools.ant.Project;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.util.FileChecksumCalculator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * This trigger is fired when a {@code pre-publish-artifact} has occurred. Allowing to get module data as to the
 * artifacts that will be published.
 *
 * @author Tomer Cohen
 */
public class IvyModuleTrigger extends AbstractTrigger {

    private static List<Module> allModules = Lists.newArrayList();

    private Properties fileLocations;
    private static final String FILE_LOCATIONS_FILE_NAME = "file-locations.properties";

    public IvyModuleTrigger() throws IOException {
        fileLocations = new Properties();
    }


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
        fileLocations.put(organization + "." + artifactFile.getName(), path);
        try {
            saveFileLocationFile(fileLocations);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        int indexOfModule = allModules.indexOf(module);
        if (indexOfModule != -1) {
            allModules.set(indexOfModule, module);
        } else {
            allModules.add(module);
        }
    }

    private void saveFileLocationFile(Properties fileLocations) throws IOException {
        File baseDir = IvyContext.getContext().getSettings().getBaseDir();
        File propFileLocation = new File(baseDir, FILE_LOCATIONS_FILE_NAME);
        if (!propFileLocation.exists()) {
            propFileLocation.createNewFile();
        } else {
            propFileLocation.delete();
            propFileLocation.createNewFile();
        }
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(propFileLocation);
            fileLocations.store(out, "");
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    public static List<Module> getAllModules() {
        return allModules;
    }
}
