package org.jfrog.gradle.plugin.artifactory.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.jfrog.build.api.Module;
import org.jfrog.build.extractor.ModuleExtractorUtils;
import org.jfrog.gradle.plugin.artifactory.extractor.GradleModuleExtractor;

import java.io.IOException;

public class ExtractModuleTask extends DefaultTask {
    @OutputFile
    public RegularFileProperty moduleFile;

    public ExtractModuleTask() {
        try {
            this.moduleFile = getProject().getObjects().fileProperty();
        } catch(NoSuchMethodError e) {
            // Gradle 4.x
            this.moduleFile = getProject().getLayout().fileProperty();
        }
    }

    @TaskAction
    void extractModuleFile() {
        Module module = new GradleModuleExtractor().extractModule(getProject());
        try {
            ModuleExtractorUtils.saveModuleToFile(module, moduleFile.getAsFile().get());
        } catch (IOException e) {
            throw new RuntimeException("Could not extract module file", e);
        }
    }
}
