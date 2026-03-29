package org.openl.rules.ruleservice.core;

import java.io.Serial;

/**
 * Exception for instantiation issues.
 *
 * @author Marat Kamalov
 */
public class RuleServiceInstantiationException extends RuleServiceException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RuleServiceInstantiationException() {
        super();
    }

    public RuleServiceInstantiationException(String message, Throwable cause) {
        super(message, cause);
    }

    public RuleServiceInstantiationException(String message) {
        super(message);
    }

    public RuleServiceInstantiationException(Throwable cause) {
        super(cause);
    }

}
