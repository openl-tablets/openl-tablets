package org.openl.rules.ruleservice.databinding;

import org.openl.rules.ruleservice.core.RuleServiceRuntimeException;

public class ServiceConfigurationException extends RuleServiceRuntimeException {

    private static final long serialVersionUID = 1L;

    public ServiceConfigurationException() {
        super();
    }

    public ServiceConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceConfigurationException(String message) {
        super(message);
    }

    public ServiceConfigurationException(Throwable cause) {
        super(cause);
    }

}
