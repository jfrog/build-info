package org.jfrog.build.api.release;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.List;

/**
 * Represents override properties that can be passed to artifactory with the REST command instead of the descriptor
 * @author Dan Feldman
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BintrayUploadInfoOverride {

    public String subject;
    public String repoName;
    public String packageName;
    public String versionName;
    public List<String> licenses;

    public BintrayUploadInfoOverride(String subject, String repoName, String packageName,
                                     String versionName, List<String> licenses) {
        this.subject = subject;
        this.repoName = repoName;
        this.packageName = packageName;
        this.versionName = versionName;
        this.licenses = licenses;
    }

    @JsonIgnore
    public boolean isValid() {
        return (StringUtils.isNotBlank(subject) && StringUtils.isNotBlank(repoName)
                && StringUtils.isNotBlank(packageName) && StringUtils.isNotBlank(versionName)
                && (licenses != null && !licenses.isEmpty() && StringUtils.isNotBlank(licenses.get(0))));
    }

    @JsonIgnore
    public boolean isEmpty() {
        return subject == null && repoName == null && packageName == null && versionName == null
                && (licenses == null || licenses.isEmpty());
    }

}
