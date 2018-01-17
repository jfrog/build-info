/*
 *
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2016 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.jfrog.build.api.release;

import java.io.Serializable;

/**
 * Represents a mapping between an input regexp pattern of build artifacts to their output target directory.
 * The target directory (output) is used only by the archive REST API and represents the hierarchy of the returned archive.
 * <p>RegExp capturing groups are supported, the corresponding place holders must be presented in the output regexp with the '$' prefix <br>
 * For example: input="(.+)/(.+)-sources.jar", output=""$1/sources/$2.jar""
 *
 * @author Shay Yaakov
 */
public class BuildArtifactsMapping implements Serializable {

    private String input;

    /**
     * Optionally, when omitted, the output target directory will be the full artifact relative path
     */
    private String output;

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }
}
