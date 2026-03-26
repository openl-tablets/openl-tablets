package org.openl.rules.ruleservice.kafka.publish;

import java.io.Serial;

public class MethodNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -8445338558999623680L;

    public MethodNotFoundException() {
        super();
    }

    public MethodNotFoundException(String message,
                                   Throwable cause,
                                   boolean enableSuppression,
                                   boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public MethodNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public MethodNotFoundException(String message) {
        super(message);
    }

    public MethodNotFoundException(Throwable cause) {
        super(cause);
    }

}
