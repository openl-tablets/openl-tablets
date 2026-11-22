package org.openl.rules.ruleservice.kafka.ser;

import java.io.Serial;

public class SerializationException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -8022442458648677937L;

    public SerializationException() {
    }

    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }

}
