package org.artifactory.build.api;

/**
 * Base implementation of the build file bean interface
 *
 * @author Noam Y. Tenne
 */
public abstract class BaseBuildFileBean extends BaseBuildBean implements BuildFileBean {

    protected String type;
    protected String sha1;
    protected String md5;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }
}