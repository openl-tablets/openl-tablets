package org.openl.syntax.error;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.impl.SyntaxError;

public abstract class SyntaxErrorUtils {

    public static SyntaxError createError(String message, IOpenSourceCodeModule source) {
        return new SyntaxError(message, null, null, source);
    }
}
