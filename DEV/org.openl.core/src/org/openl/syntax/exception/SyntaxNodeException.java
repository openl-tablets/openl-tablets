package org.openl.syntax.exception;

import org.openl.exception.OpenLCompilationException;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.text.ILocation;

public class SyntaxNodeException extends OpenLCompilationException {

    private static final long serialVersionUID = 4448924727461016950L;

    public SyntaxNodeException(String message, Throwable cause, ILocation location, IOpenSourceCodeModule source) {
        super(message, cause, location, source);
    }

    public SyntaxNodeException(String message, Throwable cause, ISyntaxNode syntaxNode) {
        this(message,
            cause,
            syntaxNode == null ? null : syntaxNode.getSourceLocation(),
            syntaxNode == null ? null : syntaxNode.getModule());
    }
}
