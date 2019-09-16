package org.openl.rules.ruleservice.kafka.ser;

public class SerializationException extends RuntimeException {
    private static final long serialVersionUID = -8022442458648677937L;

    public SerializationException() {
        super();
    }

    public SerializationException(String message,
            Throwable cause,
            boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SerializationException(String message) {
        super(message);
    }

    public SerializationException(Throwable cause) {
        super(cause);
    }

}
