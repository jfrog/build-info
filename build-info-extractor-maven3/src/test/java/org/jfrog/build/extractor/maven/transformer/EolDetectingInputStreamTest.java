package org.jfrog.build.extractor.maven.transformer;

import org.apache.commons.io.IOUtils;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Noam Y. Tenne
 */
public class EolDetectingInputStreamTest {

    @org.testng.annotations.Test
    public void testNoEols() throws Exception {
        EolDetectingInputStream stream = new EolDetectingInputStream(IOUtils.toInputStream("asdfasfdasdfasfd"));
        stream.read(new byte["momobob".getBytes().length]);
        assertFalse(stream.isCr(), "Unexpected CR EOL.");
        assertFalse(stream.isLf(), "Unexpected LF EOL.");
    }

    @org.testng.annotations.Test
    public void testOnlyCr() throws Exception {
        EolDetectingInputStream stream = new EolDetectingInputStream(IOUtils.toInputStream("\r"));
        stream.read();
        assertTrue(stream.isCr(), "Expected a CR EOL.");
        assertFalse(stream.isLf(), "Unexpected LF EOL.");
    }

    @org.testng.annotations.Test
    public void testOnlyLf() throws Exception {
        EolDetectingInputStream stream = new EolDetectingInputStream(IOUtils.toInputStream("\n"));
        stream.read();
        assertFalse(stream.isCr(), "Unexpected CR EOL.");
        assertTrue(stream.isLf(), "Expected a LF EOL.");
    }

    @org.testng.annotations.Test
    public void testNestedCr() throws Exception {
        EolDetectingInputStream stream = new EolDetectingInputStream(IOUtils.toInputStream("asdfasfd\rasaewfacva"));
        stream.read(new byte["asdfasfd\rasaewfacva".getBytes().length], 0, "asdfasfd\rasaewfacva".getBytes().length);
        assertTrue(stream.isCr(), "Expected a CR EOL.");
        assertFalse(stream.isLf(), "Unexpected LF EOL.");
    }

    @org.testng.annotations.Test
    public void testNestedLf() throws Exception {
        EolDetectingInputStream stream = new EolDetectingInputStream(IOUtils.toInputStream("fghujnd\nrtyryujth"));
        stream.read(new byte["fghujnd\nrtyryujth".getBytes().length], 0, "fghujnd\nrtyryujth".getBytes().length);
        assertFalse(stream.isCr(), "Unexpected CR EOL.");
        assertTrue(stream.isLf(), "Expected a LF EOL.");
    }

    @Test
    public void testOnlyBoth() throws Exception {
        EolDetectingInputStream stream = new EolDetectingInputStream(IOUtils.toInputStream("\n\r"));
        stream.read(new byte[2]);
        assertTrue(stream.isCr(), "Expected a CR EOL.");
        assertTrue(stream.isLf(), "Expected a LF EOL.");
    }

    @Test
    public void testNestedBoth() throws Exception {
        EolDetectingInputStream stream = new EolDetectingInputStream(
                IOUtils.toInputStream("agreagh5y2w45y\nv24y5b3u4\rc1r24t2ct"));
        stream.read(new byte["agreagh5y2w45y\nv24y5b3u4\rc1r24t2ct".getBytes().length]);
        assertTrue(stream.isCr(), "Expected a CR EOL.");
        assertTrue(stream.isLf(), "Expected a LF EOL.");
    }
}
