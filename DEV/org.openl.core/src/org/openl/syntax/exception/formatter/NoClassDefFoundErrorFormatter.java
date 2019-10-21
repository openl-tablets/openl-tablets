package org.openl.syntax.exception.formatter;

public class NoClassDefFoundErrorFormatter implements ExceptionMessageFormatter {

    @Override
    public String format(Throwable error) {
        Throwable cause = error.getCause();
        if (error instanceof NoClassDefFoundError && cause != null) {
            return String.format("Cannot load type '%s'!", cause.getMessage());
        }
        return error.getMessage();

    }

}
