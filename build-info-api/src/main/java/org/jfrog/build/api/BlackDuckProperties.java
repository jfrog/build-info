package org.jfrog.build.api;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.io.Serializable;

/**
 * @author mamo
 */
@XStreamAlias("blackduck")
public class BlackDuckProperties implements Serializable {

    private boolean blackDuckRunChecks;
    private String blackDuckAppName;
    private String blackDuckAppVersion;

    public BlackDuckProperties() {
    }

    public boolean isBlackDuckRunChecks() {
        return blackDuckRunChecks;
    }

    public void setBlackDuckRunChecks(boolean blackDuckRunChecks) {
        this.blackDuckRunChecks = blackDuckRunChecks;
    }

    public String getBlackDuckAppName() {
        return blackDuckAppName;
    }

    public void setBlackDuckAppName(String blackDuckAppName) {
        this.blackDuckAppName = blackDuckAppName;
    }

    public String getBlackDuckAppVersion() {
        return blackDuckAppVersion;
    }

    public void setBlackDuckAppVersion(String blackDuckAppVersion) {
        this.blackDuckAppVersion = blackDuckAppVersion;
    }
}
