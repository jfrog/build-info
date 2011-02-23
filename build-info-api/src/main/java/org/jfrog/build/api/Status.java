package org.jfrog.build.api;

import java.io.Serializable;

/**
 * @author Noam Y. Tenne
 */
public class Status implements Serializable {

    private StatusType status;
    private String comment;
    private String repository;
    private String timestamp;
    private String user;

    public Status() {
    }

    public Status(StatusType status, String comment, String repository, String timestamp, String user) {
        this.status = status;
        this.comment = comment;
        this.repository = repository;
        this.timestamp = timestamp;
        this.user = user;
    }

    public StatusType getStatus() {
        return status;
    }

    public void setStatus(StatusType status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
