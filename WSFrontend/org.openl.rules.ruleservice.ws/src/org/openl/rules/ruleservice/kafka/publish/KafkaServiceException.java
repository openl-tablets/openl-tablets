package org.openl.rules.ruleservice.kafka.publish;

import java.io.Serial;

import org.openl.rules.ruleservice.core.RuleServiceException;

public class KafkaServiceException extends RuleServiceException {

    @Serial
    private static final long serialVersionUID = -5914325743466096251L;

    public KafkaServiceException() {
        super();
    }

    public KafkaServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public KafkaServiceException(String message) {
        super(message);
    }

    public KafkaServiceException(Throwable cause) {
        super(cause);
    }

}
