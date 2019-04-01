package org.openl.rules.ruleservice.publish.test;

import org.openl.rules.ruleservice.core.RuleServiceException;

/**
 * This exception occurs when service can't be found.
 *
 * @author Marat Kamalov
 *
 */
public class ServiceNotFoundException extends RuleServiceException {

    private static final long serialVersionUID = -4027116341554457266L;

    /**
     * Constructs a new ServiceNotFoundException.
     */
    public ServiceNotFoundException() {
        super();
    }

    /**
     * Constructs a new ServiceNotFoundException with the specified detail message and cause.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt>
     *            value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public ServiceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new ServiceNotFoundException with the specified detail message
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     */
    public ServiceNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new RuleServiceException with a cause.
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt>
     *            value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public ServiceNotFoundException(Throwable cause) {
        super(cause);
    }

}
