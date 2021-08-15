package org.jfrog.build.extractor.buildScanTable;

import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.artifactoryXrayResponse.InfectedFile;
import org.jfrog.build.client.artifactoryXrayResponse.Issue;

import java.util.Objects;

public class SecurityViolationsTable extends ScanTableBase {
    public static final String SECURITY_VIOLATIONS_TABLE_HEADLINE = "Security Violations";

    protected SecurityViolationsTable(Log log) {
        super(log);
    }

    protected String getHeadline() {
        return SECURITY_VIOLATIONS_TABLE_HEADLINE;
    }

    protected String[] getHeaders() {
        return new String[]{"#", "Severity", "Component", "CVE"};
    }

    protected String getTableFormat() {
        return super.getFormatBase(longestDisplayName)
                // CVE.
                + "%-20s";
    }

    protected String getEmptyTableLine() {
        return "No security compliance violations were found";
    }

    protected void addElement(Issue issue, InfectedFile infectedFile) {
        // Create table element.
        SecurityTableElement element = new SecurityTableElement(infectedFile.getDisplayName(), infectedFile.getSha256(),
                issue.getSummary(), issue.getDescription(), issue.getCve());
        super.addElement(table, issue, element);
        // Update the longest display name if longer.
        if (infectedFile.getDisplayName() != null && infectedFile.getDisplayName().length() > longestDisplayName) {
            longestDisplayName = infectedFile.getDisplayName().length();
        }
    }

    protected void printTable() {
        super.printTable(table);
    }

    private static class SecurityTableElement extends TableElementBase {
        private final String cve;

        SecurityTableElement(String fileDisplayName, String fileSha256,
                             String issueSummary, String issueDescription, String cve) {
            super(fileDisplayName, fileSha256, issueSummary, issueDescription);
            this.cve = cve;
        }

        private String getCve() {
            return cve == null ? "" : cve;
        }

        @Override
        protected Object[] getLineArgs(int line, String severityName) {
            return new Object[]{line, severityName, this.getFileDisplayName(), this.getCve()};
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SecurityTableElement that = (SecurityTableElement) o;
            return Objects.equals(fileDisplayName, that.fileDisplayName) &&
                    Objects.equals(fileSha256, that.fileSha256) &&
                    Objects.equals(issueSummary, that.issueSummary) &&
                    Objects.equals(issueDescription, that.issueDescription) &&
                    Objects.equals(cve, that.cve);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fileDisplayName, fileSha256, issueSummary, issueDescription, cve);
        }
    }
}
