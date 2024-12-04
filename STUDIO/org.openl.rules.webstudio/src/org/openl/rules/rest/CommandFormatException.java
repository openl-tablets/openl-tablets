package org.openl.rules.rest;

@Deprecated
public class CommandFormatException extends Exception {
    public CommandFormatException() {
    }

    public CommandFormatException(String message) {
        super(message);
    }

    public CommandFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommandFormatException(Throwable cause) {
        super(cause);
    }

    public CommandFormatException(String message,
                                  Throwable cause,
                                  boolean enableSuppression,
                                  boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
