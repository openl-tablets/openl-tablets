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
        return SourceCodeURLTool.makeSourceLocationURL(source.getSourceLocation(), source.getModule());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((source == null) ? 0 : source.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        OpenLWarnMessage other = (OpenLWarnMessage) obj;
        if (source == null) {
            if (other.source != null) {
                return false;
            }
        } else if (!source.equals(other.source)) {
            return false;
        }
        return true;
    }

}
