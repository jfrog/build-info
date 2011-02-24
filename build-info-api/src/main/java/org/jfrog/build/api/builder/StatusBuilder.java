package org.jfrog.build.api.builder;

import org.jfrog.build.api.Build;
import org.jfrog.build.api.Status;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Noam Y. Tenne
 */
public class StatusBuilder {

    private String status;
    private String comment;
    private String repository;
    private String timestamp;
    private String user;

    public StatusBuilder(String status) {
        this.status = status;
    }

    public StatusBuilder comment(String comment) {
        this.comment = comment;
        return this;
    }

    public StatusBuilder repository(String repository) {
        this.repository = repository;
        return this;
    }

    public StatusBuilder timestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public StatusBuilder timestampDate(Date timestampDate) {
        if (timestampDate == null) {
            throw new IllegalArgumentException("Cannot format a null date.");
        }
        this.timestamp = new SimpleDateFormat(Build.STARTED_FORMAT).format(timestampDate);
        return this;
    }

    public StatusBuilder user(String user) {
        this.user = user;
        return this;
    }

    public Status build() {
        if (status == null) {
            throw new IllegalArgumentException("Status must have a type.");
        }
        if (repository == null) {
            throw new IllegalArgumentException("Status must have a target repository.");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Status must have a timestamp.");
        }
        if (user == null) {
            throw new IllegalArgumentException("Status must have a user.");
        }
        return new Status(status, comment, repository, timestamp, user);
    }
}
