package org.jfrog.build.extractor.clientConfiguration.client.distribution.types;

/**
 * @author yahavi
 **/
public class ReleaseNotes {
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
