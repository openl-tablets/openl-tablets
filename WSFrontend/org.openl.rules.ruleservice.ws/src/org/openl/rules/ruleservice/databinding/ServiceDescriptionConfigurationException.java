package org.openl.rules.ruleservice.databinding;

import org.openl.rules.ruleservice.core.RuleServiceException;

public class ServiceDescriptionConfigurationException extends RuleServiceException {

    private static final long serialVersionUID = 1L;

    public ServiceDescriptionConfigurationException() {
        super();
    }

    public ServiceDescriptionConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceDescriptionConfigurationException(String message) {
        super(message);
    }

    public ServiceDescriptionConfigurationException(Throwable cause) {
        super(cause);
    }

}
