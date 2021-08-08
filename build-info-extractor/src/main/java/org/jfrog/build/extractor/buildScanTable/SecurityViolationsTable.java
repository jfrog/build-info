package org.jfrog.build.extractor.buildScanTable;

import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.artifactoryXrayResponse.InfectedFile;
import org.jfrog.build.client.artifactoryXrayResponse.Issue;

import java.util.Objects;

class SecurityViolationsTable extends ScanTableBase {
    SecurityViolationsTable(Log log) {
        super(log);
    }

    String getHeadline() {
        return "Security Violations";
    }

    String[] getHeaders() {
        return new String[]{"#", "Severity", "Component", "CVE"};
    }

    String getTableFormat() {
        return super.getFormatBase(longestDisplayName)
                // CVE.
                + "%-20s";
    }

    String getEmptyTableLine() {
        return "No security compliance violations were found";
    }

    void addElement(Issue issue, InfectedFile infectedFile) {
        // Create table element.
        SecurityTableElement element = new SecurityTableElement(infectedFile.getDisplayName(), infectedFile.getSha256(),
                issue.getSummary(), issue.getDescription(), issue.getCve());
        super.addElement(table, issue, element);
        // Update the longest display name if longer.
        if (infectedFile.getDisplayName() != null && infectedFile.getDisplayName().length() > longestDisplayName) {
            longestDisplayName = infectedFile.getDisplayName().length();
        }
    }

    void printTable() {
        super.printTable(table);
    }

    static class SecurityTableElement extends TableElementBase {
        private final String cve;

        SecurityTableElement(String fileDisplayName, String fileSha256,
                             String issueSummary, String issueDescription, String cve) {
            super(fileDisplayName, fileSha256, issueSummary, issueDescription);
            this.cve = cve;
        }

        String getCve() {
            return cve == null ? "" : cve;
        }

        @Override
        Object[] getLineArgs(int line, String severityName) {
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
