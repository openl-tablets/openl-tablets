package org.openl.rules.ruleservice.kafka.ser;

public class RequestMessageFormatException extends RuntimeException {

    private static final long serialVersionUID = -176280184625803237L;

    public RequestMessageFormatException() {
        super();
    }

    public RequestMessageFormatException(String message,
            Throwable cause,
            boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public RequestMessageFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public RequestMessageFormatException(String message) {
        super(message);
    }

    public RequestMessageFormatException(Throwable cause) {
        super(cause);
    }

}