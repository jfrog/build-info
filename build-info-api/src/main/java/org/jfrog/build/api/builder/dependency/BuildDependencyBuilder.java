/*
 * Copyright (C) 2012 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    private String uri;

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
        buildDependency.setUri(uri);
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
     * Sets the uri of the build dependency
     *
     * @param uri build dependency uri
     * @return Builder instance
     */
    public BuildDependencyBuilder uri(String uri) {
        this.uri = uri;
        return this;
    }

}