package org.jfrog.build.client.bintrayResponse;

import org.codehaus.jackson.annotate.JsonIgnore;

/**
 * Represent succeeded request from Artifactory to Bintray
 *
 * @author Aviad Shikloshi
 */
public class BintraySuccess extends BintrayResponse {

    private String message;

    @JsonIgnore
    @Override
    public String toString(){
        return "\nStatus Code: 200\n" + message + "\n";
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}