package org.openl.syntax.exception.formatter;


public class NoClassDefFoundErrorFormatter implements ExceptionMessageFormatter {

    public String format(Throwable error) {
        if (error instanceof NoClassDefFoundError) {
            return String.format("Can't load type '%s'!", error.getCause().getMessage());
        }
        return error.getMessage();
        
    }

}
