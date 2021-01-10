package org.jfrog.build.extractor.scan;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author yahavi
 */
public class SeverityTest {

    @Test
    public void testFromString() {
        Assert.assertEquals(Severity.fromString("High"), Severity.High);
        Assert.assertEquals(Severity.fromString("Medium"), Severity.Medium);
        Assert.assertEquals(Severity.fromString("Low"), Severity.Low);
        Assert.assertEquals(Severity.fromString("Information"), Severity.Information);
        Assert.assertEquals(Severity.fromString("Unknown"), Severity.Unknown);
        Assert.assertEquals(Severity.fromString("Pending Scan"), Severity.Pending);
        Assert.assertEquals(Severity.fromString("Scanned - No Issues"), Severity.Normal);
        Assert.assertEquals(Severity.fromString("Critical"), Severity.High);
        Assert.assertEquals(Severity.fromString("Major"), Severity.Medium);
        Assert.assertEquals(Severity.fromString("Minor"), Severity.Low);
    }
}