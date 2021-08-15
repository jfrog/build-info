package org.jfrog.build.extractor.buildScanTable;

import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.artifactoryXrayResponse.Issue;
import org.jfrog.build.extractor.scan.Severity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Base class for violations tables printed by {@link BuildScanTableHelper}
 */
abstract class ScanTableBase {
    private final Log log;
    protected int longestDisplayName = 0;
    protected final Map<Severity, Set<TableElementBase>> table = new HashMap<>();

    protected ScanTableBase(Log log) {
        this.log = log;
    }

    protected abstract String getHeadline();

    protected abstract String[] getHeaders();

    protected abstract String getTableFormat();

    protected abstract String getEmptyTableLine();

    protected String getFormatBase(int longestDisplayName) {
        // Index (assuming 5 digits is sufficient).
        return "%-6s"
                // Severity (Longest is 'Information').
                + "%-14s"
                // Display name (plus space).
                + "%-" + (longestDisplayName + 3) + "s";
    }

    private void printFormattedLine(Object... args) {
        log.info(String.format(getTableFormat(), args));
    }

    protected void addElement(Map<Severity, Set<TableElementBase>> table, Issue issue, TableElementBase element) {
        Severity severity = Severity.fromString(issue.getSeverity());
        Set<TableElementBase> elements = table.get(severity);
        if (elements == null) {
            elements = new HashSet<>();
        }
        elements.add(element);
        table.put(severity, elements);
    }

    protected void printTable(Map<Severity, Set<TableElementBase>> table) {
        int line = 1;
        Severity[] severities = Severity.values();

        // Print table headline.
        log.info(getHeadline());

        // If table is empty, print the no violations found line and return.
        if (table.isEmpty()) {
            log.info(getEmptyTableLine());
            log.info("");
            return;
        }

        // Print column headers.
        printFormattedLine((Object[]) getHeaders());

        // Print lines of violations by descending severity.
        for (int i = severities.length - 1; i >= 0; i--) {
            Severity severity = severities[i];
            Set<TableElementBase> elements = table.get(severity);
            if (elements == null) {
                continue;
            }
            for (TableElementBase element : elements) {
                printFormattedLine(element.getLineArgs(line, severity.getSeverityName()));
                line++;
            }
        }
        log.info("");
    }

    /**
     * Base class for elements of the violations tables
     */
    protected abstract static class TableElementBase {
        protected final String fileDisplayName;
        // Following fields are for the set's uniqueness only:
        protected final String fileSha256;
        protected final String issueSummary;
        protected final String issueDescription;

        protected TableElementBase(String fileDisplayName, String fileSha256,
                                   String issueSummary, String issueDescription) {
            this.fileDisplayName = fileDisplayName;
            this.fileSha256 = fileSha256;
            this.issueSummary = issueSummary;
            this.issueDescription = issueDescription;
        }

        protected String getFileDisplayName() {
            return fileDisplayName == null ? "" : fileDisplayName;
        }

        protected abstract Object[] getLineArgs(int line, String severityName);
    }
}
