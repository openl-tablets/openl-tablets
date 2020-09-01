package org.openl.rules.ruleservice.kafka.publish;

public class AmbiguousMethodException extends RuntimeException {

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
