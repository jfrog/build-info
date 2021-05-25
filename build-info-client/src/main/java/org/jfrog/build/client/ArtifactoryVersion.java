package org.jfrog.build.client;

public class ArtifactoryVersion extends Version {
    public static final ArtifactoryVersion NOT_FOUND = new ArtifactoryVersion("0.0.0");
    private final boolean addons;

    public ArtifactoryVersion(String version) {
        this(version, false);
    }

    public ArtifactoryVersion(String version, boolean addons) {
        super(version);
        this.addons = addons;
    }

    public boolean hasAddons() {
        return addons;
    }

    public boolean isOSS(){
        return !hasAddons();
    }
}
