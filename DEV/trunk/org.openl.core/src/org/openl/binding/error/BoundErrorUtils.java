package org.openl.binding.error;

import org.openl.syntax.ISyntaxNode;

public abstract class BoundErrorUtils {

    //    public static BoundError createError(String message, IOpenSourceCodeModule source) {
    //        return new BoundError(message, null, null, source);
    //    }

    public static BoundError createError(String message, ISyntaxNode syntaxNode) {
        return new BoundError(message, syntaxNode);
    }

    public static BoundError createError(String message, Throwable throwable, ISyntaxNode syntaxNode) {
        return new BoundError(message, throwable, syntaxNode);
    }

    public static BoundError createError(Throwable throwable, ISyntaxNode syntaxNode) {
        return createError(throwable.getMessage(), throwable, syntaxNode);
    }

}
