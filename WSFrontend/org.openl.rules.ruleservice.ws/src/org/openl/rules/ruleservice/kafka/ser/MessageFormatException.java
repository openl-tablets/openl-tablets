package org.openl.rules.ruleservice.kafka.ser;

public class MessageFormatException extends RuntimeException {

    private static final long serialVersionUID = -176280184625803237L;

    public MessageFormatException() {
        super();
    }

    public MessageFormatException(String message,
            Throwable cause,
            boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public MessageFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public MessageFormatException(String message) {
        super(message);
    }

    public MessageFormatException(Throwable cause) {
        super(cause);
    }

}