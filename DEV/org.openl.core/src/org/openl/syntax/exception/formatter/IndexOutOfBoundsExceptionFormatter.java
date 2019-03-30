package org.openl.syntax.exception.formatter;

/**
 * Formatter for {@link ArrayIndexOutOfBoundsException} exception. As its message contains 
 * only the index number.
 * 
 * @author DLiauchuk
 *
 */
public class IndexOutOfBoundsExceptionFormatter implements ExceptionMessageFormatter {

    @Override
    public String format(Throwable error) {
        if (error instanceof ArrayIndexOutOfBoundsException) {
            return String.format("There is no index %s in the sequence", error.getMessage());
        }
        return error.getMessage();
    }

}
