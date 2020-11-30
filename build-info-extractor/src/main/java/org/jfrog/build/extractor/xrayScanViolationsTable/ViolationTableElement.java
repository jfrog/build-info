package org.jfrog.build.extractor.xrayScanViolationsTable;

import org.apache.commons.lang.StringUtils;

import java.util.Objects;

/***
 * Struct to hold a vulnerability table element.
 */
public class ViolationTableElement {
    private final String fileDisplayName;
    private final String issueType;
    // Following fields are for the set's uniqueness only:
    private final String fileSha256;
    private final String issueSummary;
    private final String issueDescription;

    public ViolationTableElement(String fileDisplayName, String fileSha256, String issueType,
                                 String issueSummary, String issueDescription) {
        this.fileDisplayName = fileDisplayName;
        this.fileSha256 = fileSha256;
        this.issueType = issueType;
        this.issueSummary = issueSummary;
        this.issueDescription = issueDescription;
    }

    public String getFileDisplayName() {
        return fileDisplayName == null ? "" : fileDisplayName;
    }

    public String getIssueType() {
        return StringUtils.capitalize(issueType == null ? "" : issueType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ViolationTableElement that = (ViolationTableElement) o;
        return Objects.equals(fileDisplayName, that.fileDisplayName) &&
                Objects.equals(issueType, that.issueType) &&
                Objects.equals(fileSha256, that.fileSha256) &&
                Objects.equals(issueSummary, that.issueSummary) &&
                Objects.equals(issueDescription, that.issueDescription);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileDisplayName, issueType, fileSha256, issueSummary, issueDescription);
    }
}
