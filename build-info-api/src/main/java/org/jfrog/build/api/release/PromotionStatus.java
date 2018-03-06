package org.jfrog.build.api.release;

import org.jfrog.build.api.Build;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Noam Y. Tenne
 */
public class PromotionStatus implements Serializable {

    public static final String RELEASED = "Released";

    private String status;
    private String comment;
    private String repository;
    private String timestamp;
    private String user;
    private String ciUser;

    public PromotionStatus() {
    }

    public PromotionStatus(String status, String comment, String repository, String timestamp, String user,
                           String ciUser) {
        this.status = status;
        this.comment = comment;
        this.repository = repository;
        this.timestamp = timestamp;
        this.user = user;
        this.ciUser = ciUser;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    // For Serialization/Deserialization
    public Date getTimestampDate() {
        if (timestamp == null) {
            throw new IllegalArgumentException("Cannot parse a null timestamp as a date");
        }
        SimpleDateFormat format = new SimpleDateFormat(Build.STARTED_FORMAT);
        try {
            return format.parse(timestamp);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    // For Serialization/Deserialization
    public void setTimestampDate(Date timestampDate) {
        SimpleDateFormat format = new SimpleDateFormat(Build.STARTED_FORMAT);
        setTimestamp(format.format(timestampDate));
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

    public String getCiUser() {
        return ciUser;
    }

    public void setCiUser(String ciUser) {
        this.ciUser = ciUser;
    }
}
