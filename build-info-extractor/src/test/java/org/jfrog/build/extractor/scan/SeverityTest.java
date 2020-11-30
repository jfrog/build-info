package org.jfrog.build.extractor.scan;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author yahavi
 */
public class SeverityTest {

    @Test
    public void testFromString() {
        Assert.assertEquals(Severity.FromString("High"), Severity.High);
        Assert.assertEquals(Severity.FromString("Medium"), Severity.Medium);
        Assert.assertEquals(Severity.FromString("Low"), Severity.Low);
        Assert.assertEquals(Severity.FromString("Information"), Severity.Information);
        Assert.assertEquals(Severity.FromString("Unknown"), Severity.Unknown);
        Assert.assertEquals(Severity.FromString("Pending Scan"), Severity.Pending);
        Assert.assertEquals(Severity.FromString("Scanned - No Issues"), Severity.Normal);
        Assert.assertEquals(Severity.FromString("Critical"), Severity.High);
        Assert.assertEquals(Severity.FromString("Major"), Severity.Medium);
        Assert.assertEquals(Severity.FromString("Minor"), Severity.Low);
    }
}