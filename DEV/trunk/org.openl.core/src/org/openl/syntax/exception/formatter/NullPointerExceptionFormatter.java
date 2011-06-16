package org.openl.syntax.exception.formatter;

public class NullPointerExceptionFormatter implements ExceptionMessageFormatter {

    public String format(Throwable error) {
        if (error instanceof NullPointerException) {
            return String.format("The element is null", error.getMessage());
        }
        return error.getMessage();
        
    }

}
