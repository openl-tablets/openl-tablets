package org.openl.syntax.exception.formatter;

public class NoClassDefFoundErrorFormatter implements ExceptionMessageFormatter {

    @Override
    public String format(Throwable error) {
        Throwable cause = error.getCause();
        if (error instanceof NoClassDefFoundError && cause != null) {
            return "Cannot load type '%s'.".formatted(cause.getMessage());
        }
        return error.getMessage();

    }

}
