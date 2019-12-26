package org.jfrog.gradle.plugin.artifactory

import org.apache.commons.io.IOUtils
import org.junit.rules.TemporaryFolder
import org.junit.runner.Description
import org.junit.runners.model.Statement
import sun.misc.Resource

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING

class TestBuildRule extends TemporaryFolder {
    private final String testBuildPath
    private Description description

    TestBuildRule() {
        this(null)
    }

    TestBuildRule(String testBuildPath) {
        super()
        this.testBuildPath = testBuildPath
    }

    @Override
    Statement apply(Statement base, Description description) {
        this.description = description
        return super.apply(base, description)
    }

    @Override
    protected void before() throws Throwable {
        super.before()

        String effectiveTestBuildPath = getEffectiveTestBuildPath(description)
        if (effectiveTestBuildPath == null) {
            throw new IllegalArgumentException("No test build path has been specified.  Either specify it when " +
                    "constructing the ${this.getClass().getSimpleName()} instance, or specify it with the " +
                    "@UsesTestBuild annotation.")
        }

        URL testResource = Thread.currentThread().contextClassLoader.getResource(effectiveTestBuildPath)
        if (testResource == null) {
            throw new IllegalArgumentException("Cannot find resource '${effectiveTestBuildPath}' on classpath.")
        }

        Path sourceDir = new File(testResource.toURI()).toPath()
        Path targetDir = getRoot().toPath()
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path targetFile = targetDir.resolve(sourceDir.relativize(file));
                Files.copy(file, targetFile, COPY_ATTRIBUTES, REPLACE_EXISTING);

                return FileVisitResult.CONTINUE;
            }

            @Override
            FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path newDir = targetDir.resolve(sourceDir.relativize(dir));
                Files.createDirectories(newDir);

                return FileVisitResult.CONTINUE;
            }
        })
    }

    String getEffectiveTestBuildPath(Description description) {
        UsesTestBuild annotation = description.getAnnotation(UsesTestBuild)
        return annotation != null ? annotation.value() : testBuildPath
    }
}
