package org.jfrog.build.extractor.scan;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * @author yahavi
 */
public class SeverityTest {

    @Test
    public void testIsHigherThan() {
        for (Severity severityOne : Severity.values()) {
            int expectedSeverityOne = getExpectedSeverityLevel(severityOne);
            for (Severity severityTwo : Severity.values()) {
                int expectedSeverityTwo = getExpectedSeverityLevel(severityTwo);

                if (expectedSeverityOne < expectedSeverityTwo) {
                    assertTrue(severityTwo.isHigherThan(severityOne));
                    continue;
                }
                if (expectedSeverityTwo < expectedSeverityOne) {
                    assertTrue(severityOne.isHigherThan(severityTwo));
                    continue;
                }
                assertFalse(severityTwo.isHigherThan(severityOne));
            }
        }
    }

    @Test
    public void testFromString() {
        assertEquals(Severity.fromString("Critical"), Severity.Critical);
        assertEquals(Severity.fromString("High"), Severity.High);
        assertEquals(Severity.fromString("Medium"), Severity.Medium);
        assertEquals(Severity.fromString("Low"), Severity.Low);
        assertEquals(Severity.fromString("Information"), Severity.Information);
        assertEquals(Severity.fromString("Unknown"), Severity.Unknown);
        assertEquals(Severity.fromString("Pending Scan"), Severity.Pending);
        assertEquals(Severity.fromString("Scanned - No Issues"), Severity.Normal);
    }

    private int getExpectedSeverityLevel(Severity severity) {
        switch (severity) {
            case Normal:
                return 0;
            case Pending:
                return 1;
            case Unknown:
                return 2;
            case Information:
                return 3;
            case Low:
                return 4;
            case Medium:
                return 5;
            case High:
                return 6;
            default:
                return 7;
        }
    }
}