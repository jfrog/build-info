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
    Log log;
    int longestDisplayName = 0;
    final Map<Severity, Set<TableElementBase>> table = new HashMap<>();

    ScanTableBase(Log log) {
        this.log = log;
    }

    abstract String getHeadline();

    abstract String[] getHeaders();

    abstract String getTableFormat();

    abstract String getEmptyTableLine();

    String getFormatBase(int longestDisplayName) {
        // Index (assuming 5 digits is sufficient).
        return "%-6s"
                // Severity (Longest is 'Information').
                + "%-14s"
                // Display name (plus space).
                + "%-" + (longestDisplayName + 3) + "s";
    }

    void printFormattedLine(Object... args) {
        log.info(String.format(getTableFormat(), args));
    }

    void addElement(Map<Severity, Set<TableElementBase>> table, Issue issue, TableElementBase element) {
        Severity severity = Severity.fromString(issue.getSeverity());
        Set<TableElementBase> elements = table.get(severity);
        if (elements == null) {
            elements = new HashSet<>();
        }
        elements.add(element);
        table.put(severity, elements);
    }

    void printTable(Map<Severity, Set<TableElementBase>> table) {
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
    abstract static class TableElementBase {
        public final String fileDisplayName;
        // Following fields are for the set's uniqueness only:
        public final String fileSha256;
        public final String issueSummary;
        public final String issueDescription;

        public TableElementBase(String fileDisplayName, String fileSha256,
                                String issueSummary, String issueDescription) {
            this.fileDisplayName = fileDisplayName;
            this.fileSha256 = fileSha256;
            this.issueSummary = issueSummary;
            this.issueDescription = issueDescription;
        }

        String getFileDisplayName() {
            return fileDisplayName == null ? "" : fileDisplayName;
        }

        abstract Object[] getLineArgs(int line, String severityName);
    }
}
