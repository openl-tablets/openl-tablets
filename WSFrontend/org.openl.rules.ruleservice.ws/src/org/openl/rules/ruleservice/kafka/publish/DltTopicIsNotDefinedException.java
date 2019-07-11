package org.openl.rules.ruleservice.kafka.publish;

public class DltTopicIsNotDefinedException extends RuntimeException {

    private static final long serialVersionUID = -1559097123094367222L;

    public DltTopicIsNotDefinedException() {
        super();
    }

    public DltTopicIsNotDefinedException(String message,
            Throwable cause,
            boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public DltTopicIsNotDefinedException(String message, Throwable cause) {
        super(message, cause);
    }

    public DltTopicIsNotDefinedException(String message) {
        super(message);
    }

    public DltTopicIsNotDefinedException(Throwable cause) {
        super(cause);
    }

}
