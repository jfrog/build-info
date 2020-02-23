package org.jfrog.build.extractor.go;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by Bar Belity on 19/02/2020.
 */
public class TestUtils {

    public static final Path PROJECTS_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "org", "jfrog", "build", "extractor"));

    public static Path createProjectDir(String targetDir, File projectOrigin) throws IOException {
        File projectDir = Files.createTempDirectory(targetDir).toFile();
        FileUtils.copyDirectory(projectOrigin, projectDir);
        return projectDir.toPath();
    }


}
