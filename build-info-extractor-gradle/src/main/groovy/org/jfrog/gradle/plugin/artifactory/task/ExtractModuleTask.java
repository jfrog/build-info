package org.jfrog.gradle.plugin.artifactory.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.util.GradleVersion;
import org.jfrog.build.extractor.ci.Module;
import org.jfrog.build.extractor.ModuleExtractorUtils;
import org.jfrog.gradle.plugin.artifactory.extractor.GradleModuleExtractor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class ExtractModuleTask extends DefaultTask {

    private final RegularFileProperty moduleFile;

    public ExtractModuleTask() {
        boolean gradleVersionOlderThanFiveZero = GradleVersion.current().compareTo(GradleVersion.version("5.0")) < 0;
        this.moduleFile = gradleVersionOlderThanFiveZero ?  invokeMethod(getProject().getLayout(), "fileProperty") : getProject().getObjects().fileProperty();
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
