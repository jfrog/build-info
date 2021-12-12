package org.jfrog.build.api.builder.dependency;

import org.jfrog.build.api.Build;
import org.jfrog.build.api.dependency.BuildDependency;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A builder for the build dependency class
 *
 * @author jbaruch
 */
public class BuildDependencyBuilder {

    private String name;
    private String number;
    private String started;
    private String url;

    /**
     * Assembles the build dependency class
     *
     * @return Assembled module
     */
    public BuildDependency build() {
        if (name == null) {
            throw new IllegalArgumentException("BuildDependency must have a name.");
        }
        if (number == null) {
            throw new IllegalArgumentException("BuildDependency must have a number.");
        }
        if (started == null) {
            throw new IllegalArgumentException("BuildDependency must have a started time.");
        }

        BuildDependency buildDependency = new BuildDependency();
        buildDependency.setName(name);
        buildDependency.setNumber(number);
        buildDependency.setStarted(started);
        buildDependency.setUrl(url);
        return buildDependency;
    }

    /**
     * Sets the name of the build dependency
     *
     * @param name build dependency name
     * @return Builder instance
     */
    public BuildDependencyBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the number of the build dependency
     *
     * @param number build dependency number
     * @return Builder instance
     */
    public BuildDependencyBuilder number(String number) {
        this.number = number;
        return this;
    }

    /**
     * Sets the started of the build dependency
     *
     * @param started build dependency started
     * @return Builder instance
     */
    public BuildDependencyBuilder started(String started) {
        this.started = started;
        return this;
    }


    /**
     * Sets the started of the build dependency from Date
     *
     * @param startedDate build dependency started as date
     * @return Builder instance
     */
    public BuildDependencyBuilder startedDate(Date startedDate) {
        if (startedDate == null) {
            throw new IllegalArgumentException("Cannot format a null date.");
        }
        this.started = new SimpleDateFormat(Build.STARTED_FORMAT).format(startedDate);
        return this;
    }


    /**
     * Sets the url of the build dependency
     *
     * @param url build dependency url
     * @return Builder instance
     */
    public BuildDependencyBuilder url(String url) {
        this.url = url;
        return this;
    }

}