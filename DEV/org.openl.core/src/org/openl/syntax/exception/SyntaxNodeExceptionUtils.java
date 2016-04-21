package org.openl.syntax.exception;

import java.util.HashMap;
import java.util.Map;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.formatter.ExceptionMessageFormatter;
import org.openl.syntax.exception.formatter.IndexOutOfBoundsExceptionFormatter;
import org.openl.syntax.exception.formatter.NullPointerExceptionFormatter;
import org.openl.util.text.ILocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyntaxNodeExceptionUtils {
    
    private static Map<Class<?>, ExceptionMessageFormatter> formatters = new HashMap<Class<?>, ExceptionMessageFormatter>();
    
    static {
        formatters.put(ArrayIndexOutOfBoundsException.class, new IndexOutOfBoundsExceptionFormatter());
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
            Throwable throwable,
            ILocation location,
            IOpenSourceCodeModule source) {
        Logger logger = LoggerFactory.getLogger(SyntaxNodeExceptionUtils.class);
        logger.info(message, throwable);
        return new SyntaxNodeException(message, throwable, location, source);
    }

    public static SyntaxNodeException createError(String message, Throwable throwable, ISyntaxNode syntaxNode) {
        Logger logger = LoggerFactory.getLogger(SyntaxNodeExceptionUtils.class);
        logger.info(message, throwable);
        return new SyntaxNodeException(message, throwable, syntaxNode);
    }

    public static SyntaxNodeException createError(Throwable throwable, ISyntaxNode syntaxNode) {
        return createError(formatErrorMessage(throwable), throwable, syntaxNode);
    }
    
    private static String formatErrorMessage(Throwable throwable) {
        String formattedMessage = throwable.getMessage();
        ExceptionMessageFormatter filter = formatters.get(throwable.getClass());
        if (filter != null) {
            formattedMessage = filter.format(throwable);
        }
        return formattedMessage;
     }
}
