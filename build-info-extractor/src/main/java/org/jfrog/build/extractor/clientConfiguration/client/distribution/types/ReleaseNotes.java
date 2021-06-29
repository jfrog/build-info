package org.jfrog.build.extractor.clientConfiguration.client.distribution.types;

import java.io.Serializable;

/**
 * @author yahavi
 **/
public class ReleaseNotes implements Serializable {
    private static final long serialVersionUID = 1L;

    private Syntax syntax;
    private String content;

    public Syntax getSyntax() {
        return syntax;
    }

    public void setSyntax(Syntax syntax) {
        this.syntax = syntax;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public enum Syntax {
        markdown, asciidoc, plain_text
    }
}
