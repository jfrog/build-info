package org.jfrog.build.api.repository;

public class RepositoryResult {
    private String key;
    private String rclass;
    private String description;
    private String url;
    private String packageType;

    public void setDescription(String description) {
        this.description = description;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setPackageType(String packageType) {
        this.packageType = packageType;
    }

    public String getRclass() {
        return rclass;
    }

    public void setRclass(String rclass) {
        this.rclass = rclass;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
