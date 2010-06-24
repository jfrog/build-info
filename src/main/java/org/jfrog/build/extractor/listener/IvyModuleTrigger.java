package org.jfrog.build.extractor.listener;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.ivy.core.event.IvyEvent;
import org.apache.ivy.plugins.trigger.AbstractTrigger;
import org.apache.ivy.plugins.trigger.Trigger;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.util.FileChecksumCalculator;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * @author Tomer Cohen
 */
public class IvyModuleTrigger extends AbstractTrigger implements Trigger {

    private static List<Module> allModules = Lists.newArrayList();

    public void progress(IvyEvent event) {
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

    public static List<Module> getAllModules() {
        return allModules;
    }
}
