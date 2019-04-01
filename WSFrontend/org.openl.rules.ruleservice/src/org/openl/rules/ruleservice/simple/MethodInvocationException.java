package org.openl.rules.ruleservice.simple;

import org.openl.rules.ruleservice.core.RuleServiceException;

/**
 * Invocation exception.
 *
 * @author Marat Kamalov
 *
 */
public class MethodInvocationException extends RuleServiceException {

    private static final long serialVersionUID = 6506393788240623317L;

    /**
     * Constructs a new MethodInvocationException
     */
    public MethodInvocationException() {
        super();
    }

    /**
     * Constructs a new MethodInvocationException with the specified detail message
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     */
    public MethodInvocationException(String message) {
        super(message);
    }

    /**
     * Constructs a new MethodInvocationException with the specified detail message and cause.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt>
     *            value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public MethodInvocationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new MethodInvocationException with a cause.
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt>
     *            value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public MethodInvocationException(Throwable cause) {
        super(cause);
    }
}
