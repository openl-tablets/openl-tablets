package org.openl.syntax.exception.formatter;

/**
 * Common interface for formatting exception messages. Some java exceptions, has no informative messages. Appropriate
 * implementation of this interface may add additional information to such messages.
 * 
 * @author DLiauchuk
 *
 */
public interface ExceptionMessageFormatter {
    String format(Throwable error);
}
