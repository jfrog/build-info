package org.jfrog.build.api;

import java.io.Serializable;

/**
 * @author Noam Y. Tenne
 */
public class Status implements Serializable {

    private final StatusType status;
    private final String comment;
    private final String repository;
    private final String timestamp;
    private final String user;

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

    public String getComment() {
        return comment;
    }

    public String getRepository() {
        return repository;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getUser() {
        return user;
    }
}
