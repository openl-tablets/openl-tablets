package org.openl.rules.ruleservice.kafka.publish;

import java.io.Serial;

public class MethodNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -8445338558999623680L;

    public MethodNotFoundException() {
    }

    public MethodNotFoundException(String message) {
        super(message);
    }

}
