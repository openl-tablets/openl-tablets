package org.openl.message;

import org.apache.commons.lang.StringUtils;
import org.openl.syntax.ISyntaxNode;

public class OpenLWarnMessage extends OpenLMessage {

    private ISyntaxNode source;

    public OpenLWarnMessage(String summary, String details) {
        this(summary, details, null);
    }

    public OpenLWarnMessage(String summary, ISyntaxNode source) {
        this(summary, StringUtils.EMPTY, source);
    }

    public OpenLWarnMessage(String summary, String details, ISyntaxNode source) {
        super(summary, details, Severity.WARN);

        this.source = source;
    }

    public ISyntaxNode getSource() {
        return source;
    }

}
