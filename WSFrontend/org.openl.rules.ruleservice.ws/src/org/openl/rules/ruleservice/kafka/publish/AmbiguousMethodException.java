package org.openl.rules.ruleservice.kafka.publish;

public class AmbiguousMethodException extends RuntimeException {

    public AmbiguousMethodException() {
    }

    public AmbiguousMethodException(String message) {
        super(message);
    }

}
