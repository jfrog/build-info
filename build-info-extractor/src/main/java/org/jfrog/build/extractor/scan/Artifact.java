package org.jfrog.build.extractor.scan;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author yahavi
 */
public class Artifact implements Serializable {

    private static final long serialVersionUID = 1L;

    private GeneralInfo generalInfo;
    private Set<Issue> issues;
    private Set<License> licenses;

    // Empty constructor for serialization
    public Artifact() {
        generalInfo = new GeneralInfo();
        issues = new HashSet<>();
        licenses = new HashSet<>();
    }

    public Artifact(GeneralInfo generalInfo, Set<Issue> issues, Set<License> licenses) {
        this.generalInfo = generalInfo;
        this.issues = issues;
        this.licenses = licenses;
    }

    @SuppressWarnings("unused")
    public GeneralInfo getGeneralInfo() {
        return generalInfo;
    }

    @SuppressWarnings("unused")
    public void setGeneralInfo(GeneralInfo generalInfo) {
        this.generalInfo = generalInfo;
    }

    public Set<Issue> getIssues() {
        return issues;
    }

    public void setIssues(Set<Issue> issues) {
        this.issues = issues;
    }

    public Set<License> getLicenses() {
        return licenses;
    }

    public void setLicenses(Set<License> licenses) {
        this.licenses = licenses;
    }
}
