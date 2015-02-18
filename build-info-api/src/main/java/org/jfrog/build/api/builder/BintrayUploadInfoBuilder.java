package org.jfrog.build.api.builder;

import org.jfrog.build.api.release.BintrayUploadInfoOverride;

import java.util.List;

/**
 * @author Aviad Shikloshi
 */
public class BintrayUploadInfoBuilder {

    private String subject;
    private String repoName;
    private String packageName;
    private String versionName;
    private List<String> licenses;

    public BintrayUploadInfoBuilder() {
    }

    public BintrayUploadInfoBuilder setSubject(String subject){
        this.subject = subject;
        return this;
    }

    public BintrayUploadInfoBuilder setRepoName(String repoName){
        this.repoName = repoName;
        return this;
    }

    public BintrayUploadInfoBuilder setPackageName(String packageName){
        this.packageName = packageName;
        return this;
    }

    public BintrayUploadInfoBuilder setLicenses(List<String> licenses){
        this.licenses = licenses;
        return this;
    }

    public BintrayUploadInfoBuilder setVersionName(String versionName){
        this.versionName = versionName;
        return this;
    }

    public BintrayUploadInfoOverride build(){
        return new BintrayUploadInfoOverride(subject, repoName,
                packageName, versionName, licenses);
    }

}
