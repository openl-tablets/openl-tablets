package org.openl.rules.ruleservice.kafka.publish;

import org.openl.rules.ruleservice.core.RuleServiceException;

public class UndefinedTopicException extends RuleServiceException {

    private static final long serialVersionUID = -1559097123094367222L;

    public UndefinedTopicException() {
        super();
    }

    public UndefinedTopicException(String message, Throwable cause) {
        super(message, cause);
    }

    public UndefinedTopicException(String message) {
        super(message);
    }

    public UndefinedTopicException(Throwable cause) {
        super(cause);
    }

}
