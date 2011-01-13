package org.openl.syntax.exception;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.text.ILocation;

public class SyntaxNodeExceptionUtils {

    private SyntaxNodeExceptionUtils() {
    }

    public static SyntaxNodeException createError(String message, IOpenSourceCodeModule source) {
        return new SyntaxNodeException(message, null, null, source);
    }

    public static SyntaxNodeException createError(String message, ISyntaxNode syntaxNode) {
        return new SyntaxNodeException(message, null, syntaxNode);
    }

    public static SyntaxNodeException createError(String message,
            Throwable throwable,
            ILocation location,
            IOpenSourceCodeModule source) {
        return new SyntaxNodeException(message, throwable, location, source);
    }

    public static SyntaxNodeException createError(String message, Throwable throwable, ISyntaxNode syntaxNode) {
        return new SyntaxNodeException(message, throwable, syntaxNode);
    }

    public static SyntaxNodeException createError(Throwable throwable, ISyntaxNode syntaxNode) {
        return createError(throwable.getMessage(), throwable, syntaxNode);
    }
}
