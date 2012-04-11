package org.openl.rules.ruleservice.core;

public class RuleServiceWrapperException extends RuleServiceException {

    private static final long serialVersionUID = 3618613334261575918L;

    public RuleServiceWrapperException() {
        super();
    }

    public RuleServiceWrapperException(String message) {
        super(message);
    }

    public RuleServiceWrapperException(String message, Throwable cause) {
        super(message, cause);
    }

    public RuleServiceWrapperException(Throwable cause) {
        super(cause);
    }
}
