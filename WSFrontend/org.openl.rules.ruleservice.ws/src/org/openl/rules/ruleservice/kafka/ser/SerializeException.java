package org.openl.rules.ruleservice.kafka.ser;

public class SerializeException extends RuntimeException {
    private static final long serialVersionUID = -8022442458648677937L;

    public SerializeException() {
        super();
    }

    public SerializeException(String message,
            Throwable cause,
            boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public SerializeException(String message, Throwable cause) {
        super(message, cause);
    }

    public SerializeException(String message) {
        super(message);
    }

    public SerializeException(Throwable cause) {
        super(cause);
    }

}
