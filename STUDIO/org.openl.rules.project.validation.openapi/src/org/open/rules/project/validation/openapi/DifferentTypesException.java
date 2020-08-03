package org.open.rules.project.validation.openapi;

public class DifferentTypesException extends Exception {
    public DifferentTypesException() {
    }

    public DifferentTypesException(String message) {
        super(message);
    }

    public DifferentTypesException(String message, Throwable cause) {
        super(message, cause);
    }

    public DifferentTypesException(Throwable cause) {
        super(cause);
    }

    public DifferentTypesException(String message,
            Throwable cause,
            boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
