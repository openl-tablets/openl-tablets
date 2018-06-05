package org.openl.rules.ruleservice.logging;

import org.openl.rules.ruleservice.core.RuleServiceException;

public class ProcessingException extends RuleServiceException {

    private static final long serialVersionUID = 4793264886531859843L;

    public ProcessingException() {
        super();
    }

    public ProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProcessingException(String message) {
        super(message);
    }

    public ProcessingException(Throwable cause) {
        super(cause);
    }

}
