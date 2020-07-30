package org.jfrog.build.extractor.pip.extractor;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by Bar Belity on 21/07/2020.
 */
public class TestUtils {

    static Path createTempDir(String targetDir) throws IOException {
        return Files.createTempDirectory(targetDir);
    }

    static Path createTestProjectTempDir(String targetDir, File projectOrigin) throws IOException {
        File projectDir = createTempDir(targetDir).toFile();
        FileUtils.copyDirectory(projectOrigin, projectDir);
        return projectDir.toPath();
    }

}
