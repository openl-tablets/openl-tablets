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

    /**
     * {@inheritDoc}
     */
    public RuleServiceInstantiationException() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    public RuleServiceInstantiationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * {@inheritDoc}
     */
    public RuleServiceInstantiationException(String message) {
        super(message);
    }

    /**
     * {@inheritDoc}
     */
    public RuleServiceInstantiationException(Throwable cause) {
        super(cause);
    }

}
