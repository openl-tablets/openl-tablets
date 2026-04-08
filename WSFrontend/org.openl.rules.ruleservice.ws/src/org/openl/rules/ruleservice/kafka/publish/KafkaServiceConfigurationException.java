package org.openl.rules.ruleservice.kafka.publish;

import java.io.Serial;

public class KafkaServiceConfigurationException extends KafkaServiceException {

    @Serial
    private static final long serialVersionUID = 1L;

    public KafkaServiceConfigurationException() {
        super();
    }

    public KafkaServiceConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public KafkaServiceConfigurationException(String message) {
        super(message);
    }

    public KafkaServiceConfigurationException(Throwable cause) {
        super(cause);
    }

}
