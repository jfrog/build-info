package org.jfrog.build.api.builder;

import org.jfrog.build.api.Build;
import org.jfrog.build.api.release.PromotionStatus;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Noam Y. Tenne
 */
public class PromotionStatusBuilder {

    private String status;
    private String comment;
    private String repository;
    private String timestamp;
    private String user;
    private String ciUser;

    public PromotionStatusBuilder(String status) {
        this.status = status;
    }

    public PromotionStatusBuilder comment(String comment) {
        this.comment = comment;
        return this;
    }

    public PromotionStatusBuilder repository(String repository) {
        this.repository = repository;
        return this;
    }

    public PromotionStatusBuilder timestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public PromotionStatusBuilder timestampDate(Date timestampDate) {
        if (timestampDate == null) {
            throw new IllegalArgumentException("Cannot format a null date.");
        }
        this.timestamp = new SimpleDateFormat(Build.STARTED_FORMAT).format(timestampDate);
        return this;
    }

    public PromotionStatusBuilder user(String user) {
        this.user = user;
        return this;
    }

    public PromotionStatusBuilder ciUser(String ciUser) {
        this.ciUser = ciUser;
        return this;
    }

    public PromotionStatus build() {
        if (status == null) {
            throw new IllegalArgumentException("Status must have a type.");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Status must have a timestamp.");
        }
        return new PromotionStatus(status, comment, repository, timestamp, user, ciUser);
    }
}
