package org.jfrog.build.extractor.buildScanTable;

import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.artifactoryXrayResponse.*;
import org.jfrog.build.extractor.scan.Severity;

import java.util.*;

import static org.jfrog.build.api.util.CommonUtils.emptyIfNull;

/***
 * Helper for printing build scan results as a violations table to log.
 */
@SuppressWarnings("unused")
public class BuildScanTableHelper {
    private ArtifactoryXrayResponse scanResult;
    private Map<Severity, Set<BuildScanTableElement>> table;
    private Log log;
    private int longestDisplayName = 0;
    private String tableFormat = "";
    public String TABLE_HEADLINE = "Xray Scan Summary:";
    public List<String> TABLE_HEADERS = Arrays.asList("#", "Component", "Severity", "Type");

    @SuppressWarnings("unused")
    public void PrintTable(ArtifactoryXrayResponse scanResult, Log log) {
        this.scanResult = scanResult;
        this.log = log;
        generateResultTable();
        updateTableFormat();
        print();
    }

    /***
     * Prints the generated build scan table to log.
     * Table is rendered with the table format.
     */
    private void print() {
        int line = 1;
        Severity[] severities = Severity.values();

        // Print table headline.
        log.info(TABLE_HEADLINE);
        // Print column headers.
        printLine(TABLE_HEADERS.toArray());

        // Print lines of violations by descending severity.
        for (int i = severities.length - 1; i >= 0; i--) {
            Severity severity = severities[i];
            Set<BuildScanTableElement> elements = table.get(severity);
            if (elements == null) {
                continue;
            }
            for (BuildScanTableElement element : elements) {
                printLine(line, element.getFileDisplayName(), severity.getSeverityName(), element.getIssueType());
                line++;
            }
        }
        log.info("");
    }

    private void printLine(Object... args) {
        log.info(String.format(tableFormat, args));
    }

    /***
     * Updates table format after longestDisplayName is known.
     * Format aligns elements to the left.
     * Padding on a column must be longer than the longest element in that column.
     */
    private void updateTableFormat() {
        // Index (assuming 5 digits is sufficient).
        tableFormat = "%-6s"
                // Display name (plus space).
                + "%-" + (longestDisplayName + 5) + "s"
                // Severity (Longest is 'Information').
                + "%-15s"
                // Type (Longest is 'Security').
                + "%-10s";
    }

    /***
     * Loops over all alerts and adds infected files with required information.
     */
    private void generateResultTable() {
        table = new HashMap<>();
        for (Alert alert : emptyIfNull(scanResult.getAlerts())) {
            for (Issue issue : emptyIfNull(alert.getIssues())) {
                for (ImpactedArtifact impactedArtifact : emptyIfNull(issue.getImpactedArtifacts())) {
                    for (InfectedFile infectedFile : emptyIfNull(impactedArtifact.getInfectedFiles())) {
                        addElement(issue, infectedFile);
                    }
                }
            }
        }
    }

    private void addElement(Issue issue, InfectedFile infectedFile) {
        // Create table element.
        Severity severity = Severity.fromString(issue.getSeverity());
        BuildScanTableElement buildScanTableElement = new BuildScanTableElement(infectedFile.getDisplayName(), infectedFile.getSha256(),
                issue.getType(), issue.getSummary(), issue.getDescription());

        // Add element to table.
        Set<BuildScanTableElement> elements = table.get(severity);
        if (elements == null) {
            elements = new HashSet<>();
        }
        elements.add(buildScanTableElement);
        table.put(severity, elements);

        // Update longest display name if longer.
        if (infectedFile.getDisplayName().length() > longestDisplayName) {
            longestDisplayName = infectedFile.getDisplayName().length();
        }
    }
}
