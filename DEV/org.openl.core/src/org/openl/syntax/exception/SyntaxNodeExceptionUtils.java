package org.openl.syntax.exception;

import java.util.HashMap;
import java.util.Map;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.formatter.ExceptionMessageFormatter;
import org.openl.syntax.exception.formatter.IndexOutOfBoundsExceptionFormatter;
import org.openl.syntax.exception.formatter.NoClassDefFoundErrorFormatter;
import org.openl.syntax.exception.formatter.NullPointerExceptionFormatter;
import org.openl.util.text.ILocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyntaxNodeExceptionUtils {

    private static Map<Class<?>, ExceptionMessageFormatter> formatters = new HashMap<>();

    static {
        formatters.put(ArrayIndexOutOfBoundsException.class, new IndexOutOfBoundsExceptionFormatter());
        formatters.put(NoClassDefFoundError.class, new NoClassDefFoundErrorFormatter());
        formatters.put(NullPointerException.class, new NullPointerExceptionFormatter());
    }

    private SyntaxNodeExceptionUtils() {
    }

    public static SyntaxNodeException createError(String message, IOpenSourceCodeModule source) {
        return createError(message, null, null, source);
    }

    public static SyntaxNodeException createError(String message, ISyntaxNode syntaxNode) {
        return createError(message, null, syntaxNode);
    }

    public static SyntaxNodeException createError(String message,
            Throwable ex,
            ILocation location,
            IOpenSourceCodeModule source) {
        Logger logger = LoggerFactory.getLogger(SyntaxNodeExceptionUtils.class);
        logger.debug(message, ex);
        return new SyntaxNodeException(message, ex, location, source);
    }

    public static SyntaxNodeException createError(Throwable ex,
            ILocation location,
            IOpenSourceCodeModule source) {
        return createError(formatErrorMessage(ex), ex, location, source);
    }

    public static SyntaxNodeException createError(String message, Throwable ex, ISyntaxNode syntaxNode) {
        Logger logger = LoggerFactory.getLogger(SyntaxNodeExceptionUtils.class);
        logger.debug(message, ex);
        return new SyntaxNodeException(message, ex, syntaxNode);
    }

    public static SyntaxNodeException createError(Throwable ex, ISyntaxNode syntaxNode) {
        return createError(formatErrorMessage(ex), ex, syntaxNode);
    }

    private static String formatErrorMessage(Throwable ex) {
        String formattedMessage = ex.getMessage();
        ExceptionMessageFormatter filter = formatters.get(ex.getClass());
        if (filter != null) {
            formattedMessage = filter.format(ex);
        }
        if (formattedMessage == null) {
            return ex.getClass().getSimpleName();
        }
        return formattedMessage;
    }
}
