package org.jfrog.build.extractor.scan;

/**
 * @author yahavi
 */
public enum Severity {
    Normal("Scanned - No Issues"),
    Pending("Pending Scan"),
    Unknown("Unknown"),
    Information("Information"),
    Low("Low"),
    Medium("Medium"),
    High("High"),
    Critical("Critical");

    private final String severityName;

    Severity(String severityName) {
        this.severityName = severityName;
    }

    public String getSeverityName() {
        return this.severityName;
    }

    public boolean isHigherThan(Severity other) {
        return this.ordinal() > other.ordinal();
    }

    public static Severity fromString(String inputSeverity) {
        for (Severity severity : Severity.values()) {
            if (severity.getSeverityName().equals(inputSeverity)) {
                return severity;
            }
        }
        throw new IllegalArgumentException("Severity " + inputSeverity + " doesn't exist");
    }

}