package org.openl.rules.ruleservice.kafka.publish;

public class AmbiguousMethodException extends RuntimeException {

    private static final long serialVersionUID = -8445338558999623680L;

    public AmbiguousMethodException() {
        super();
    }

    public AmbiguousMethodException(String message,
            Throwable cause,
            boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public AmbiguousMethodException(String message, Throwable cause) {
        super(message, cause);
    }

    public AmbiguousMethodException(String message) {
        super(message);
    }

    public AmbiguousMethodException(Throwable cause) {
        super(cause);
    }

}
