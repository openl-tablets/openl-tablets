package org.openl.rules.ruleservice.databinding;

import org.openl.rules.ruleservice.core.RuleServiceException;

public class ServiceConfigurationException extends RuleServiceException {

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
