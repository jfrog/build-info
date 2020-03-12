package org.jfrog.gradle.plugin.artifactory.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.jfrog.build.api.Module;
import org.jfrog.build.extractor.ModuleExtractorUtils;
import org.jfrog.gradle.plugin.artifactory.extractor.GradleModuleExtractor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class ExtractModuleTask extends DefaultTask {

    public RegularFileProperty moduleFile;

    public ExtractModuleTask() {
        try {
            this.moduleFile = getProject().getObjects().fileProperty();
        } catch (NoSuchMethodError e) {
            // Gradle 4.x
            this.moduleFile = invokeMethod(getProject().getLayout(), "fileProperty");
        }
    }

    @OutputFile
    public RegularFileProperty getModuleFile() {
        return moduleFile;
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

    <T> T invokeMethod(Object source, String methodName) {
        try {
            return (T) source.getClass().getMethod(methodName).invoke(source);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
