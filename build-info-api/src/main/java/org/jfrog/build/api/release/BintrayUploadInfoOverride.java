package org.jfrog.build.api.release;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * Represents override properties that can be passed to artifactory with the REST command instead of the descriptor
 *
 * @author Dan Feldman
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BintrayUploadInfoOverride {

    public String subject;
    public String repoName;
    public String packageName;
    public String versionName;
    public List<String> licenses; //Mandatory only for OSS
    @JsonProperty("vcs_url")
    public String vcsUrl;         //Mandatory only for OSS

    public BintrayUploadInfoOverride() {

    }

    public BintrayUploadInfoOverride(String subject, String repoName, String packageName, String versionName,
                                     List<String> licenses, String vcsUrl) {
        this.subject = subject;
        this.repoName = repoName;
        this.packageName = packageName;
        this.versionName = versionName;
        this.licenses = licenses;
        this.vcsUrl = vcsUrl;
    }

    @JsonIgnore
    public boolean isValid() {
        return (StringUtils.isNotBlank(subject) && StringUtils.isNotBlank(repoName)
                && StringUtils.isNotBlank(packageName) && StringUtils.isNotBlank(versionName));
    }

    @JsonIgnore
    public boolean isEmpty() {
        return subject == null && repoName == null && packageName == null && versionName == null
                && (licenses == null || licenses.isEmpty()) && vcsUrl == null;
    }

}
