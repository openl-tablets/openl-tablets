package org.openl.message;

import org.apache.commons.lang3.StringUtils;
import org.openl.syntax.ISyntaxNode;

public class OpenLWarnMessage extends OpenLMessage {

    private ISyntaxNode source;

    public OpenLWarnMessage(String summary, ISyntaxNode source) {
        super(summary, Severity.WARN);

        this.source = source;
    }

    public ISyntaxNode getSource() {
        return source;
    }

}
