package org.jfrog.build.client.bintrayResponse;

import org.codehaus.jackson.annotate.JsonIgnore;

import java.util.List;

/**
 * This represent failed response from pushToBintray
 *
 * @author Aviad Shikloshi
 */
public class BintrayFailure extends BintrayResponse {

    private List<BintrayError> errors;

    @Override
    @JsonIgnore
    public String toString(){
        StringBuilder sb = new StringBuilder("\nBintray push Failed with some Errors:\n\n");
        for (BintrayError error : errors){
            sb.append("Status Code: ").append(error.getStatus()).append("\nMessage:").append(error.getMessage())
            .append("\n\n");
        }
        sb.append("View Artifactory logs for more details.\n");
        return sb.toString();
    }

    public List<BintrayError> getErrors() {
        return errors;
    }

    public void setErrors(List<BintrayError> error) {
        this.errors = error;
    }

    @Override
    public boolean isSuccessful() {
        return false;
    }

    private static class BintrayError {

        private int status;
        private String message;

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}