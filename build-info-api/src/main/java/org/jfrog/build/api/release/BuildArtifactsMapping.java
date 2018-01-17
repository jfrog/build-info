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
