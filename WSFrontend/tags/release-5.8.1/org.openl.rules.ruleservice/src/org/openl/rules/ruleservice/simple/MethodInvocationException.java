package org.openl.rules.ruleservice.simple;

import org.openl.rules.ruleservice.core.RuleServiceSystemException;

public class MethodInvocationException extends RuleServiceSystemException {

    private static final long serialVersionUID = 6506393788240623317L;

    public MethodInvocationException() {
        super();
    }

    public MethodInvocationException(String message) {
        super(message);
    }

    public MethodInvocationException(String message, Throwable cause) {
        super(message, cause);
    }

    public MethodInvocationException(Throwable cause) {
        super(cause);
    }
}
