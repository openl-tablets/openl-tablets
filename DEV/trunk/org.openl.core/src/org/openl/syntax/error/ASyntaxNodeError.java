package org.openl.syntax.error;

import org.openl.error.AOpenLError;
import org.openl.main.SourceCodeURLTool;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.text.ILocation;

public class ASyntaxNodeError extends AOpenLError implements ISyntaxNodeError {

    private static final long serialVersionUID = 4448924727461016950L;

    private ISyntaxNode syntaxNode;
    private ISyntaxNode topLevelSyntaxNode;

    public ASyntaxNodeError(String message, Throwable cause, ILocation location, IOpenSourceCodeModule source) {
        super(message, cause, location, source);
    }

    public ASyntaxNodeError(String message, Throwable cause, ILocation location) {
        super(message, cause, location);
    }

    public ASyntaxNodeError(String message, Throwable cause, ISyntaxNode syntaxNode) {
        this(message, cause, syntaxNode == null ? null : syntaxNode.getSourceLocation());

        this.syntaxNode = syntaxNode;
    }

    public IOpenSourceCodeModule getSourceModule() {

        IOpenSourceCodeModule source = super.getSourceModule();

        if (source != null) {
            return source;
        } else if (syntaxNode != null) {
            return syntaxNode.getModule();
        }

        return null;
    }

    public ISyntaxNode getSyntaxNode() {
        return syntaxNode;
    }

    public void setTopLevelSyntaxNode(ISyntaxNode topLevelSyntaxNode) {
        this.topLevelSyntaxNode = topLevelSyntaxNode;
    }

    public ISyntaxNode getTopLevelSyntaxNode() {
        return topLevelSyntaxNode;
    }

    public String getUri() {
        return SourceCodeURLTool.makeSourceLocationURL(getLocation(), getSourceModule(), "");
    }
}
