package org.jfrog.build.api.util;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.jfrog.build.api.util.CommonUtils.handleJavaTmpdirProperty;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.FileAssert.fail;

/**
 * @author yahavi
 **/
@Test
public class CreateTempDirTests {

    private static final String JAVA_IO_TMPDIR_PROP = "java.io.tmpdir";
    private static final String OLD_JAVA_IO_TMPDIR = System.getProperty(JAVA_IO_TMPDIR_PROP);

    @AfterMethod
    public void setUp() {
        System.setProperty(JAVA_IO_TMPDIR_PROP, OLD_JAVA_IO_TMPDIR);
    }

    public void createTempDirPropertyExist() {
        String oldJavaIoTmpdir = System.getProperty(JAVA_IO_TMPDIR_PROP);
        try {
            handleJavaTmpdirProperty();
            assertEquals(System.getProperty(JAVA_IO_TMPDIR_PROP), oldJavaIoTmpdir);
        } catch (RuntimeException e) {
            fail(ExceptionUtils.getRootCauseMessage(e));
        }
    }

    public void createTempDirMissingProperty() {
        System.setProperty(JAVA_IO_TMPDIR_PROP, "");
        if (SystemUtils.IS_OS_WINDOWS) {
            assertThrows(IOException.class, CommonUtils::handleJavaTmpdirProperty);
            return;
        }
        try {
            handleJavaTmpdirProperty();
            assertEquals(System.getProperty(JAVA_IO_TMPDIR_PROP), "/tmp");
        } catch (RuntimeException e) {
            fail(ExceptionUtils.getRootCauseMessage(e));
        }
    }

    public void createTempDirMissingDir() {
        Path currentDir = Paths.get("not-exist-dir").toAbsolutePath();
        System.setProperty(JAVA_IO_TMPDIR_PROP, currentDir.toString());
        try {
            handleJavaTmpdirProperty();
            assertEquals(System.getProperty(JAVA_IO_TMPDIR_PROP), currentDir.toString());
        } catch (RuntimeException e) {
            fail(ExceptionUtils.getRootCauseMessage(e));
        }
    }

}
