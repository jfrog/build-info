package org.jfrog.build.client.bintrayResponse;

/**
 * This is an empty response that contains only the status code.
 * used in any case {@link org.jfrog.build.client.bintrayResponse.BintrayResponseFactory}
 * will be unable to retrieve the response content from Http.
 *
 * @author Aviad Shikloshi
 */
public class EmptyBintrayResponse extends BintrayResponse {

    private int status;

    public EmptyBintrayResponse(int status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "\nStatus Code: " + status + "\nView Artifactory logs for more details.\n";
    }

    @Override
    public boolean isSuccessful() {
        return false;
    }
}
