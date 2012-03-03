package org.openl.rules.ruleservice.core;


/**
 * Exception for issues that occurs while openL service object instantiation.
 * 
 * @author Marat Kamalov
 * 
 */
public class RuleServiceOpenLServiceInstantiationException extends RuleServiceException {

    private static final long serialVersionUID = 1L;

    /** {@inheritDoc} */
    public RuleServiceOpenLServiceInstantiationException() {
        super();
    }

    /** {@inheritDoc} */
    public RuleServiceOpenLServiceInstantiationException(String message, Throwable cause) {
        super(message, cause);
    }

    /** {@inheritDoc} */
    public RuleServiceOpenLServiceInstantiationException(String message) {
        super(message);
    }

    /** {@inheritDoc} */
    public RuleServiceOpenLServiceInstantiationException(Throwable cause) {
        super(cause);
    }

}
