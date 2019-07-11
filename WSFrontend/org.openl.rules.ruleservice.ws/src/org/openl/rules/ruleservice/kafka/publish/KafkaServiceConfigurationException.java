package org.openl.rules.ruleservice.kafka.publish;

public class KafkaServiceConfigurationException extends KafkaServiceException {

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
