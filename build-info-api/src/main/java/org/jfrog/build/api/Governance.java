package org.jfrog.build.api;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.io.Serializable;

/**
 * @author mamo
 */
@XStreamAlias("governance")
public class Governance implements Serializable {

    private BlackDuckProperties blackDuckProperties;

    public Governance() {
    }

    public BlackDuckProperties getBlackDuckProperties() {
        return blackDuckProperties;
    }

    public void setBlackDuckProperties(BlackDuckProperties blackDuckProperties) {
        this.blackDuckProperties = blackDuckProperties;
    }
}
