package org.jfrog.build.extractor.scan;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author yahavi
 */
public class Artifact implements Serializable {

    private static final long serialVersionUID = 1L;

    private GeneralInfo generalInfo = new GeneralInfo();
    private Set<Issue> issues = new HashSet<>();
    private Set<License> licenses = new HashSet<>();

    // Empty constructor for serialization
    public Artifact() {
    }

    public Artifact(GeneralInfo generalInfo, Set<Issue> issues, Set<License> licenses) {
        this.generalInfo = new GeneralInfo(generalInfo);
        this.issues.addAll(issues
                .stream()
                .map(Issue::new)
                .collect(Collectors.toSet()));
        this.licenses.addAll(licenses
                .stream()
                .map(License::new)
                .collect(Collectors.toSet()));
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
