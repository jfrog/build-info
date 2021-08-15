package org.jfrog.build.extractor.buildScanTable;

import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.artifactoryXrayResponse.InfectedFile;
import org.jfrog.build.client.artifactoryXrayResponse.Issue;

import java.util.Objects;

public class LicenseViolationsTable extends ScanTableBase {
    public static final String LICENSE_VIOLATIONS_TABLE_HEADLINE = "License Compliance Violations";

    protected LicenseViolationsTable(Log log) {
        super(log);
    }

    protected String getHeadline() {
        return LICENSE_VIOLATIONS_TABLE_HEADLINE;
    }

    protected String[] getHeaders() {
        return new String[]{"#", "Severity", "Component"};
    }

    protected String getTableFormat() {
        return super.getFormatBase(longestDisplayName);
    }

    protected String getEmptyTableLine() {
        return "No license compliance violations were found";
    }

    protected void addElement(Issue issue, InfectedFile infectedFile) {
        // Create table element.
        LicenseTableElement element = new LicenseTableElement(infectedFile.getDisplayName(), infectedFile.getSha256(),
                issue.getSummary(), issue.getDescription());
        super.addElement(table, issue, element);
        // Update the longest display name if longer.
        if (infectedFile.getDisplayName() != null && infectedFile.getDisplayName().length() > longestDisplayName) {
            longestDisplayName = infectedFile.getDisplayName().length();
        }
    }

    protected void printTable() {
        super.printTable(table);
    }

    private static class LicenseTableElement extends TableElementBase {
        LicenseTableElement(String fileDisplayName, String fileSha256,
                            String issueSummary, String issueDescription) {
            super(fileDisplayName, fileSha256, issueSummary, issueDescription);
        }

        @Override
        protected Object[] getLineArgs(int line, String severityName) {
            return new Object[]{line, severityName, this.getFileDisplayName()};
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LicenseTableElement that = (LicenseTableElement) o;
            return Objects.equals(fileDisplayName, that.fileDisplayName) &&
                    Objects.equals(fileSha256, that.fileSha256) &&
                    Objects.equals(issueSummary, that.issueSummary) &&
                    Objects.equals(issueDescription, that.issueDescription);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fileDisplayName, fileSha256, issueSummary, issueDescription);
        }
    }
}
