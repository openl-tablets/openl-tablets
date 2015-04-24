package org.openl.message;

import org.openl.main.SourceCodeURLTool;
import org.openl.syntax.ISyntaxNode;

public class OpenLWarnMessage extends OpenLMessage {

    private ISyntaxNode source;

    public OpenLWarnMessage(String summary, ISyntaxNode source) {
        super(summary, Severity.WARN);
        if (source == null) {
            throw new NullPointerException();
        }
        this.source = source;
    }

    public ISyntaxNode getSource() {
        return source;
    }

    @Override
    public String getSourceLocation() {
        return SourceCodeURLTool.makeSourceLocationURL(source.getSourceLocation(), source.getModule(), "");
    }
}
