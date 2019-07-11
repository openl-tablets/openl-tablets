package org.openl.rules.ruleservice.kafka.publish;

public class OutputTopicIsNotDefinedException extends RuntimeException {

    private static final long serialVersionUID = -1559097123094367222L;

    public OutputTopicIsNotDefinedException() {
        super();
    }

    public OutputTopicIsNotDefinedException(String message,
            Throwable cause,
            boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public OutputTopicIsNotDefinedException(String message, Throwable cause) {
        super(message, cause);
    }

    public OutputTopicIsNotDefinedException(String message) {
        super(message);
    }

    public OutputTopicIsNotDefinedException(Throwable cause) {
        super(cause);
    }

}
