package org.jfrog.build.extractor.buildScanTable;

import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.artifactoryXrayResponse.*;

import static org.jfrog.build.api.util.CommonUtils.emptyIfNull;

/***
 * Helper for printing build scan results as violations tables to log.
 */
@SuppressWarnings("unused")
public class BuildScanTableHelper {
    private ArtifactoryXrayResponse scanResult;
    private Log log;

    SecurityViolationsTable securityViolationsTable;
    LicenseViolationsTable licenseViolationsTable;

    @SuppressWarnings("unused")
    public void printTable(ArtifactoryXrayResponse scanResult, Log log) {
        this.scanResult = scanResult;
        this.log = log;
        securityViolationsTable = new SecurityViolationsTable(log);
        licenseViolationsTable = new LicenseViolationsTable(log);
        generateResultTable();
        doPrintTables();
    }

    /***
     * Prints the generated violations tables to log.
     */
    private void doPrintTables() {
        securityViolationsTable.printTable();
        log.info("");
        licenseViolationsTable.printTable();
    }

    /***
     * Loops over all alerts and adds infected files with required information.
     */
    private void generateResultTable() {
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

    /**
     * Add a violation to the corresponding table.
     *
     * @param issue        Issue that caused violation.
     * @param infectedFile Infected file.
     */
    private void addElement(Issue issue, InfectedFile infectedFile) {
        Issue.IssueType issueType = issue.getIssueType();
        if (issueType == Issue.IssueType.SECURITY) {
            securityViolationsTable.addElement(issue, infectedFile);
        } else if (issueType == Issue.IssueType.LICENSE) {
            licenseViolationsTable.addElement(issue, infectedFile);
        } else {
            throw new IllegalArgumentException(String.format("Illegal issue type '%s'. Expecting either 'Security' or 'License'", issue.getType()));
        }
    }
}
