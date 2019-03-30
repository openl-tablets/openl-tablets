package org.openl.syntax.exception.formatter;

public class NullPointerExceptionFormatter implements ExceptionMessageFormatter {

    @Override
    public String format(Throwable error) {
        if (error instanceof NullPointerException) {
            return "The element is null";
        }
        return error.getMessage();

    }

}
